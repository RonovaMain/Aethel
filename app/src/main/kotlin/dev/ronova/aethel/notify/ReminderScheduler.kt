package dev.ronova.aethel.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import dev.ronova.aethel.data.AethelDb
import dev.ronova.aethel.data.GameId
import dev.ronova.aethel.data.TaskPeriod
import dev.ronova.aethel.domain.TimeCalc
import dev.ronova.aethel.repo

object ReminderScheduler {

    const val ACTION = "dev.ronova.aethel.REMINDER"
    const val EXTRA_KEY = "key"
    const val EXTRA_KIND = "kind"
    const val EXTRA_GAME = "game"
    const val EXTRA_REF = "ref"

    const val KIND_RESET = "reset"
    const val KIND_UNDONE = "undone"
    const val KIND_ENERGY = "energy"
    const val KIND_SUB = "sub"
    const val KIND_EVENT = "event"

    private const val PREFS = "aethel_alarms"
    private const val PREF_KEYS = "keys"

    data class Planned(
        val key: String,
        val at: Long,
        val kind: String,
        val game: GameId,
        val ref: Long = 0L,
    )

    /** Вычисляет, какие напоминания должны быть заведены прямо сейчас. */
    fun plan(db: AethelDb, now: Long): List<Planned> {
        val out = ArrayList<Planned>()
        val guard = now + 60_000L
        for (g in db.games) {
            val id = g.gameId
            if (g.remindDailyReset) {
                out += Planned("reset_${id.name}", TimeCalc.nextDailyAt(guard, g.dailyResetMinutes), KIND_RESET, id)
            }
            if (g.remindUndone) {
                out += Planned("undone_${id.name}", TimeCalc.nextDailyAt(guard, g.undoneRemindMinutes), KIND_UNDONE, id)
            }
            if (g.remindSubClaim && g.subDaysLeft(now) > 0) {
                out += Planned("sub_${id.name}", TimeCalc.nextDailyAt(guard, g.subClaimMinutes), KIND_SUB, id)
            }
            if (g.remindEnergyFull) {
                val full = g.energy.fullAt()
                if (full != null && full > guard) {
                    out += Planned("energy_${id.name}", full, KIND_ENERGY, id)
                }
            }
        }
        for (e in db.events) {
            if (!e.enabled) continue
            val trigger = TimeCalc.nextEventTrigger(e, guard) ?: continue
            out += Planned("ev_${e.id}", trigger, KIND_EVENT, e.game, e.id)
        }
        return out
    }

    private fun buildIntent(ctx: Context, key: String): Intent =
        Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION)
            .setData(Uri.parse("aethel://reminder/$key"))

    private fun cancel(ctx: Context, am: AlarmManager, key: String) {
        val pi = PendingIntent.getBroadcast(
            ctx, 0, buildIntent(ctx, key),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun schedule(ctx: Context, am: AlarmManager, p: Planned) {
        val intent = buildIntent(ctx, p.key)
            .putExtra(EXTRA_KEY, p.key)
            .putExtra(EXTRA_KIND, p.kind)
            .putExtra(EXTRA_GAME, p.game.name)
            .putExtra(EXTRA_REF, p.ref)
        val pi = PendingIntent.getBroadcast(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.at, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.at, pi)
        }
    }

    /** Отменяет все прежние будильники и заводит актуальные по текущей базе. */
    @Synchronized
    fun reschedule(ctx: Context) {
        val app = ctx.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = prefs.getStringSet(PREF_KEYS, emptySet()) ?: emptySet()
        for (key in old) cancel(app, am, key)

        val db = app.repo().db.value
        if (!db.settings.notificationsEnabled) {
            prefs.edit().putStringSet(PREF_KEYS, emptySet()).apply()
            return
        }
        val planned = plan(db, System.currentTimeMillis())
        for (p in planned) schedule(app, am, p)
        prefs.edit().putStringSet(PREF_KEYS, planned.map { it.key }.toSet()).apply()
    }

    /** Вызывается из ReminderReceiver: проверяет условия и показывает уведомление. */
    fun fire(ctx: Context, key: String, kind: String, game: GameId, ref: Long) {
        val db = ctx.repo().db.value
        if (!db.settings.notificationsEnabled) return
        val g = db.game(game)
        val now = System.currentTimeMillis()
        val title = "Aethel · ${game.title}"
        val notifId = key.hashCode()

        when (kind) {
            KIND_RESET -> {
                if (!g.remindDailyReset) return
                Notifier.show(ctx, notifId, title, "Ежедневные поручения обновились — самое время зайти в игру.")
            }

            KIND_UNDONE -> {
                if (!g.remindUndone) return
                val left = g.tasks.count {
                    it.period == TaskPeriod.DAILY && !TimeCalc.isTaskDone(g, it, now)
                }
                if (left > 0) {
                    Notifier.show(ctx, notifId, title, "Невыполненных ежедневных поручений: $left.")
                }
            }

            KIND_ENERGY -> {
                if (!g.remindEnergyFull) return
                val full = g.energy.fullAt()
                if (full == null || now >= full - 90_000L) {
                    Notifier.show(
                        ctx, notifId, title,
                        "${game.energyName}: ${g.energy.max}/${g.energy.max} — энергия полностью восстановилась.",
                    )
                }
            }

            KIND_SUB -> {
                if (!g.remindSubClaim) return
                val left = g.subDaysLeft(now)
                if (left <= 0) return
                val claimed = g.subClaimedToday(now)
                if (!claimed) {
                    val tail = if (left <= 3) " Осталось дней: $left." else ""
                    Notifier.show(ctx, notifId, title, "Заберите ежедневную награду: ${game.subName}.$tail")
                } else if (left <= 3) {
                    Notifier.show(ctx, notifId, title, "${game.subName} скоро закончится. Осталось дней: $left.")
                }
            }

            KIND_EVENT -> {
                val e = db.events.firstOrNull { it.id == ref } ?: return
                if (!e.enabled) return
                val text = if (e.remindBeforeMin <= 0) {
                    "Событие наступило: ${e.title}"
                } else {
                    val lead = TimeCalc.remindLabel(e.remindBeforeMin).replaceFirst("За ", "через ")
                    "${e.title} — $lead"
                }
                Notifier.show(ctx, notifId, "Aethel · ${e.game.title}", text)
            }
        }
    }
}

package dev.ronova.aethel.domain

import dev.ronova.aethel.data.GameState
import dev.ronova.aethel.data.Repeat
import dev.ronova.aethel.data.TaskDef
import dev.ronova.aethel.data.TaskPeriod
import dev.ronova.aethel.data.TimerEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeCalc {

    val zone: ZoneId get() = ZoneId.systemDefault()

    private val ruLocale = Locale("ru")

    private fun resetTime(minutes: Int): LocalTime = LocalTime.of((minutes / 60) % 24, minutes % 60)

    private fun at(date: LocalDate, minutes: Int): ZonedDateTime =
        ZonedDateTime.of(date, resetTime(minutes), zone)

    /** «Логический» игровой день: сдвинут на время ежедневного сброса. */
    fun logicalDay(now: Long, resetMinutes: Int): Long =
        Instant.ofEpochMilli(now).atZone(zone).minusMinutes(resetMinutes.toLong()).toLocalDate().toEpochDay()

    /** Момент последнего сброса для периода (<= now). */
    fun lastReset(now: Long, period: TaskPeriod, resetMinutes: Int, weeklyDay: Int): Long {
        val nowDt = Instant.ofEpochMilli(now).atZone(zone)
        val today = nowDt.toLocalDate()
        val result: ZonedDateTime = when (period) {
            TaskPeriod.DAILY -> {
                val c = at(today, resetMinutes)
                if (c.isAfter(nowDt)) at(today.minusDays(1), resetMinutes) else c
            }

            TaskPeriod.WEEKLY -> {
                var d = today
                while (d.dayOfWeek.value != weeklyDay) d = d.minusDays(1)
                val c = at(d, resetMinutes)
                if (c.isAfter(nowDt)) at(d.minusWeeks(1), resetMinutes) else c
            }

            TaskPeriod.MONTHLY -> {
                val first = today.withDayOfMonth(1)
                val c = at(first, resetMinutes)
                if (c.isAfter(nowDt)) at(first.minusMonths(1), resetMinutes) else c
            }

            TaskPeriod.HALF_MONTH -> {
                val c16 = at(today.withDayOfMonth(16), resetMinutes)
                val c1 = at(today.withDayOfMonth(1), resetMinutes)
                when {
                    !c16.isAfter(nowDt) -> c16
                    !c1.isAfter(nowDt) -> c1
                    else -> at(today.withDayOfMonth(1).minusMonths(1).withDayOfMonth(16), resetMinutes)
                }
            }
        }
        return result.toInstant().toEpochMilli()
    }

    /** Момент следующего сброса для периода (> now). */
    fun nextReset(now: Long, period: TaskPeriod, resetMinutes: Int, weeklyDay: Int): Long {
        val last = Instant.ofEpochMilli(lastReset(now, period, resetMinutes, weeklyDay)).atZone(zone)
        val lastDate = last.toLocalDate()
        val nextDate = when (period) {
            TaskPeriod.DAILY -> lastDate.plusDays(1)
            TaskPeriod.WEEKLY -> lastDate.plusWeeks(1)
            TaskPeriod.MONTHLY -> lastDate.withDayOfMonth(1).plusMonths(1)
            TaskPeriod.HALF_MONTH ->
                if (lastDate.dayOfMonth >= 16) lastDate.withDayOfMonth(1).plusMonths(1)
                else lastDate.withDayOfMonth(16)
        }
        return at(nextDate, resetMinutes).toInstant().toEpochMilli()
    }

    fun isTaskDone(game: GameState, task: TaskDef, now: Long): Boolean {
        val doneAt = game.taskDone[task.id] ?: return false
        return doneAt >= lastReset(now, task.period, game.dailyResetMinutes, game.weeklyResetDay)
    }

    /** Ближайший момент строго после [after], когда локальное время равно [minutesOfDay]. */
    fun nextDailyAt(after: Long, minutesOfDay: Int): Long {
        val afterInstant = Instant.ofEpochMilli(after)
        var date = afterInstant.atZone(zone).toLocalDate()
        var cand = at(date, minutesOfDay)
        while (!cand.toInstant().isAfter(afterInstant)) {
            date = date.plusDays(1)
            cand = at(date, minutesOfDay)
        }
        return cand.toInstant().toEpochMilli()
    }

    // ---------- события ----------

    private fun occurrence(e: TimerEvent, k: Long): Long {
        val base = Instant.ofEpochMilli(e.at).atZone(zone)
        val t = when (e.repeat) {
            Repeat.NONE -> base
            Repeat.DAILY -> base.plusDays(k)
            Repeat.WEEKLY -> base.plusWeeks(k)
            Repeat.BIWEEKLY -> base.plusWeeks(2 * k)
            Repeat.MONTHLY -> base.plusMonths(k)
        }
        return t.toInstant().toEpochMilli()
    }

    /** Первое наступление события не раньше [from] или null, если событие прошло. */
    fun nextOccurrence(e: TimerEvent, from: Long): Long? {
        if (e.repeat == Repeat.NONE) return if (e.at >= from) e.at else null
        if (from <= e.at) return e.at
        val periodMs = when (e.repeat) {
            Repeat.DAILY -> 86_400_000L
            Repeat.WEEKLY -> 604_800_000L
            Repeat.BIWEEKLY -> 1_209_600_000L
            else -> 31L * 86_400_000L
        }
        var k = maxOf(0L, (from - e.at) / periodMs - 1)
        var guard = 0
        while (occurrence(e, k) < from && guard < 100_000) {
            k++
            guard++
        }
        return occurrence(e, k)
    }

    /** Момент уведомления: первое наступление, чьё время минус напоминание > [after]. */
    fun nextEventTrigger(e: TimerEvent, after: Long): Long? {
        if (e.remindBeforeMin < 0) return null
        val lead = e.remindBeforeMin * 60_000L
        val occ = nextOccurrence(e, after + lead + 1) ?: return null
        return occ - lead
    }

    // ---------- форматирование ----------

    fun formatCountdown(ms: Long): String {
        if (ms <= 0L) return "сейчас"
        val totalMin = ms / 60_000L
        val d = totalMin / (60 * 24)
        val h = (totalMin / 60) % 24
        val m = totalMin % 60
        return when {
            d > 0 -> if (h > 0) "$d д $h ч" else "$d д"
            h > 0 -> if (m > 0) "$h ч $m мин" else "$h ч"
            totalMin > 0 -> "$m мин"
            else -> "меньше минуты"
        }
    }

    fun formatMinutes(minutes: Int): String =
        "%02d:%02d".format((minutes / 60) % 24, minutes % 60)

    private val dateTimeFmt: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", ruLocale)

    private val dateFmt: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("d MMM yyyy", ruLocale)

    private val timeFmt: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("HH:mm", ruLocale)

    fun formatDateTime(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).format(dateTimeFmt)

    fun formatDate(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).format(dateFmt)

    fun formatTime(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).format(timeFmt)

    val weekDayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    val weekDayFull = listOf(
        "понедельник", "вторник", "среду", "четверг", "пятницу", "субботу", "воскресенье",
    )

    fun remindLabel(min: Int): String = when {
        min < 0 -> "Без напоминания"
        min == 0 -> "В момент события"
        min < 60 -> "За $min мин"
        min < 1440 -> "За ${min / 60} ч"
        else -> "За ${min / 1440} д"
    }
}

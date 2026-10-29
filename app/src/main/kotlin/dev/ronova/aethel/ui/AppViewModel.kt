package dev.ronova.aethel.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ronova.aethel.data.AethelDb
import dev.ronova.aethel.data.AppTab
import dev.ronova.aethel.data.BannerType
import dev.ronova.aethel.data.GameCatalog
import dev.ronova.aethel.data.GameId
import dev.ronova.aethel.data.GameState
import dev.ronova.aethel.data.PaletteId
import dev.ronova.aethel.data.Pull
import dev.ronova.aethel.data.Repeat
import dev.ronova.aethel.data.Settings
import dev.ronova.aethel.data.SubState
import dev.ronova.aethel.data.TaskDef
import dev.ronova.aethel.data.TaskPeriod
import dev.ronova.aethel.data.ThemeMode
import dev.ronova.aethel.data.TimerEvent
import dev.ronova.aethel.domain.TimeCalc
import dev.ronova.aethel.notify.ReminderScheduler
import dev.ronova.aethel.repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = app.repo()

    val db: StateFlow<AethelDb> = repository.db

    private val _currentGame = MutableStateFlow(repository.db.value.settings.defaultGame)
    val currentGame: StateFlow<GameId> = _currentGame.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    fun toast(text: String) {
        _messages.tryEmit(text)
    }

    fun selectGame(game: GameId) {
        _currentGame.value = game
    }

    // ---------- базовые обёртки ----------

    private fun mutate(block: (AethelDb) -> AethelDb) {
        repository.update(block)
        ReminderScheduler.reschedule(getApplication<Application>())
    }

    private fun mutateGame(game: GameId, block: (GameState) -> GameState) {
        mutate { db ->
            db.copy(games = db.games.map { if (it.gameId == game) block(it) else it })
        }
    }

    private fun mutateSettings(block: (Settings) -> Settings) {
        mutate { it.copy(settings = block(it.settings)) }
    }

    // ---------- онбординг и профиль ----------

    fun completeOnboarding(nickname: String, game: GameId, resetMinutes: Int) {
        mutate { db ->
            db.copy(
                profile = db.profile.copy(
                    nickname = nickname.trim(),
                    onboarded = true,
                    createdAt = System.currentTimeMillis(),
                ),
                settings = db.settings.copy(defaultGame = game),
                games = db.games.map {
                    if (it.gameId == game) it.copy(dailyResetMinutes = resetMinutes) else it
                },
            )
        }
        _currentGame.value = game
    }

    fun setNickname(name: String) {
        mutate { it.copy(profile = it.profile.copy(nickname = name.trim())) }
    }

    fun setDefaultGame(game: GameId) {
        mutateSettings { it.copy(defaultGame = game) }
        _currentGame.value = game
    }

    fun setUid(game: GameId, uid: String, level: String) {
        mutateGame(game) { it.copy(uid = uid.trim(), level = level.trim()) }
    }

    // ---------- оформление ----------

    fun setThemeMode(mode: ThemeMode) = mutateSettings { it.copy(themeMode = mode) }
    fun setPalette(palette: PaletteId) = mutateSettings { it.copy(palette = palette) }
    fun setAmoled(value: Boolean) = mutateSettings { it.copy(amoled = value) }
    fun setStartTab(tab: AppTab) = mutateSettings { it.copy(startTab = tab) }
    fun setNotificationsEnabled(value: Boolean) = mutateSettings { it.copy(notificationsEnabled = value) }

    // ---------- сбросы и напоминания ----------

    fun setResetMinutes(game: GameId, minutes: Int) =
        mutateGame(game) { it.copy(dailyResetMinutes = minutes) }

    fun setWeeklyDay(game: GameId, day: Int) =
        mutateGame(game) { it.copy(weeklyResetDay = day) }

    fun setRemindDailyReset(game: GameId, value: Boolean) =
        mutateGame(game) { it.copy(remindDailyReset = value) }

    fun setRemindUndone(game: GameId, value: Boolean) =
        mutateGame(game) { it.copy(remindUndone = value) }

    fun setUndoneRemindMinutes(game: GameId, minutes: Int) =
        mutateGame(game) { it.copy(undoneRemindMinutes = minutes) }

    fun setRemindEnergy(game: GameId, value: Boolean) =
        mutateGame(game) { it.copy(remindEnergyFull = value) }

    fun setRemindSub(game: GameId, value: Boolean) =
        mutateGame(game) { it.copy(remindSubClaim = value) }

    fun setSubClaimMinutes(game: GameId, minutes: Int) =
        mutateGame(game) { it.copy(subClaimMinutes = minutes) }

    // ---------- поручения ----------

    fun toggleTask(game: GameId, taskId: String) {
        val now = System.currentTimeMillis()
        mutateGame(game) { g ->
            val task = g.tasks.firstOrNull { it.id == taskId } ?: return@mutateGame g
            val done = TimeCalc.isTaskDone(g, task, now)
            g.copy(taskDone = if (done) g.taskDone - taskId else g.taskDone + (taskId to now))
        }
    }

    fun addTask(game: GameId, title: String, period: TaskPeriod, note: String) {
        val id = "custom_${System.currentTimeMillis()}"
        mutateGame(game) { it.copy(tasks = it.tasks + TaskDef(id, title.trim(), period, note.trim(), builtin = false)) }
    }

    fun updateTask(game: GameId, taskId: String, title: String, period: TaskPeriod, note: String) {
        mutateGame(game) { g ->
            g.copy(tasks = g.tasks.map {
                if (it.id == taskId) it.copy(title = title.trim(), period = period, note = note.trim()) else it
            })
        }
    }

    fun deleteTask(game: GameId, taskId: String) {
        mutateGame(game) { g ->
            g.copy(tasks = g.tasks.filterNot { it.id == taskId }, taskDone = g.taskDone - taskId)
        }
    }

    fun restoreDefaultTasks(game: GameId) {
        mutateGame(game) { g ->
            val customs = g.tasks.filterNot { it.builtin }
            g.copy(tasks = GameCatalog.defaultTasks(game) + customs)
        }
    }

    fun resetTaskProgress(game: GameId) {
        mutateGame(game) { it.copy(taskDone = emptyMap()) }
    }

    // ---------- энергия ----------

    fun setEnergy(game: GameId, value: Int) {
        val now = System.currentTimeMillis()
        mutateGame(game) { g ->
            g.copy(energy = g.energy.copy(valueAtSet = value.coerceIn(0, 9999), setAt = now))
        }
    }

    fun spendEnergy(game: GameId, amount: Int) {
        val now = System.currentTimeMillis()
        mutateGame(game) { g ->
            val cur = g.energy.current(now)
            g.copy(energy = g.energy.copy(valueAtSet = (cur - amount).coerceAtLeast(0), setAt = now))
        }
    }

    fun setEnergyMax(game: GameId, max: Int) {
        val now = System.currentTimeMillis()
        mutateGame(game) { g ->
            val cur = g.energy.current(now)
            g.copy(energy = g.energy.copy(max = max.coerceIn(1, 9999), valueAtSet = cur, setAt = now))
        }
    }

    // ---------- подписка (Полая луна и аналоги) ----------

    fun activateSub(game: GameId, days: Int) {
        val now = System.currentTimeMillis()
        mutateGame(game) { g ->
            val today = g.logicalDay(now)
            g.copy(sub = g.sub.copy(endEpochDay = today + days.coerceAtLeast(0)))
        }
    }

    fun extendSub(game: GameId, days: Int) {
        val now = System.currentTimeMillis()
        mutateGame(game) { g ->
            val today = g.logicalDay(now)
            val base = maxOf(g.sub.endEpochDay, today)
            g.copy(sub = g.sub.copy(endEpochDay = base + days))
        }
    }

    fun deactivateSub(game: GameId) {
        mutateGame(game) { it.copy(sub = SubState()) }
    }

    fun setSubClaimed(game: GameId, claimed: Boolean) {
        val now = System.currentTimeMillis()
        mutateGame(game) { g ->
            g.copy(sub = g.sub.copy(lastClaimDay = if (claimed) g.logicalDay(now) else -1L))
        }
    }

    // ---------- ресурсы ----------

    fun setResources(game: GameId, gems: Int, tickets: Int) {
        mutateGame(game) { it.copy(gems = gems.coerceAtLeast(0), tickets = tickets.coerceAtLeast(0)) }
    }

    // ---------- крутки ----------

    /**
     * Добавляет записи крутки. Перед основной записью вставляется [fillers] обычных (3★) круток.
     * Если [rarity] == 3, добавляется просто [fillers] штук обычных круток.
     */
    fun addPulls(
        game: GameId,
        banner: BannerType,
        rarity: Int,
        name: String,
        featured: Boolean,
        fillers: Int,
    ) {
        val now = System.currentTimeMillis()
        mutate { db ->
            var id = db.nextId
            val added = ArrayList<Pull>()
            val fillCount = fillers.coerceIn(0, 500)
            repeat(fillCount) {
                added += Pull(id++, game, banner, 3, "", false, now)
            }
            if (rarity >= 4) {
                added += Pull(id++, game, banner, rarity, name.trim(), featured, now)
            }
            db.copy(pulls = db.pulls + added, nextId = id)
        }
    }

    fun deletePull(pullId: Long) {
        mutate { db -> db.copy(pulls = db.pulls.filterNot { it.id == pullId }) }
    }

    fun undoLastPull(game: GameId, banner: BannerType) {
        mutate { db ->
            val last = db.pulls.filter { it.game == game && it.banner == banner }.maxByOrNull { it.id }
            if (last == null) db else db.copy(pulls = db.pulls.filterNot { it.id == last.id })
        }
    }

    fun clearBanner(game: GameId, banner: BannerType) {
        mutate { db -> db.copy(pulls = db.pulls.filterNot { it.game == game && it.banner == banner }) }
    }

    // ---------- события ----------

    fun saveEvent(
        id: Long?,
        game: GameId,
        title: String,
        at: Long,
        repeat: Repeat,
        remindBeforeMin: Int,
    ) {
        mutate { db ->
            if (id == null) {
                val ev = TimerEvent(db.nextId, game, title.trim(), at, repeat, remindBeforeMin, true)
                db.copy(events = db.events + ev, nextId = db.nextId + 1)
            } else {
                db.copy(events = db.events.map {
                    if (it.id == id) it.copy(
                        game = game, title = title.trim(), at = at,
                        repeat = repeat, remindBeforeMin = remindBeforeMin,
                    ) else it
                })
            }
        }
    }

    fun setEventEnabled(id: Long, enabled: Boolean) {
        mutate { db -> db.copy(events = db.events.map { if (it.id == id) it.copy(enabled = enabled) else it }) }
    }

    fun deleteEvent(id: Long) {
        mutate { db -> db.copy(events = db.events.filterNot { it.id == id }) }
    }

    // ---------- резервные копии ----------

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val bytes = repository.exportJson().toByteArray(Charsets.UTF_8)
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(bytes)
                    } != null
                } catch (e: Exception) {
                    false
                }
            }
            toast(if (ok) "База сохранена" else "Не удалось сохранить файл")
        }
    }

    fun exportCsvTo(uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val bytes = repository.exportPullsCsv().toByteArray(Charsets.UTF_8)
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(bytes)
                    } != null
                } catch (e: Exception) {
                    false
                }
            }
            toast(if (ok) "Крутки экспортированы в CSV" else "Не удалось сохранить файл")
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            val error: String? = withContext(Dispatchers.IO) {
                try {
                    val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    }
                    if (text == null) "Не удалось открыть файл" else repository.importJson(text)
                } catch (e: Exception) {
                    "Не удалось прочитать файл"
                }
            }
            if (error == null) {
                ReminderScheduler.reschedule(getApplication<Application>())
                _currentGame.value = repository.db.value.settings.defaultGame
                toast("База импортирована")
            } else {
                toast(error)
            }
        }
    }

    fun resetAllData() {
        repository.resetAll()
        ReminderScheduler.reschedule(getApplication<Application>())
        _currentGame.value = GameId.GENSHIN
        toast("Все данные удалены")
    }
}

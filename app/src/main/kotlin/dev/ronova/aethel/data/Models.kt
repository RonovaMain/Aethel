package dev.ronova.aethel.data

import kotlinx.serialization.Serializable
import kotlin.math.max

/** Поддерживаемые игры HoYoverse. */
enum class GameId(
    val title: String,
    val short: String,
    val gemName: String,
    val energyName: String,
    val energyMax: Int,
    val energyRegenSec: Int,
    val subName: String,
    val subShort: String,
    val colorA: Long,
    val colorB: Long,
    val quickSpend: List<Int>,
) {
    GENSHIN(
        title = "Genshin Impact",
        short = "Genshin",
        gemName = "Примогемы",
        energyName = "Оригинальная смола",
        energyMax = 200,
        energyRegenSec = 8 * 60,
        subName = "Благословение полой луны",
        subShort = "Полая луна",
        colorA = 0xFF1F7A8C,
        colorB = 0xFFB27A1E,
        quickSpend = listOf(20, 40, 60),
    ),
    HSR(
        title = "Honkai: Star Rail",
        short = "HSR",
        gemName = "Звёздный нефрит",
        energyName = "Энергия (Trailblaze Power)",
        energyMax = 300,
        energyRegenSec = 6 * 60,
        subName = "Пропуск экспресс-поставок",
        subShort = "Экспресс-пропуск",
        colorA = 0xFF4B3FB8,
        colorB = 0xFF8A5CF5,
        quickSpend = listOf(10, 30, 40, 60),
    ),
    ZZZ(
        title = "Zenless Zone Zero",
        short = "ZZZ",
        gemName = "Полихромы",
        energyName = "Заряд батареи",
        energyMax = 240,
        energyRegenSec = 6 * 60,
        subName = "Членство Inter-Knot",
        subShort = "Inter-Knot",
        colorA = 0xFF1F1F23,
        colorB = 0xFFB8860B,
        quickSpend = listOf(20, 40, 60),
    );

    /** Подпись редкости: в ZZZ ранги S/A/B, в остальных играх звёзды. */
    fun rarityLabel(rarity: Int): String =
        if (this == ZZZ) {
            when (rarity) {
                5 -> "S"
                4 -> "A"
                else -> "B"
            }
        } else {
            "$rarity★"
        }
}

enum class ThemeMode(val title: String) { SYSTEM("Системная"), LIGHT("Светлая"), DARK("Тёмная") }

enum class PaletteId(val title: String) {
    DYNAMIC("Динамическая"),
    AETHEL("Aethel"),
    SAKURA("Сакура"),
    OCEAN("Океан"),
    MINT("Мята"),
}

enum class AppTab(val title: String) {
    HOME("Главная"),
    WISHES("Крутки"),
    TASKS("Поручения"),
    EVENTS("События"),
    PROFILE("Профиль"),
}

enum class TaskPeriod(val title: String) {
    DAILY("Ежедневно"),
    WEEKLY("Еженедельно"),
    HALF_MONTH("1 и 16 числа"),
    MONTHLY("Ежемесячно"),
}

enum class Repeat(val title: String) {
    NONE("Не повторять"),
    DAILY("Каждый день"),
    WEEKLY("Каждую неделю"),
    BIWEEKLY("Раз в 2 недели"),
    MONTHLY("Каждый месяц"),
}

enum class BannerType { CHARACTER, WEAPON, STANDARD, CHRONICLED, BANGBOO }

@Serializable
data class Profile(
    val nickname: String = "",
    val onboarded: Boolean = false,
    val createdAt: Long = 0L,
)

@Serializable
data class Settings(
    val defaultGame: GameId = GameId.GENSHIN,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: PaletteId = PaletteId.AETHEL,
    val amoled: Boolean = false,
    val startTab: AppTab = AppTab.HOME,
    val notificationsEnabled: Boolean = true,
)

@Serializable
data class TaskDef(
    val id: String,
    val title: String,
    val period: TaskPeriod = TaskPeriod.DAILY,
    val note: String = "",
    val builtin: Boolean = false,
)

@Serializable
data class EnergyState(
    val max: Int = 200,
    val regenSeconds: Int = 480,
    val valueAtSet: Int = 200,
    val setAt: Long = 0L,
) {
    /** Текущее значение энергии на момент [now]. */
    fun current(now: Long): Int {
        if (valueAtSet >= max || setAt <= 0L) return valueAtSet
        val gained = ((now - setAt).coerceAtLeast(0L) / 1000L / regenSeconds).toInt()
        return minOf(max, valueAtSet + gained)
    }

    /** Момент полного заполнения или null, если уже заполнено. */
    fun fullAt(): Long? {
        if (valueAtSet >= max || setAt <= 0L) return null
        return setAt + (max - valueAtSet).toLong() * regenSeconds * 1000L
    }
}

@Serializable
data class SubState(
    /** День (epoch day), на котором подписка заканчивается. 0 — подписки нет. */
    val endEpochDay: Long = 0L,
    /** Последний «логический день», в который награда была получена. */
    val lastClaimDay: Long = -1L,
)

@Serializable
data class GameState(
    val gameId: GameId,
    val uid: String = "",
    val level: String = "",
    /** Время ежедневного обновления по времени игрока, минуты от полуночи. */
    val dailyResetMinutes: Int = 4 * 60,
    /** День недели еженедельного сброса: 1 = понедельник … 7 = воскресенье. */
    val weeklyResetDay: Int = 1,
    val tasks: List<TaskDef> = emptyList(),
    val taskDone: Map<String, Long> = emptyMap(),
    val energy: EnergyState = EnergyState(),
    val sub: SubState = SubState(),
    val gems: Int = 0,
    val tickets: Int = 0,
    val remindDailyReset: Boolean = true,
    val remindUndone: Boolean = false,
    val undoneRemindMinutes: Int = 20 * 60,
    val remindEnergyFull: Boolean = true,
    val remindSubClaim: Boolean = true,
    val subClaimMinutes: Int = 12 * 60,
) {
    fun logicalDay(now: Long): Long =
        dev.ronova.aethel.domain.TimeCalc.logicalDay(now, dailyResetMinutes)

    fun subDaysLeft(now: Long): Int =
        max(0L, sub.endEpochDay - logicalDay(now)).toInt()

    fun subClaimedToday(now: Long): Boolean = sub.lastClaimDay == logicalDay(now)

    /** Сколько ежедневных наград ещё можно забрать (включая сегодняшнюю, если не получена). */
    fun subRemainingClaims(now: Long): Int {
        val left = subDaysLeft(now)
        return if (left > 0 && subClaimedToday(now)) left - 1 else left
    }
}

@Serializable
data class Pull(
    val id: Long,
    val game: GameId,
    val banner: BannerType,
    val rarity: Int,
    val name: String = "",
    val featured: Boolean = false,
    val at: Long = 0L,
)

@Serializable
data class TimerEvent(
    val id: Long,
    val game: GameId,
    val title: String,
    val at: Long,
    val repeat: Repeat = Repeat.NONE,
    /** За сколько минут напомнить; -1 — не напоминать. */
    val remindBeforeMin: Int = 0,
    val enabled: Boolean = true,
)

object DbInfo {
    const val FORMAT = "aethel-db"
    const val SCHEMA = 1
}

@Serializable
data class AethelDb(
    val format: String = DbInfo.FORMAT,
    val schema: Int = DbInfo.SCHEMA,
    val profile: Profile = Profile(),
    val settings: Settings = Settings(),
    val games: List<GameState> = emptyList(),
    val pulls: List<Pull> = emptyList(),
    val events: List<TimerEvent> = emptyList(),
    val nextId: Long = 1L,
) {
    fun game(id: GameId): GameState =
        games.firstOrNull { it.gameId == id } ?: GameCatalog.defaultState(id)

    /** Гарантирует наличие всех игр и корректный счётчик id. */
    fun normalized(): AethelDb {
        val existing = games.associateBy { it.gameId }
        val fixed = GameId.entries.map { existing[it] ?: GameCatalog.defaultState(it) }
        val maxPull = pulls.maxOfOrNull { it.id } ?: 0L
        val maxEvent = events.maxOfOrNull { it.id } ?: 0L
        return copy(
            games = fixed,
            nextId = maxOf(nextId, maxPull + 1, maxEvent + 1),
        )
    }
}

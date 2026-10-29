package dev.ronova.aethel.data

/** Описание баннера: гарант 5★, мягкий гарант и наличие системы 50/50. */
data class BannerDef(
    val type: BannerType,
    val title: String,
    val hard5: Int,
    val soft5: Int,
    val hard4: Int = 10,
    val guarantee: Boolean,
    val note: String = "",
)

object GameCatalog {

    const val GEMS_PER_PULL = 160

    fun banners(game: GameId): List<BannerDef> = when (game) {
        GameId.GENSHIN -> listOf(
            BannerDef(BannerType.CHARACTER, "Событие: персонаж", 90, 74, guarantee = true, note = "Гарантия 50/50"),
            BannerDef(BannerType.WEAPON, "Событие: оружие", 80, 63, guarantee = true, note = "Гарантия 75/25"),
            BannerDef(BannerType.STANDARD, "Стандартные молитвы", 90, 74, guarantee = false),
            BannerDef(BannerType.CHRONICLED, "Хроники желаний", 90, 74, guarantee = true, note = "Гарантия 50/50"),
        )

        GameId.HSR -> listOf(
            BannerDef(BannerType.CHARACTER, "Событие: персонаж", 90, 74, guarantee = true, note = "Гарантия 50/50"),
            BannerDef(BannerType.WEAPON, "Событие: световой конус", 80, 66, guarantee = true, note = "Гарантия 75/25"),
            BannerDef(BannerType.STANDARD, "Стандартный варп", 90, 74, guarantee = false),
        )

        GameId.ZZZ -> listOf(
            BannerDef(BannerType.CHARACTER, "Эксклюзивный канал", 90, 74, guarantee = true, note = "Гарантия 50/50"),
            BannerDef(BannerType.WEAPON, "Канал W-Engine", 80, 65, guarantee = true, note = "Гарантия 75/25"),
            BannerDef(BannerType.STANDARD, "Стандартный канал", 90, 73, guarantee = false),
            BannerDef(BannerType.BANGBOO, "Канал Бангбу", 80, 64, guarantee = true, note = "Гарантия 75/25"),
        )
    }

    fun banner(game: GameId, type: BannerType): BannerDef =
        banners(game).firstOrNull { it.type == type } ?: banners(game).first()

    fun defaultTasks(game: GameId): List<TaskDef> = when (game) {
        GameId.GENSHIN -> listOf(
            task("gi_commission", "Ежедневные поручения", TaskPeriod.DAILY, "4 поручения и награда Гильдии"),
            task("gi_guild", "Награда Гильдии искателей приключений", TaskPeriod.DAILY),
            task("gi_resin", "Потратить оригинальную смолу", TaskPeriod.DAILY),
            task("gi_realm", "Собрать монеты в Чайнике безмятежности", TaskPeriod.DAILY),
            task("gi_expedition", "Отправить экспедиции", TaskPeriod.DAILY),
            task("gi_boss", "Боссы недели", TaskPeriod.WEEKLY, "3 награды со скидкой смолы"),
            task("gi_transformer", "Параметрический преобразователь", TaskPeriod.WEEKLY),
            task("gi_bp_weekly", "Задания боевого пропуска", TaskPeriod.WEEKLY),
            task("gi_abyss", "Витая бездна", TaskPeriod.HALF_MONTH, "Сброс 1 и 16 числа"),
            task("gi_theater", "Воображаемый театр", TaskPeriod.MONTHLY),
            task("gi_shop", "Лавка Паймон: ежемесячный обмен", TaskPeriod.MONTHLY),
        )

        GameId.HSR -> listOf(
            task("hsr_training", "Ежедневные тренировки", TaskPeriod.DAILY, "500 очков активности"),
            task("hsr_energy", "Потратить энергию (Trailblaze Power)", TaskPeriod.DAILY),
            task("hsr_assign", "Поручения: отправить и забрать", TaskPeriod.DAILY),
            task("hsr_eow", "Эхо войны", TaskPeriod.WEEKLY),
            task("hsr_su", "Симулированная вселенная", TaskPeriod.WEEKLY, "Еженедельные очки"),
            task("hsr_bp_weekly", "Задания пропуска (недельные)", TaskPeriod.WEEKLY),
            task("hsr_moc", "Забвенный зал", TaskPeriod.HALF_MONTH, "Примерно раз в 2 недели"),
            task("hsr_pf", "Чистая фантастика", TaskPeriod.HALF_MONTH, "Примерно раз в 2 недели"),
            task("hsr_as", "Апокалиптическая тень", TaskPeriod.HALF_MONTH, "Примерно раз в 2 недели"),
        )

        GameId.ZZZ -> listOf(
            task("zzz_daily", "Ежедневная активность", TaskPeriod.DAILY, "Задания дня"),
            task("zzz_battery", "Потратить заряд батареи", TaskPeriod.DAILY),
            task("zzz_scratch", "Скретч-карта", TaskPeriod.DAILY),
            task("zzz_video", "Видеопрокат: управление", TaskPeriod.DAILY),
            task("zzz_ridu", "Еженедельные задания Ridu", TaskPeriod.WEEKLY),
            task("zzz_hunt", "Охота на Нотории", TaskPeriod.WEEKLY),
            task("zzz_hollow", "Hollow Zero: еженедельные награды", TaskPeriod.WEEKLY),
            task("zzz_shiyu", "Оборона Шиюй", TaskPeriod.HALF_MONTH, "Примерно раз в 2 недели"),
            task("zzz_deadly", "Смертельная угроза", TaskPeriod.HALF_MONTH, "Примерно раз в 2 недели"),
        )
    }

    private fun task(id: String, title: String, period: TaskPeriod, note: String = "") =
        TaskDef(id = id, title = title, period = period, note = note, builtin = true)

    fun defaultState(game: GameId): GameState = GameState(
        gameId = game,
        tasks = defaultTasks(game),
        energy = EnergyState(
            max = game.energyMax,
            regenSeconds = game.energyRegenSec,
            valueAtSet = game.energyMax,
            setAt = 0L,
        ),
    )

    val eventPresets: List<String> = listOf(
        "Конец баннера",
        "Обновление версии",
        "Сброс бездны / испытаний",
        "Конец события",
        "Техобслуживание",
    )
}

package dev.ronova.aethel.domain

import dev.ronova.aethel.data.BannerDef
import dev.ronova.aethel.data.Pull

enum class PullTag(val label: String) {
    NONE(""),
    WON("Выиграл 50/50"),
    LOST("Проиграл 50/50"),
    GUARANTEED("По гарантии"),
}

data class PullView(
    val pull: Pull,
    /** Номер крутки с момента предыдущей 5★/4★ (для 3★ — 0). */
    val pityAt: Int,
    val tag: PullTag,
)

data class BannerStats(
    val total: Int,
    val pity5: Int,
    val pity4: Int,
    val guaranteed5: Boolean,
    val count5: Int,
    val count4: Int,
    val avg5: Double,
    val winRatePercent: Int?,
    val wins: Int,
    val losses: Int,
    /** Хронологический порядок (старые → новые). */
    val views: List<PullView>,
)

object PityCalc {

    fun compute(pullsChrono: List<Pull>, def: BannerDef): BannerStats {
        var c5 = 0
        var c4 = 0
        var guaranteed = false
        var wins = 0
        var losses = 0
        var count5 = 0
        var count4 = 0
        var pitySum5 = 0
        val views = ArrayList<PullView>(pullsChrono.size)

        for (p in pullsChrono) {
            c5++
            c4++
            when (p.rarity) {
                5 -> {
                    val tag = when {
                        !def.guarantee -> PullTag.NONE
                        guaranteed -> PullTag.GUARANTEED
                        p.featured -> PullTag.WON
                        else -> PullTag.LOST
                    }
                    if (def.guarantee) {
                        if (guaranteed) {
                            guaranteed = false
                        } else if (p.featured) {
                            wins++
                        } else {
                            losses++
                            guaranteed = true
                        }
                    }
                    views += PullView(p, c5, tag)
                    pitySum5 += c5
                    count5++
                    c5 = 0
                }

                4 -> {
                    views += PullView(p, c4, PullTag.NONE)
                    count4++
                    c4 = 0
                }

                else -> views += PullView(p, 0, PullTag.NONE)
            }
        }

        val rate = if (wins + losses > 0) wins * 100 / (wins + losses) else null
        return BannerStats(
            total = pullsChrono.size,
            pity5 = c5,
            pity4 = c4,
            guaranteed5 = guaranteed,
            count5 = count5,
            count4 = count4,
            avg5 = if (count5 > 0) pitySum5.toDouble() / count5 else 0.0,
            winRatePercent = rate,
            wins = wins,
            losses = losses,
            views = views,
        )
    }
}

/** Крутки выбранного баннера в хронологическом порядке. */
fun dev.ronova.aethel.data.AethelDb.pullsFor(
    game: dev.ronova.aethel.data.GameId,
    banner: dev.ronova.aethel.data.BannerType,
): List<Pull> = pulls.filter { it.game == game && it.banner == banner }.sortedBy { it.id }

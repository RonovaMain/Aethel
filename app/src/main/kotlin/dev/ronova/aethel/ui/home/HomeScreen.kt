package dev.ronova.aethel.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ronova.aethel.R
import dev.ronova.aethel.data.AethelDb
import dev.ronova.aethel.data.AppTab
import dev.ronova.aethel.data.BannerDef
import dev.ronova.aethel.data.GameCatalog
import dev.ronova.aethel.data.GameId
import dev.ronova.aethel.data.TaskPeriod
import dev.ronova.aethel.domain.BannerStats
import dev.ronova.aethel.domain.PityCalc
import dev.ronova.aethel.domain.TimeCalc
import dev.ronova.aethel.domain.pullsFor
import dev.ronova.aethel.ui.AppViewModel
import dev.ronova.aethel.ui.common.EmptyHint
import dev.ronova.aethel.ui.common.GameSwitcher
import dev.ronova.aethel.ui.common.HorizontalChips
import dev.ronova.aethel.ui.common.NumberDialog
import dev.ronova.aethel.ui.common.ConfirmDialog
import dev.ronova.aethel.ui.common.SectionCard
import dev.ronova.aethel.ui.common.gameBrush
import dev.ronova.aethel.ui.common.pityColor
import dev.ronova.aethel.ui.common.rememberNow

private enum class HomeDialog { ENERGY, SUB_ACTIVATE, SUB_EXTEND, SUB_END }

@Composable
fun HomeScreen(
    vm: AppViewModel,
    db: AethelDb,
    game: GameId,
    onNavigate: (AppTab) -> Unit,
    padding: PaddingValues,
) {
    val now by rememberNow()
    val g = db.game(game)
    var dialog by remember { mutableStateOf<HomeDialog?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GameSwitcher(selected = game, onSelect = { vm.selectGame(it) })
        }

        // ---- шапка ----
        item {
            val nick = db.profile.nickname
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(gameBrush(game))
                    .padding(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (nick.isBlank()) "Добро пожаловать" else "Привет, $nick",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    val info = listOfNotNull(
                        g.uid.takeIf { it.isNotBlank() }?.let { "UID $it" },
                        g.level.takeIf { it.isNotBlank() }?.let { "Уровень $it" },
                    ).joinToString("  ·  ")
                    Text(
                        text = info.ifBlank { "Добавьте UID и уровень в профиле" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
                Image(
                    painter = painterResource(R.drawable.ic_aethel_star),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.TopEnd)
                        .alpha(0.9f),
                )
            }
        }

        // ---- поручения ----
        item {
            val daily = g.tasks.filter { it.period == TaskPeriod.DAILY }
            val dailyDone = daily.count { TimeCalc.isTaskDone(g, it, now) }
            val other = g.tasks.filter { it.period != TaskPeriod.DAILY }
            val otherDone = other.count { TimeCalc.isTaskDone(g, it, now) }
            val nextReset = TimeCalc.nextReset(now, TaskPeriod.DAILY, g.dailyResetMinutes, g.weeklyResetDay)
            SectionCard(title = "Сегодня") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$dailyDone / ${daily.size}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (daily.isNotEmpty() && dailyDone == daily.size) "Всё выполнено" else "ежедневных поручений",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { if (daily.isEmpty()) 0f else dailyDone.toFloat() / daily.size },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Text(
                    text = "До обновления: ${TimeCalc.formatCountdown(nextReset - now)} (в ${TimeCalc.formatTime(nextReset)})",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (other.isNotEmpty()) {
                    Text(
                        text = "Недельные и периодические: $otherDone / ${other.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(onClick = { onNavigate(AppTab.TASKS) }) { Text("Открыть поручения") }
            }
        }

        // ---- энергия ----
        item {
            val e = g.energy
            val cur = e.current(now)
            val full = e.fullAt()
            SectionCard(title = game.energyName) {
                Text(
                    text = "$cur / ${e.max}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                LinearProgressIndicator(
                    progress = { (cur.toFloat() / e.max).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                )
                Text(
                    text = if (full == null || full <= now) {
                        "Полностью заполнено"
                    } else {
                        val whenText = if (full - now >= 20L * 3_600_000L) TimeCalc.formatDateTime(full) else TimeCalc.formatTime(full)
                        "Заполнится через ${TimeCalc.formatCountdown(full - now)} ($whenText)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                HorizontalChips {
                    game.quickSpend.forEach { n ->
                        AssistChip(onClick = { vm.spendEnergy(game, n) }, label = { Text("−$n") })
                    }
                    AssistChip(onClick = { dialog = HomeDialog.ENERGY }, label = { Text("Задать") })
                }
            }
        }

        // ---- подписка ----
        item {
            val left = g.subDaysLeft(now)
            val claimed = g.subClaimedToday(now)
            SectionCard(title = game.subName) {
                if (left <= 0) {
                    Text(
                        text = "Подписка не активна",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Если подписка есть, укажите, сколько дней осталось: приложение будет напоминать забирать награду и считать, сколько ещё получите.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { dialog = HomeDialog.SUB_ACTIVATE }) { Text("Указать подписку") }
                } else {
                    Text(
                        text = "Осталось дней: $left",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    LinearProgressIndicator(
                        progress = { (left / 30f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    val remaining = g.subRemainingClaims(now)
                    Text(
                        text = "Ещё можно получить: $remaining × 90 = ${remaining * 90} (${game.gemName.lowercase()})",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (claimed) {
                        FilledTonalButton(onClick = { vm.setSubClaimed(game, false) }) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Награда за сегодня получена")
                        }
                    } else {
                        Button(onClick = { vm.setSubClaimed(game, true) }) { Text("Отметить получение награды") }
                    }
                    HorizontalChips {
                        TextButton(onClick = { dialog = HomeDialog.SUB_ACTIVATE }) { Text("Изменить дни") }
                        TextButton(onClick = { dialog = HomeDialog.SUB_EXTEND }) { Text("Продлить") }
                        TextButton(onClick = { dialog = HomeDialog.SUB_END }) { Text("Закончилась") }
                    }
                }
            }
        }

        // ---- гарант ----
        item {
            val banners = GameCatalog.banners(game)
            val stats = remember(db.pulls, game) {
                banners.map { def -> def to PityCalc.compute(db.pullsFor(game, def.type), def) }
            }
            SectionCard(title = "Гарант") {
                stats.forEach { (def, st) -> PityRow(def, st, game) }
                OutlinedButton(onClick = { onNavigate(AppTab.WISHES) }) { Text("Открыть крутки") }
            }
        }

        // ---- ближайшие события ----
        item {
            val upcoming = db.events
                .filter { it.game == game && it.enabled }
                .mapNotNull { e -> TimeCalc.nextOccurrence(e, now)?.let { e to it } }
                .sortedBy { it.second }
                .take(3)
            SectionCard(title = "Ближайшие события") {
                if (upcoming.isEmpty()) {
                    EmptyHint("Событий пока нет")
                } else {
                    upcoming.forEach { (e, at) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(
                                    TimeCalc.formatDateTime(at),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "через ${TimeCalc.formatCountdown(at - now)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                OutlinedButton(onClick = { onNavigate(AppTab.EVENTS) }) { Text("Таймеры и события") }
            }
        }
    }

    // ---- диалоги ----
    when (dialog) {
        HomeDialog.ENERGY -> NumberDialog(
            title = game.energyName,
            label = "Сколько сейчас",
            initial = g.energy.current(now),
            supporting = "Введите текущее значение из игры — таймер до заполнения пересчитается.",
            onDismiss = { dialog = null },
            onConfirm = {
                vm.setEnergy(game, it)
                dialog = null
            },
        )

        HomeDialog.SUB_ACTIVATE -> NumberDialog(
            title = game.subName,
            label = "Осталось дней",
            initial = g.subDaysLeft(now).takeIf { it > 0 } ?: 30,
            supporting = "Сколько дней подписки осталось сейчас (включая сегодняшний).",
            onDismiss = { dialog = null },
            onConfirm = {
                if (it > 0) vm.activateSub(game, it) else vm.deactivateSub(game)
                dialog = null
            },
        )

        HomeDialog.SUB_EXTEND -> NumberDialog(
            title = "Продлить подписку",
            label = "Добавить дней",
            initial = 30,
            onDismiss = { dialog = null },
            onConfirm = {
                if (it > 0) vm.extendSub(game, it)
                dialog = null
            },
        )

        HomeDialog.SUB_END -> ConfirmDialog(
            title = "Подписка закончилась?",
            text = "Напоминания о ежедневной награде будут отключены, пока вы снова не укажете подписку.",
            confirmLabel = "Да, закончилась",
            onDismiss = { dialog = null },
            onConfirm = {
                vm.deactivateSub(game)
                dialog = null
            },
        )

        null -> Unit
    }
}

@Composable
fun PityRow(def: BannerDef, st: BannerStats, game: GameId) {
    val color = pityColor(st.pity5, def.soft5, def.hard5)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = def.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (def.guarantee && st.guaranteed5) {
                Text(
                    text = "гарант  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "${st.pity5} / ${def.hard5}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
        LinearProgressIndicator(
            progress = { (st.pity5.toFloat() / def.hard5).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
        )
        Text(
            text = "${game.rarityLabel(4)}: ${st.pity4} / ${def.hard4}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

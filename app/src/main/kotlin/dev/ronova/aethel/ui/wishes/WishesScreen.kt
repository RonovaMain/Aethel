package dev.ronova.aethel.ui.wishes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.ronova.aethel.data.AethelDb
import dev.ronova.aethel.data.BannerDef
import dev.ronova.aethel.data.GameCatalog
import dev.ronova.aethel.data.GameId
import dev.ronova.aethel.domain.BannerStats
import dev.ronova.aethel.domain.PityCalc
import dev.ronova.aethel.domain.PullTag
import dev.ronova.aethel.domain.PullView
import dev.ronova.aethel.domain.TimeCalc
import dev.ronova.aethel.domain.pullsFor
import dev.ronova.aethel.ui.AppViewModel
import dev.ronova.aethel.ui.common.ConfirmDialog
import dev.ronova.aethel.ui.common.EmptyHint
import dev.ronova.aethel.ui.common.GameSwitcher
import dev.ronova.aethel.ui.common.HorizontalChips
import dev.ronova.aethel.ui.common.SectionCard
import dev.ronova.aethel.ui.common.StatTile
import dev.ronova.aethel.ui.common.pityColor
import dev.ronova.aethel.ui.common.rarityColor
import kotlin.math.ceil

private enum class HistoryFilter(val title: String) {
    HIGH("5★ и 4★"),
    FIVE("Только 5★"),
    ALL("Все"),
}

@Composable
fun WishesScreen(
    vm: AppViewModel,
    db: AethelDb,
    game: GameId,
    padding: PaddingValues,
) {
    val banners = GameCatalog.banners(game)
    var bannerKey by rememberSaveable(game.name) { mutableStateOf(banners.first().type.name) }
    val def = banners.firstOrNull { it.type.name == bannerKey } ?: banners.first()

    val stats = remember(db.pulls, game, def.type) {
        PityCalc.compute(db.pullsFor(game, def.type), def)
    }
    val g = db.game(game)

    var showAdd by remember { mutableStateOf(false) }
    var showResources by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<Long?>(null) }
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.HIGH.name) }
    val filterValue = HistoryFilter.entries.firstOrNull { it.name == filter } ?: HistoryFilter.HIGH

    val shown: List<PullView> = remember(stats, filterValue) {
        stats.views.asReversed().filter {
            when (filterValue) {
                HistoryFilter.HIGH -> it.pull.rarity >= 4
                HistoryFilter.FIVE -> it.pull.rarity == 5
                HistoryFilter.ALL -> true
            }
        }
    }

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
        item { GameSwitcher(selected = game, onSelect = { vm.selectGame(it) }) }

        item {
            HorizontalChips {
                banners.forEach { b ->
                    FilterChip(
                        selected = b.type == def.type,
                        onClick = { bannerKey = b.type.name },
                        label = { Text(b.title) },
                    )
                }
            }
        }

        item { PityCard(game, def, stats) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { showAdd = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить")
                }
                OutlinedButton(
                    onClick = { vm.undoLastPull(game, def.type) },
                    enabled = stats.total > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Отменить")
                }
            }
        }

        item { StatsCard(game, stats) }

        item { PlannerCard(game, g.gems, g.tickets, def, stats, onEdit = { showResources = true }) }

        item {
            SectionCard(title = "История") {
                HorizontalChips {
                    HistoryFilter.entries.forEach { f ->
                        FilterChip(
                            selected = f == filterValue,
                            onClick = { filter = f.name },
                            label = { Text(f.title) },
                        )
                    }
                }
                if (shown.isEmpty()) {
                    EmptyHint(
                        if (stats.total == 0) "Здесь появятся ваши крутки. Нажмите «Добавить»."
                        else "В этом фильтре пока ничего нет",
                    )
                }
            }
        }

        items(shown, key = { it.pull.id }) { view ->
            HistoryRow(game, def, view, onDelete = { deleteId = view.pull.id })
        }

        if (stats.total > 0) {
            item {
                TextButton(onClick = { showClear = true }) { Text("Очистить историю баннера") }
            }
        }
    }

    if (showAdd) {
        AddPullSheet(
            game = game,
            def = def,
            onDismiss = { showAdd = false },
            onAdd = { rarity, name, featured, fillers ->
                vm.addPulls(game, def.type, rarity, name, featured, fillers)
                showAdd = false
            },
        )
    }

    if (showResources) {
        ResourcesDialog(
            game = game,
            gems = g.gems,
            tickets = g.tickets,
            onDismiss = { showResources = false },
            onConfirm = { gems, tickets ->
                vm.setResources(game, gems, tickets)
                showResources = false
            },
        )
    }

    val del = deleteId
    if (del != null) {
        ConfirmDialog(
            title = "Удалить запись?",
            text = "Запись будет удалена, а счётчик гаранта пересчитается.",
            confirmLabel = "Удалить",
            onDismiss = { deleteId = null },
            onConfirm = {
                vm.deletePull(del)
                deleteId = null
            },
        )
    }

    if (showClear) {
        ConfirmDialog(
            title = "Очистить баннер?",
            text = "Все записи баннера «${def.title}» будут удалены без возможности восстановления.",
            confirmLabel = "Очистить",
            onDismiss = { showClear = false },
            onConfirm = {
                vm.clearBanner(game, def.type)
                showClear = false
            },
        )
    }
}

@Composable
private fun PityCard(game: GameId, def: BannerDef, st: BannerStats) {
    val color = pityColor(st.pity5, def.soft5, def.hard5)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "${game.rarityLabel(5)} · ${def.title}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${st.pity5}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Text(
                    text = "  / ${def.hard5}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val w = maxWidth
                LinearProgressIndicator(
                    progress = { (st.pity5.toFloat() / def.hard5).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = color,
                )
                Box(
                    Modifier
                        .offset(x = w * (def.soft5.toFloat() / def.hard5))
                        .width(2.dp)
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)),
                )
            }
            Text(
                text = if (st.pity5 >= def.soft5) {
                    "Мягкий гарант уже действует (с ${def.soft5})"
                } else {
                    "Мягкий гарант с ${def.soft5} — через ${def.soft5 - st.pity5}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (def.guarantee) {
                Text(
                    text = if (st.guaranteed5) {
                        "Следующий ${game.rarityLabel(5)} гарантированно с баннера"
                    } else {
                        "Следующий ${game.rarityLabel(5)}: ${def.note.removePrefix("Гарантия ").ifBlank { "50/50" }}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = "${game.rarityLabel(4)}: ${st.pity4} / ${def.hard4}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            LinearProgressIndicator(
                progress = { (st.pity4.toFloat() / def.hard4).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = rarityColor(4),
            )
        }
    }
}

@Composable
private fun StatsCard(game: GameId, st: BannerStats) {
    SectionCard(title = "Статистика") {
        Row(Modifier.fillMaxWidth()) {
            StatTile("Всего круток", "${st.total}", Modifier.weight(1f))
            StatTile(game.rarityLabel(5), "${st.count5}", Modifier.weight(1f))
            StatTile(game.rarityLabel(4), "${st.count4}", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            StatTile(
                "В среднем на ${game.rarityLabel(5)}",
                if (st.count5 > 0) "%.1f".format(st.avg5) else "—",
                Modifier.weight(1f),
            )
            StatTile(
                "Выигрыш 50/50",
                st.winRatePercent?.let { "$it%" } ?: "—",
                Modifier.weight(1f),
            )
            StatTile(
                game.gemName,
                "${st.total * GameCatalog.GEMS_PER_PULL}",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PlannerCard(
    game: GameId,
    gems: Int,
    tickets: Int,
    def: BannerDef,
    st: BannerStats,
    onEdit: () -> Unit,
) {
    val perPull = GameCatalog.GEMS_PER_PULL
    val available = gems / perPull + tickets
    val toHard = (def.hard5 - st.pity5).coerceAtLeast(0)
    val worst = if (def.guarantee && !st.guaranteed5) toHard + def.hard5 else toHard
    val needGems = ((worst - tickets).coerceAtLeast(0) * perPull - gems).coerceAtLeast(0)

    SectionCard(title = "Планировщик") {
        Text(
            text = "${game.gemName}: $gems · круток-билетов: $tickets",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Хватит на $available круток",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "До жёсткого гаранта: $toHard" +
                if (def.guarantee && !st.guaranteed5) ", до гарантированного баннерного: $worst" else "",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = if (needGems == 0) {
                "Ресурсов достаточно, чтобы гарантированно получить ${game.rarityLabel(5)}."
            } else {
                "Не хватает: $needGems (${game.gemName.lowercase()}), это ≈ ${ceil(needGems / perPull.toDouble()).toInt()} круток."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onEdit) { Text("Изменить ресурсы") }
    }
}

@Composable
private fun HistoryRow(game: GameId, def: BannerDef, view: PullView, onDelete: () -> Unit) {
    val p = view.pull
    val accent = rarityColor(p.rarity)
    val badgeColor = if (p.rarity == 5) pityColor(view.pityAt, def.soft5, def.hard5) else accent
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(
                Modifier.size(44.dp).background(accent.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (p.rarity >= 4) "${view.pityAt}" else "·",
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                )
            }
        },
        headlineContent = {
            Text(
                text = p.name.ifBlank { "Предмет ${game.rarityLabel(p.rarity)}" },
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = {
            val tag = if (view.tag != PullTag.NONE) " · ${view.tag.label}" else ""
            Text("${game.rarityLabel(p.rarity)} · ${TimeCalc.formatDate(p.at)}$tag")
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
            }
        },
    )
}

@Composable
private fun AddPullSheet(
    game: GameId,
    def: BannerDef,
    onDismiss: () -> Unit,
    onAdd: (rarity: Int, name: String, featured: Boolean, fillers: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rarity by remember { mutableIntStateOf(5) }
    var name by remember { mutableStateOf("") }
    var featured by remember { mutableStateOf(true) }
    var count by remember { mutableStateOf("0") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Добавить · ${def.title}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            val options = listOf(3, 4, 5)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, r ->
                    SegmentedButton(
                        selected = rarity == r,
                        onClick = {
                            rarity = r
                            count = if (r == 3) "10" else "0"
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        label = { Text(game.rarityLabel(r)) },
                    )
                }
            }

            if (rarity >= 4) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Название (персонаж или предмет)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (rarity == 5 && def.guarantee) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Выпал рейт-ап", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Выключите, если проиграли 50/50 — следующий будет гарантированным",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = featured, onCheckedChange = { featured = it })
                }
            }

            OutlinedTextField(
                value = count,
                onValueChange = { v -> count = v.filter { it.isDigit() }.take(3) },
                label = {
                    Text(
                        if (rarity == 3) "Сколько обычных круток (${game.rarityLabel(3)})"
                        else "Обычных круток перед этой записью",
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = if (rarity == 3) {
                    "Так можно быстро внести текущий гарант: например, 40 обычных круток."
                } else {
                    "Например, 5★ выпал на 76-й крутке — укажите 75 (если между ними не было 4★)."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val fillers = count.toIntOrNull() ?: 0
            Button(
                onClick = { onAdd(rarity, name, rarity == 5 && featured, fillers) },
                enabled = rarity >= 4 || fillers > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Добавить") }
        }
    }
}

@Composable
private fun ResourcesDialog(
    game: GameId,
    gems: Int,
    tickets: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var gemsText by remember { mutableStateOf(gems.toString()) }
    var ticketsText by remember { mutableStateOf(tickets.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ресурсы") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = gemsText,
                    onValueChange = { v -> gemsText = v.filter { it.isDigit() }.take(7) },
                    label = { Text(game.gemName) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = ticketsText,
                    onValueChange = { v -> ticketsText = v.filter { it.isDigit() }.take(5) },
                    label = { Text("Билеты на крутки") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(gemsText.toIntOrNull() ?: 0, ticketsText.toIntOrNull() ?: 0)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

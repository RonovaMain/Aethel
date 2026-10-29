package dev.ronova.aethel.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.ronova.aethel.data.AethelDb
import dev.ronova.aethel.data.GameId
import dev.ronova.aethel.data.TaskDef
import dev.ronova.aethel.data.TaskPeriod
import dev.ronova.aethel.domain.TimeCalc
import dev.ronova.aethel.ui.AppViewModel
import dev.ronova.aethel.ui.common.ConfirmDialog
import dev.ronova.aethel.ui.common.EmptyHint
import dev.ronova.aethel.ui.common.GameSwitcher
import dev.ronova.aethel.ui.common.HorizontalChips
import dev.ronova.aethel.ui.common.SectionCard
import dev.ronova.aethel.ui.common.TimeDialog
import dev.ronova.aethel.ui.common.rememberNow

private val tabTitles = listOf("Ежедневные", "Еженедельные", "Периодические")

private fun matchesTab(task: TaskDef, tab: Int): Boolean = when (tab) {
    0 -> task.period == TaskPeriod.DAILY
    1 -> task.period == TaskPeriod.WEEKLY
    else -> task.period == TaskPeriod.HALF_MONTH || task.period == TaskPeriod.MONTHLY
}

@Composable
fun TasksScreen(
    vm: AppViewModel,
    db: AethelDb,
    game: GameId,
    padding: PaddingValues,
) {
    val now by rememberNow()
    val g = db.game(game)
    var tab by rememberSaveable { mutableStateOf(0) }
    var showTime by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskDef?>(null) }
    var deleting by remember { mutableStateOf<TaskDef?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }

    val tasks = g.tasks.filter { matchesTab(it, tab) }
    val doneCount = tasks.count { TimeCalc.isTaskDone(g, it, now) }

    val nextForTab = when (tab) {
        0 -> TimeCalc.nextReset(now, TaskPeriod.DAILY, g.dailyResetMinutes, g.weeklyResetDay)
        1 -> TimeCalc.nextReset(now, TaskPeriod.WEEKLY, g.dailyResetMinutes, g.weeklyResetDay)
        else -> minOf(
            TimeCalc.nextReset(now, TaskPeriod.HALF_MONTH, g.dailyResetMinutes, g.weeklyResetDay),
            TimeCalc.nextReset(now, TaskPeriod.MONTHLY, g.dailyResetMinutes, g.weeklyResetDay),
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { GameSwitcher(selected = game, onSelect = { vm.selectGame(it) }) }

        item {
            SectionCard(title = "Время обновления") {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Каждый день в ${TimeCalc.formatMinutes(g.dailyResetMinutes)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "По вашему времени. Укажите, когда в вашем регионе игра обновляет поручения.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { showTime = true }) { Text("Изменить") }
                }
                Text(
                    text = "Еженедельный сброс — день недели:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                HorizontalChips {
                    TimeCalc.weekDayNames.forEachIndexed { i, name ->
                        FilterChip(
                            selected = g.weeklyResetDay == i + 1,
                            onClick = { vm.setWeeklyDay(game, i + 1) },
                            label = { Text(name) },
                        )
                    }
                }
            }
        }

        item {
            val options = tabTitles
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, title ->
                    SegmentedButton(
                        selected = tab == index,
                        onClick = { tab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        label = { Text(title, maxLines = 1) },
                    )
                }
            }
        }

        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Выполнено $doneCount из ${tasks.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Обновление через ${TimeCalc.formatCountdown(nextForTab - now)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Сбросить отметки") },
                            onClick = {
                                showMenu = false
                                confirmReset = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Вернуть стандартные поручения") },
                            onClick = {
                                showMenu = false
                                confirmRestore = true
                            },
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { if (tasks.isEmpty()) 0f else doneCount.toFloat() / tasks.size },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
          }
        }

        if (tasks.isEmpty()) {
            item { EmptyHint("В этой вкладке пока нет поручений") }
        }

        items(tasks, key = { it.id }) { task ->
            val done = TimeCalc.isTaskDone(g, task, now)
            var itemMenu by remember { mutableStateOf(false) }
            ListItem(
                modifier = Modifier.clickable { vm.toggleTask(game, task.id) },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                leadingContent = {
                    Checkbox(checked = done, onCheckedChange = { vm.toggleTask(game, task.id) })
                },
                headlineContent = {
                    Text(
                        text = task.title,
                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                    )
                },
                supportingContent = {
                    val extra = if (tab == 2) task.period.title else ""
                    val line = listOf(task.note, extra).filter { it.isNotBlank() }.joinToString(" · ")
                    if (line.isNotBlank()) Text(line)
                },
                trailingContent = {
                    Box {
                        IconButton(onClick = { itemMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Действия")
                        }
                        DropdownMenu(expanded = itemMenu, onDismissRequest = { itemMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Изменить") },
                                onClick = {
                                    itemMenu = false
                                    editing = task
                                    showEditor = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Удалить") },
                                onClick = {
                                    itemMenu = false
                                    deleting = task
                                },
                            )
                        }
                    }
                },
            )
        }

        item {
            OutlinedButton(
                onClick = {
                    editing = null
                    showEditor = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Добавить своё поручение")
            }
        }
    }

    if (showTime) {
        TimeDialog(
            title = "Время ежедневного обновления",
            initialMinutes = g.dailyResetMinutes,
            onDismiss = { showTime = false },
            onConfirm = {
                vm.setResetMinutes(game, it)
                showTime = false
            },
        )
    }

    if (showEditor) {
        val e = editing
        TaskEditorDialog(
            initial = e,
            defaultPeriod = when (tab) {
                0 -> TaskPeriod.DAILY
                1 -> TaskPeriod.WEEKLY
                else -> TaskPeriod.MONTHLY
            },
            onDismiss = { showEditor = false },
            onSave = { title, period, note ->
                if (e == null) vm.addTask(game, title, period, note)
                else vm.updateTask(game, e.id, title, period, note)
                showEditor = false
            },
        )
    }

    val del = deleting
    if (del != null) {
        ConfirmDialog(
            title = "Удалить поручение?",
            text = "«${del.title}» будет удалено из списка. Стандартные поручения можно вернуть через меню.",
            confirmLabel = "Удалить",
            onDismiss = { deleting = null },
            onConfirm = {
                vm.deleteTask(game, del.id)
                deleting = null
            },
        )
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Сбросить отметки?",
            text = "Все поручения этой игры снова станут невыполненными.",
            confirmLabel = "Сбросить",
            onDismiss = { confirmReset = false },
            onConfirm = {
                vm.resetTaskProgress(game)
                confirmReset = false
            },
        )
    }

    if (confirmRestore) {
        ConfirmDialog(
            title = "Вернуть стандартные?",
            text = "Список стандартных поручений будет восстановлен. Ваши собственные поручения останутся.",
            confirmLabel = "Вернуть",
            onDismiss = { confirmRestore = false },
            onConfirm = {
                vm.restoreDefaultTasks(game)
                confirmRestore = false
            },
        )
    }
}

@Composable
private fun TaskEditorDialog(
    initial: TaskDef?,
    defaultPeriod: TaskPeriod,
    onDismiss: () -> Unit,
    onSave: (String, TaskPeriod, String) -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var period by remember { mutableStateOf(initial?.period ?: defaultPeriod) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новое поручение" else "Изменить поручение") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("Название") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(120) },
                    label = { Text("Заметка (необязательно)") },
                    singleLine = true,
                )
                Text("Как часто сбрасывается:", style = MaterialTheme.typography.bodyMedium)
                HorizontalChips {
                    TaskPeriod.entries.forEach { p ->
                        FilterChip(
                            selected = period == p,
                            onClick = { period = p },
                            label = { Text(p.title) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, period, note) },
                enabled = title.isNotBlank(),
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

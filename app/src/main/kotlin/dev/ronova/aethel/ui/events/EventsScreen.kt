package dev.ronova.aethel.ui.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ronova.aethel.data.AethelDb
import dev.ronova.aethel.data.GameCatalog
import dev.ronova.aethel.data.GameId
import dev.ronova.aethel.data.Repeat
import dev.ronova.aethel.data.TimerEvent
import dev.ronova.aethel.domain.TimeCalc
import dev.ronova.aethel.ui.AppViewModel
import dev.ronova.aethel.ui.common.ConfirmDialog
import dev.ronova.aethel.ui.common.DateDialog
import dev.ronova.aethel.ui.common.EmptyHint
import dev.ronova.aethel.ui.common.GameSwitcher
import dev.ronova.aethel.ui.common.HorizontalChips
import dev.ronova.aethel.ui.common.TimeDialog
import dev.ronova.aethel.ui.common.rememberNow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private val remindOptions = listOf(-1, 0, 15, 60, 180, 1440)

@Composable
fun EventsScreen(
    vm: AppViewModel,
    db: AethelDb,
    game: GameId,
    padding: PaddingValues,
) {
    val now by rememberNow()
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TimerEvent?>(null) }
    var deleting by remember { mutableStateOf<TimerEvent?>(null) }

    val all = db.events.filter { it.game == game }
    val withNext = all.map { it to TimeCalc.nextOccurrence(it, now) }
    val upcoming = withNext.filter { it.second != null }.sortedBy { it.second }
    val finished = withNext.filter { it.second == null }.map { it.first }.sortedByDescending { it.at }

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
            Text(
                text = "Добавляйте конец баннера, обновление версии, сброс бездны и любые другие даты — приложение напомнит заранее. Время указывается по вашему часовому поясу.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Button(
                onClick = {
                    editing = null
                    showEditor = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Добавить событие")
            }
        }

        if (upcoming.isEmpty() && finished.isEmpty()) {
            item { EmptyHint("Для ${game.short} событий пока нет") }
        }

        items(upcoming, key = { it.first.id }) { (event, next) ->
            EventCard(
                event = event,
                countdown = TimeCalc.formatCountdown((next ?: now) - now),
                nextAt = next ?: event.at,
                onClick = {
                    editing = event
                    showEditor = true
                },
                onToggle = { vm.setEventEnabled(event.id, it) },
                onDelete = { deleting = event },
            )
        }

        if (finished.isNotEmpty()) {
            item {
                Text(
                    text = "Завершённые",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(finished, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    countdown = "завершено",
                    nextAt = event.at,
                    onClick = {
                        editing = event
                        showEditor = true
                    },
                    onToggle = null,
                    onDelete = { deleting = event },
                )
            }
        }
    }

    if (showEditor) {
        val e = editing
        EventEditorDialog(
            initial = e,
            onDismiss = { showEditor = false },
            onSave = { title, at, repeat, remind ->
                vm.saveEvent(e?.id, game, title, at, repeat, remind)
                showEditor = false
            },
        )
    }

    val del = deleting
    if (del != null) {
        ConfirmDialog(
            title = "Удалить событие?",
            text = "«${del.title}» и его напоминания будут удалены.",
            confirmLabel = "Удалить",
            onDismiss = { deleting = null },
            onConfirm = {
                vm.deleteEvent(del.id)
                deleting = null
            },
        )
    }
}

@Composable
private fun EventCard(
    event: TimerEvent,
    countdown: String,
    nextAt: Long,
    onClick: () -> Unit,
    onToggle: ((Boolean) -> Unit)?,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (onToggle != null) "через $countdown" else countdown,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = TimeCalc.formatDateTime(nextAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val details = listOf(
                    if (event.repeat != Repeat.NONE) event.repeat.title else "",
                    TimeCalc.remindLabel(event.remindBeforeMin),
                ).filter { it.isNotBlank() }.joinToString(" · ")
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (onToggle != null) {
                    Switch(checked = event.enabled, onCheckedChange = onToggle)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                }
            }
        }
    }
}

@Composable
private fun EventEditorDialog(
    initial: TimerEvent?,
    onDismiss: () -> Unit,
    onSave: (title: String, at: Long, repeat: Repeat, remind: Int) -> Unit,
) {
    val zone = TimeCalc.zone
    val startDateTime = remember(initial) {
        if (initial != null) {
            Instant.ofEpochMilli(initial.at).atZone(zone).toLocalDateTime()
        } else {
            LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0)
        }
    }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var date by remember { mutableStateOf(startDateTime.toLocalDate()) }
    var minutes by remember { mutableIntStateOf(startDateTime.hour * 60 + startDateTime.minute) }
    var repeat by remember { mutableStateOf(initial?.repeat ?: Repeat.NONE) }
    var remind by remember { mutableIntStateOf(initial?.remindBeforeMin ?: 60) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новое событие" else "Изменить событие") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HorizontalChips {
                    GameCatalog.eventPresets.forEach { preset ->
                        FilterChip(
                            selected = title == preset,
                            onClick = { title = preset },
                            label = { Text(preset) },
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    label = { Text("Название") },
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                        Text(TimeCalc.formatDate(date.atStartOfDay(zone).toInstant().toEpochMilli()))
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
                        Text(TimeCalc.formatMinutes(minutes))
                    }
                }
                Text("Повтор", style = MaterialTheme.typography.labelLarge)
                HorizontalChips {
                    Repeat.entries.forEach { r ->
                        FilterChip(selected = repeat == r, onClick = { repeat = r }, label = { Text(r.title) })
                    }
                }
                Text("Напоминание", style = MaterialTheme.typography.labelLarge)
                HorizontalChips {
                    remindOptions.forEach { m ->
                        FilterChip(
                            selected = remind == m,
                            onClick = { remind = m },
                            label = { Text(TimeCalc.remindLabel(m)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val at = LocalDateTime.of(date, LocalTime.of(minutes / 60, minutes % 60))
                        .atZone(zone).toInstant().toEpochMilli()
                    onSave(title, at, repeat, remind)
                },
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )

    if (showDate) {
        DateDialog(
            initialDate = date,
            onDismiss = { showDate = false },
            onConfirm = {
                date = it
                showDate = false
            },
        )
    }
    if (showTime) {
        TimeDialog(
            title = "Время события",
            initialMinutes = minutes,
            onDismiss = { showTime = false },
            onConfirm = {
                minutes = it
                showTime = false
            },
        )
    }
}

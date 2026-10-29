package dev.ronova.aethel.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.ronova.aethel.data.AethelDb
import dev.ronova.aethel.data.AppTab
import dev.ronova.aethel.data.GameId
import dev.ronova.aethel.data.PaletteId
import dev.ronova.aethel.data.ThemeMode
import dev.ronova.aethel.domain.TimeCalc
import dev.ronova.aethel.notify.Notifier
import dev.ronova.aethel.ui.AppViewModel
import dev.ronova.aethel.ui.common.BackupCard
import dev.ronova.aethel.ui.common.ConfirmDialog
import dev.ronova.aethel.ui.common.GameSwitcher
import dev.ronova.aethel.ui.common.HorizontalChips
import dev.ronova.aethel.ui.common.NumberDialog
import dev.ronova.aethel.ui.common.SectionCard
import dev.ronova.aethel.ui.common.TimeDialog
import dev.ronova.aethel.ui.theme.dynamicColorsSupported
import dev.ronova.aethel.ui.theme.paletteSwatch
import dev.ronova.aethel.ui.theme.paletteSwatchSecondary

private enum class TimeTarget { RESET, UNDONE, SUB }

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    db: AethelDb,
    initialGame: GameId,
    padding: PaddingValues,
) {
    val context = LocalContext.current
    val s = db.settings
    var game by remember { mutableStateOf(initialGame) }
    val g = db.game(game)

    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }
    var showEnergyMax by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }

    // перечитываем состояние разрешений, когда пользователь возвращается из системных настроек
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val notifGranted = remember(tick) { Notifier.hasPermission(context) }
    val exactOk = remember(tick) { canUseExactAlarms(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { tick++ }

    val dark = when (s.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
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
        // ---------- Оформление ----------
        item {
            SectionCard(title = "Оформление") {
                Text("Тема", style = MaterialTheme.typography.labelLarge)
                val modes = ThemeMode.entries
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = s.themeMode == mode,
                            onClick = { vm.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                            label = { Text(mode.title, maxLines = 1) },
                        )
                    }
                }
                SwitchRow(
                    title = "Чисто чёрный фон",
                    subtitle = "AMOLED: для тёмной темы",
                    checked = s.amoled,
                    onChange = { vm.setAmoled(it) },
                )
                Text("Цветовая схема", style = MaterialTheme.typography.labelLarge)
                HorizontalChips {
                    PaletteId.entries.forEach { id ->
                        val enabled = id != PaletteId.DYNAMIC || dynamicColorsSupported
                        PaletteSwatch(
                            id = id,
                            dark = dark,
                            selected = s.palette == id,
                            enabled = enabled,
                            onClick = { vm.setPalette(id) },
                        )
                    }
                }
                if (!dynamicColorsSupported) {
                    Text(
                        text = "Динамические цвета Material You доступны на Android 12 и новее. На этом устройстве используйте готовые схемы.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Сакура — нежно-розовая схема: светлая розово-белая и мягкая розовая ночная.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---------- Запуск ----------
        item {
            SectionCard(title = "Запуск приложения") {
                Text("Игра по умолчанию", style = MaterialTheme.typography.labelLarge)
                GameSwitcher(selected = s.defaultGame, onSelect = { vm.setDefaultGame(it) })
                Text("Стартовая вкладка", style = MaterialTheme.typography.labelLarge)
                HorizontalChips {
                    AppTab.entries.forEach { tab ->
                        FilterChip(
                            selected = s.startTab == tab,
                            onClick = { vm.setStartTab(tab) },
                            label = { Text(tab.title) },
                        )
                    }
                }
            }
        }

        // ---------- Уведомления ----------
        item {
            SectionCard(title = "Уведомления") {
                SwitchRow(
                    title = "Напоминания",
                    subtitle = "Главный переключатель всех уведомлений",
                    checked = s.notificationsEnabled,
                    onChange = { vm.setNotificationsEnabled(it) },
                )
                if (!notifGranted) {
                    Text(
                        text = "Разрешение на уведомления не выдано — напоминания не будут показываться.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }) { Text("Разрешить") }
                        OutlinedButton(onClick = { openNotificationSettings(context) }) { Text("Настройки системы") }
                    }
                }
                if (!exactOk) {
                    Text(
                        text = "Точные будильники отключены: напоминания могут приходить с небольшой задержкой. Разрешите их для точного времени.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = { openExactAlarmSettings(context) }) { Text("Разрешить точные будильники") }
                }
            }
        }

        // ---------- Настройки игры ----------
        item {
            SectionCard(title = "Настройки игры") {
                GameSwitcher(selected = game, onSelect = { game = it })

                // время обновления
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Обновление поручений", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "По вашему времени",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { timeTarget = TimeTarget.RESET }) {
                        Text(TimeCalc.formatMinutes(g.dailyResetMinutes))
                    }
                }
                Text("День еженедельного сброса", style = MaterialTheme.typography.labelLarge)
                HorizontalChips {
                    TimeCalc.weekDayNames.forEachIndexed { i, name ->
                        FilterChip(
                            selected = g.weeklyResetDay == i + 1,
                            onClick = { vm.setWeeklyDay(game, i + 1) },
                            label = { Text(name) },
                        )
                    }
                }

                SwitchRow(
                    title = "Сообщать об обновлении поручений",
                    subtitle = "В ${TimeCalc.formatMinutes(g.dailyResetMinutes)} каждый день",
                    checked = g.remindDailyReset,
                    onChange = { vm.setRemindDailyReset(game, it) },
                )

                SwitchRow(
                    title = "Напомнить, если не всё сделано",
                    subtitle = "В ${TimeCalc.formatMinutes(g.undoneRemindMinutes)}, только если есть невыполненные",
                    checked = g.remindUndone,
                    onChange = { vm.setRemindUndone(game, it) },
                )
                if (g.remindUndone) {
                    TextButton(onClick = { timeTarget = TimeTarget.UNDONE }) { Text("Время напоминания") }
                }

                SwitchRow(
                    title = "${game.energyName}: заполнена",
                    subtitle = "Максимум: ${g.energy.max}",
                    checked = g.remindEnergyFull,
                    onChange = { vm.setRemindEnergy(game, it) },
                )
                TextButton(onClick = { showEnergyMax = true }) { Text("Изменить максимум энергии") }

                SwitchRow(
                    title = "${game.subShort}: забрать награду",
                    subtitle = "В ${TimeCalc.formatMinutes(g.subClaimMinutes)}, пока подписка активна",
                    checked = g.remindSubClaim,
                    onChange = { vm.setRemindSub(game, it) },
                )
                if (g.remindSubClaim) {
                    TextButton(onClick = { timeTarget = TimeTarget.SUB }) { Text("Время напоминания") }
                }
            }
        }

        // ---------- Данные ----------
        item { BackupCard(vm) }

        item {
            SectionCard(title = "Опасная зона") {
                Text(
                    text = "Удаляет профиль, крутки, поручения и события. Сначала сделайте экспорт, если данные нужны.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { confirmWipe = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) { Text("Удалить все данные") }
            }
        }
    }

    when (timeTarget) {
        TimeTarget.RESET -> TimeDialog(
            title = "Обновление поручений",
            initialMinutes = g.dailyResetMinutes,
            onDismiss = { timeTarget = null },
            onConfirm = {
                vm.setResetMinutes(game, it)
                timeTarget = null
            },
        )

        TimeTarget.UNDONE -> TimeDialog(
            title = "Напоминание о невыполненных",
            initialMinutes = g.undoneRemindMinutes,
            onDismiss = { timeTarget = null },
            onConfirm = {
                vm.setUndoneRemindMinutes(game, it)
                timeTarget = null
            },
        )

        TimeTarget.SUB -> TimeDialog(
            title = "Напоминание о награде",
            initialMinutes = g.subClaimMinutes,
            onDismiss = { timeTarget = null },
            onConfirm = {
                vm.setSubClaimMinutes(game, it)
                timeTarget = null
            },
        )

        null -> Unit
    }

    if (showEnergyMax) {
        NumberDialog(
            title = game.energyName,
            label = "Максимум",
            initial = g.energy.max,
            onDismiss = { showEnergyMax = false },
            onConfirm = {
                if (it > 0) vm.setEnergyMax(game, it)
                showEnergyMax = false
            },
        )
    }

    if (confirmWipe) {
        ConfirmDialog(
            title = "Удалить все данные?",
            text = "Это действие нельзя отменить. Приложение вернётся к первоначальному состоянию.",
            confirmLabel = "Удалить",
            onDismiss = { confirmWipe = false },
            onConfirm = {
                vm.resetAllData()
                confirmWipe = false
            },
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PaletteSwatch(
    id: PaletteId,
    dark: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val ring = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val fill: Brush = if (id == PaletteId.DYNAMIC) {
        Brush.linearGradient(listOf(Color(0xFFFF8A65), Color(0xFFBA68C8), Color(0xFF4DD0E1)))
    } else {
        Brush.linearGradient(listOf(paletteSwatch(id, dark), paletteSwatchSecondary(id, dark)))
    }
    Column(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .border(if (selected) 3.dp else 1.dp, ring, CircleShape)
                .padding(5.dp)
                .background(fill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
            }
        }
        Text(
            text = id.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private fun canUseExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return am.canScheduleExactAlarms()
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: Exception) {
            openAppSettings(context)
        }
    }
}

private fun openNotificationSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: Exception) {
        openAppSettings(context)
    }
}

private fun openAppSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: Exception) {
    }
}

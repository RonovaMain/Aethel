package dev.ronova.aethel.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ronova.aethel.R
import dev.ronova.aethel.data.AethelDb
import dev.ronova.aethel.data.GameId
import dev.ronova.aethel.ui.AppViewModel
import dev.ronova.aethel.ui.common.BackupCard
import dev.ronova.aethel.ui.common.SectionCard
import dev.ronova.aethel.ui.common.StatTile
import dev.ronova.aethel.ui.common.TextDialog

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    db: AethelDb,
    padding: PaddingValues,
    onOpenSettings: () -> Unit,
) {
    var editNick by remember { mutableStateOf(false) }
    var editGame by remember { mutableStateOf<GameId?>(null) }
    var showAbout by remember { mutableStateOf(false) }

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
        // ---- шапка профиля ----
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Box(Modifier.fillMaxWidth().padding(20.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val nick = db.profile.nickname
                        Box(
                            Modifier
                                .size(84.dp)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF7C5CFF), Color(0xFF35C8FF))),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = nick.trim().firstOrNull()?.uppercase() ?: "A",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = nick.ifBlank { "Путешественник" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            IconButton(onClick = { editNick = true }) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Изменить имя",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Text(
                            text = "Локальный профиль · без регистрации",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { showAbout = true },
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = "О программе")
                    }
                }
            }
        }

        // ---- основная игра ----
        item {
            SectionCard(title = "Основная игра") {
                Text(
                    text = "Эта игра будет открываться при запуске приложения.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GameId.entries.forEach { game ->
                    val selected = db.settings.defaultGame == game
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.setDefaultGame(game) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = { vm.setDefaultGame(game) })
                        Text(
                            text = game.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }

        // ---- аккаунты ----
        item {
            SectionCard(title = "Аккаунты") {
                GameId.entries.forEach { game ->
                    val g = db.game(game)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(game.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            val info = listOfNotNull(
                                g.uid.takeIf { it.isNotBlank() }?.let { "UID $it" },
                                g.level.takeIf { it.isNotBlank() }?.let { "Ур. $it" },
                            ).joinToString(" · ")
                            Text(
                                text = info.ifBlank { "Не заполнено" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { editGame = game }) { Text("Изменить") }
                    }
                }
            }
        }

        // ---- сводка ----
        item {
            val total = db.pulls.size
            val five = db.pulls.count { it.rarity == 5 }
            val four = db.pulls.count { it.rarity == 4 }
            SectionCard(title = "Сводка по всем играм") {
                Row(Modifier.fillMaxWidth()) {
                    StatTile("Круток", "$total", Modifier.weight(1f))
                    StatTile("5★ / S", "$five", Modifier.weight(1f))
                    StatTile("4★ / A", "$four", Modifier.weight(1f))
                }
            }
        }

        item { BackupCard(vm) }

        item {
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Настройки")
            }
        }
    }

    if (editNick) {
        TextDialog(
            title = "Ваше имя",
            label = "Имя или ник",
            initial = db.profile.nickname,
            onDismiss = { editNick = false },
            onConfirm = {
                vm.setNickname(it)
                editNick = false
            },
        )
    }

    val eg = editGame
    if (eg != null) {
        val g = db.game(eg)
        AccountDialog(
            game = eg,
            uid = g.uid,
            level = g.level,
            onDismiss = { editGame = null },
            onConfirm = { uid, level ->
                vm.setUid(eg, uid, level)
                editGame = null
            },
        )
    }

    if (showAbout) {
        AboutSheet(onDismiss = { showAbout = false })
    }
}

@Composable
private fun AccountDialog(
    game: GameId,
    uid: String,
    level: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var uidText by remember { mutableStateOf(uid) }
    var levelText by remember { mutableStateOf(level) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(game.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uidText,
                    onValueChange = { uidText = it.take(20) },
                    label = { Text("UID") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = levelText,
                    onValueChange = { levelText = it.take(4) },
                    label = { Text("Уровень") },
                    singleLine = true,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(uidText, levelText) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun AboutSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_aethel_star),
                contentDescription = null,
                modifier = Modifier.size(84.dp),
            )
            Text(
                text = "Aethel",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Версия $version",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Локальный трекер круток, поручений и событий для Genshin Impact, Honkai: Star Rail и Zenless Zone Zero. Работает без интернета и регистрации — все данные остаются на вашем устройстве.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Автор",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "RonovaDev",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(
                    onClick = { runCatching { uriHandler.openUri(Links.TELEGRAM) } },
                    label = { Text("Telegram") },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
                AssistChip(
                    onClick = { runCatching { uriHandler.openUri(Links.GITHUB) } },
                    label = { Text("GitHub") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_github),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = LocalContentColor.current,
                        )
                    },
                )
            }
            Text(
                text = "Неофициальное приложение. Не связано с HoYoverse. Genshin Impact, Honkai: Star Rail и Zenless Zone Zero — товарные знаки их владельцев.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

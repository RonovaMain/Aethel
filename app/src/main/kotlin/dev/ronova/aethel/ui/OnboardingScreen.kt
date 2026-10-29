package dev.ronova.aethel.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ronova.aethel.R
import dev.ronova.aethel.data.GameId
import dev.ronova.aethel.domain.TimeCalc
import dev.ronova.aethel.notify.Notifier
import dev.ronova.aethel.ui.common.TimeDialog

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    val context = LocalContext.current
    var nickname by remember { mutableStateOf("") }
    var game by remember { mutableStateOf(GameId.GENSHIN) }
    var resetMinutes by remember { mutableIntStateOf(4 * 60) }
    var showTime by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Image(
            painter = painterResource(R.drawable.ic_aethel_star),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
        )
        Text(
            text = "Aethel",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Крутки, поручения и таймеры для Genshin Impact, Honkai: Star Rail и Zenless Zone Zero. Всё работает офлайн, без регистрации.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it.take(30) },
            label = { Text("Как вас называть? (необязательно)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Какую игру открывать по умолчанию?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                GameId.entries.forEach { g ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { game = g },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = game == g, onClick = { game = g })
                        Text(g.title, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Text(
                    text = "Изменить можно позже в профиле или настройках.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Когда обновляются поручения?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Укажите время по вашим часам. Обычно игра обновляется в 04:00 по времени сервера — переведите его в свой часовой пояс. Приложение запомнит это время и будет присылать напоминания.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { showTime = true }) {
                    Text("Обновление в ${TimeCalc.formatMinutes(resetMinutes)}")
                }
            }
        }

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifier.hasPermission(context)) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                vm.completeOnboarding(nickname, game, resetMinutes)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Начать") }
    }

    if (showTime) {
        TimeDialog(
            title = "Время обновления поручений",
            initialMinutes = resetMinutes,
            onDismiss = { showTime = false },
            onConfirm = {
                resetMinutes = it
                showTime = false
            },
        )
    }
}

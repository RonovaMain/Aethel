package dev.ronova.aethel.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ronova.aethel.data.AethelDb
import dev.ronova.aethel.data.AppTab
import dev.ronova.aethel.ui.events.EventsScreen
import dev.ronova.aethel.ui.home.HomeScreen
import dev.ronova.aethel.ui.profile.ProfileScreen
import dev.ronova.aethel.ui.settings.SettingsScreen
import dev.ronova.aethel.ui.tasks.TasksScreen
import dev.ronova.aethel.ui.theme.AethelTheme
import dev.ronova.aethel.ui.wishes.WishesScreen

@Composable
fun AethelRoot(vm: AppViewModel = viewModel()) {
    val db by vm.db.collectAsState()
    val s = db.settings
    AethelTheme(mode = s.themeMode, palette = s.palette, amoled = s.amoled) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (!db.profile.onboarded) {
                OnboardingScreen(vm)
            } else {
                MainScaffold(vm, db)
            }
        }
    }
}

private fun iconFor(tab: AppTab): ImageVector = when (tab) {
    AppTab.HOME -> Icons.Filled.Home
    AppTab.WISHES -> Icons.Filled.AutoAwesome
    AppTab.TASKS -> Icons.Filled.CheckCircle
    AppTab.EVENTS -> Icons.Filled.Schedule
    AppTab.PROFILE -> Icons.Filled.Person
}

@Composable
private fun MainScaffold(vm: AppViewModel, db: AethelDb) {
    val game by vm.currentGame.collectAsState()
    var tabIndex by rememberSaveable { mutableIntStateOf(db.settings.startTab.ordinal) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val tabs = AppTab.entries
    val tab = tabs[tabIndex.coerceIn(0, tabs.size - 1)]

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (showSettings) {
                TopAppBar(
                    title = { Text("Настройки") },
                    navigationIcon = {
                        IconButton(onClick = { showSettings = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(if (tab == AppTab.HOME) "Aethel" else tab.title) },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Настройки")
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (!showSettings) {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = t == tab,
                            onClick = { tabIndex = t.ordinal },
                            icon = { Icon(iconFor(t), contentDescription = t.title) },
                            label = { Text(t.title, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (showSettings) {
            SettingsScreen(vm = vm, db = db, initialGame = game, padding = padding)
        } else {
            when (tab) {
                AppTab.HOME -> HomeScreen(
                    vm = vm, db = db, game = game, padding = padding,
                    onNavigate = { tabIndex = it.ordinal },
                )

                AppTab.WISHES -> WishesScreen(vm = vm, db = db, game = game, padding = padding)
                AppTab.TASKS -> TasksScreen(vm = vm, db = db, game = game, padding = padding)
                AppTab.EVENTS -> EventsScreen(vm = vm, db = db, game = game, padding = padding)
                AppTab.PROFILE -> ProfileScreen(
                    vm = vm, db = db, padding = padding,
                    onOpenSettings = { showSettings = true },
                )
            }
        }
    }
}

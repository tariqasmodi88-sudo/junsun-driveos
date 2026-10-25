package it.driveos.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import it.driveos.launcher.apps.AppKey
import it.driveos.launcher.system.LaunchResult
import it.driveos.launcher.system.Launcher
import it.driveos.launcher.ui.AppDrawerScreen
import it.driveos.launcher.ui.AppPickerDialog
import it.driveos.launcher.ui.BottomBar
import it.driveos.launcher.ui.DashboardScreen
import it.driveos.launcher.ui.DeviceInfoScreen
import it.driveos.launcher.ui.SettingsScreen
import it.driveos.launcher.ui.TextInputDialog
import it.driveos.launcher.ui.theme.DriveOSTheme
import kotlinx.coroutines.launch

enum class Screen { DASHBOARD, APPS, SETTINGS, DEVICE_INFO }

private enum class PickerTarget(val title: String) {
    NAV("App di navigazione"),
    MUSIC("App musica"),
    PHONE("App telefono"),
}

private enum class TextTarget { HOME_ADDRESS, DESTINATION }

/**
 * Unica activity: schermata Home (categoria HOME) e voce nell'elenco app (LAUNCHER).
 * Non tocca radio, CAN, retrocamera o impostazioni di sistema: avvia solo altre app.
 */
class MainActivity : ComponentActivity() {

    private var screen by mutableStateOf(Screen.DASHBOARD)
    private val driveApp: DriveOSApp get() = application as DriveOSApp
    private var receiverRegistered = false

    /** Aggiorna l'elenco quando un'app viene installata, rimossa o aggiornata. */
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lifecycleScope.launch { driveApp.appRepository.refresh() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerPackageReceiver()
        lifecycleScope.launch { driveApp.appRepository.refresh() }
        setContent { DriveOSRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Tasto Home premuto mentre DriveOS e' gia' aperto: torna alla dashboard.
        if (intent.hasCategory(Intent.CATEGORY_HOME)) screen = Screen.DASHBOARD
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            runCatching { unregisterReceiver(packageReceiver) }
            receiverRegistered = false
        }
        super.onDestroy()
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
    }

    @Composable
    private fun DriveOSRoot() {
        val settingsState by driveApp.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
        val apps by driveApp.appRepository.apps.collectAsStateWithLifecycle()
        val loaded by driveApp.appRepository.loaded.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        var picker by remember { mutableStateOf<PickerTarget?>(null) }
        var textDialog by remember { mutableStateOf<TextTarget?>(null) }

        val s = settingsState
        if (s == null) {
            // Preferenze non ancora lette (pochi millisecondi): schermo nero, nessun valore inventato.
            Box(Modifier.fillMaxSize().background(Color.Black))
            return
        }

        val repo = driveApp.settingsRepository
        val context: Context = this

        fun report(result: LaunchResult) {
            if (result is LaunchResult.Error) scope.launch { snackbar.showSnackbar(result.message) }
        }

        fun openOrPick(key: AppKey?, target: PickerTarget) {
            if (key == null) picker = target else report(Launcher.launchApp(context, key))
        }

        fun navigateTo(destination: String) {
            val nav = s.navApp
            if (nav == null) {
                picker = PickerTarget.NAV
            } else {
                report(Launcher.navigate(context, nav.packageName, destination))
            }
        }

        fun goHome() {
            if (s.homeAddress.isBlank()) textDialog = TextTarget.HOME_ADDRESS else navigateTo(s.homeAddress)
        }

        // Il tasto Indietro riporta alla dashboard; sulla dashboard non fa nulla (come ogni launcher).
        BackHandler(enabled = true) {
            if (screen != Screen.DASHBOARD) screen = Screen.DASHBOARD
        }

        DriveOSTheme(s.themeMode, s.accentIndex) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (screen) {
                            Screen.DASHBOARD -> DashboardScreen(
                                settings = s,
                                apps = apps,
                                onOpenNav = { openOrPick(s.navApp, PickerTarget.NAV) },
                                onChooseNav = { picker = PickerTarget.NAV },
                                onGoHome = { goHome() },
                                onSearchDestination = { textDialog = TextTarget.DESTINATION },
                                onLaunch = { report(Launcher.launchApp(context, it)) },
                                onOpenApps = { screen = Screen.APPS },
                            )

                            Screen.APPS -> AppDrawerScreen(
                                apps = apps,
                                loaded = loaded,
                                favorites = s.favorites.toSet(),
                                onLaunch = { report(Launcher.launchApp(context, it)) },
                                onToggleFavorite = { key -> scope.launch { repo.toggleFavorite(key) } },
                                onBack = { screen = Screen.DASHBOARD },
                            )

                            Screen.SETTINGS -> SettingsScreen(
                                settings = s,
                                appLabel = { key -> apps.firstOrNull { it.key == key }?.label },
                                onPickNav = { picker = PickerTarget.NAV },
                                onEditHome = { textDialog = TextTarget.HOME_ADDRESS },
                                onPickMusic = { picker = PickerTarget.MUSIC },
                                onPickPhone = { picker = PickerTarget.PHONE },
                                onThemeMode = { mode -> scope.launch { repo.setThemeMode(mode) } },
                                onAccent = { index -> scope.launch { repo.setAccent(index) } },
                                onHomeSettings = { report(Launcher.openSystemSettings(context, Settings.ACTION_HOME_SETTINGS)) },
                                onAndroidSettings = { report(Launcher.openSystemSettings(context, Settings.ACTION_SETTINGS)) },
                                onDeviceInfo = { screen = Screen.DEVICE_INFO },
                                onBack = { screen = Screen.DASHBOARD },
                            )

                            Screen.DEVICE_INFO -> DeviceInfoScreen(onBack = { screen = Screen.SETTINGS })
                        }
                    }
                    BottomBar(
                        current = screen,
                        onHome = { screen = Screen.DASHBOARD },
                        onNav = { openOrPick(s.navApp, PickerTarget.NAV) },
                        onMusic = { openOrPick(s.musicApp, PickerTarget.MUSIC) },
                        onPhone = { openOrPick(s.phoneApp, PickerTarget.PHONE) },
                        onApps = { screen = Screen.APPS },
                        onSettings = { screen = Screen.SETTINGS },
                    )
                }
            }

            picker?.let { target ->
                AppPickerDialog(
                    title = target.title,
                    apps = apps,
                    selected = when (target) {
                        PickerTarget.NAV -> s.navApp
                        PickerTarget.MUSIC -> s.musicApp
                        PickerTarget.PHONE -> s.phoneApp
                    },
                    onPick = { entry ->
                        scope.launch {
                            when (target) {
                                PickerTarget.NAV -> repo.setNavApp(entry?.key)
                                PickerTarget.MUSIC -> repo.setMusicApp(entry?.key)
                                PickerTarget.PHONE -> repo.setPhoneApp(entry?.key)
                            }
                        }
                        picker = null
                    },
                    onDismiss = { picker = null },
                )
            }

            when (textDialog) {
                TextTarget.HOME_ADDRESS -> TextInputDialog(
                    title = "Indirizzo di casa",
                    initial = s.homeAddress,
                    placeholder = "Via, numero civico, città",
                    confirmLabel = "Salva",
                    onConfirm = { value ->
                        scope.launch { repo.setHomeAddress(value) }
                        textDialog = null
                    },
                    onDismiss = { textDialog = null },
                )

                TextTarget.DESTINATION -> TextInputDialog(
                    title = "Destinazione",
                    initial = "",
                    placeholder = "Indirizzo o nome del luogo",
                    confirmLabel = "Naviga",
                    onConfirm = { value ->
                        textDialog = null
                        navigateTo(value)
                    },
                    onDismiss = { textDialog = null },
                )

                null -> Unit
            }
        }
    }
}

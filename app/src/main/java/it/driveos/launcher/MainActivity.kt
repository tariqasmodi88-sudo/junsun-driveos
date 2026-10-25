package it.driveos.launcher

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import it.driveos.launcher.apps.AppKey
import it.driveos.launcher.calendar.AgendaFormatter
import it.driveos.launcher.dashboard.CardType
import it.driveos.launcher.media.MediaSources
import it.driveos.launcher.nav.NavGuidanceStore
import it.driveos.launcher.system.LaunchResult
import it.driveos.launcher.system.Launcher
import it.driveos.launcher.ui.AppDrawerScreen
import it.driveos.launcher.ui.AppPickerDialog
import it.driveos.launcher.ui.BottomBar
import it.driveos.launcher.ui.DashboardScreen
import it.driveos.launcher.ui.DeviceInfoScreen
import it.driveos.launcher.ui.SettingsScreen
import it.driveos.launcher.ui.TextInputDialog
import it.driveos.launcher.ui.cards.CalendarCard
import it.driveos.launcher.ui.cards.ClockCard
import it.driveos.launcher.ui.cards.MusicCard
import it.driveos.launcher.ui.cards.NavCard
import it.driveos.launcher.ui.detail.CalendarDetailScreen
import it.driveos.launcher.ui.detail.ClockDetailScreen
import it.driveos.launcher.ui.detail.MusicDetailScreen
import it.driveos.launcher.ui.detail.NavDetailScreen
import it.driveos.launcher.ui.theme.DriveOSTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

enum class Screen { DASHBOARD, APPS, SETTINGS, DEVICE_INFO, NAV, MUSIC, CALENDAR, CLOCK }

private enum class PickerTarget(val title: String) {
    NAV("App di navigazione"),
    MUSIC("App musica"),
    PHONE("App telefono"),
}

private enum class TextTarget { HOME_ADDRESS, WORK_ADDRESS }

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

    override fun onResume() {
        super.onResume()
        // Al ritorno dalle impostazioni di Android i permessi potrebbero essere cambiati.
        driveApp.mediaRepository.start()
        driveApp.calendarRepository.refresh()
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
        val mediaAccess by driveApp.mediaRepository.access.collectAsStateWithLifecycle()
        val media by driveApp.mediaRepository.media.collectAsStateWithLifecycle()
        val calendar by driveApp.calendarRepository.state.collectAsStateWithLifecycle()
        val guidanceMap by NavGuidanceStore.guidance.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        var picker by remember { mutableStateOf<PickerTarget?>(null) }
        var textDialog by remember { mutableStateOf<TextTarget?>(null) }
        var navQuery by rememberSaveable { mutableStateOf("") }
        val context: Context = this
        val pm = context.packageManager

        fun message(text: String) {
            scope.launch { snackbar.showSnackbar(text) }
        }

        val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            driveApp.calendarRepository.refresh()
            if (!granted) message("Permesso negato. Puoi concederlo in Impostazioni Android → App → DriveOS → Autorizzazioni.")
        }
        val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!text.isNullOrBlank()) navQuery = text
        }
        val voiceAvailable = remember {
            pm.queryIntentActivities(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0).isNotEmpty()
        }
        val declaredAudio = remember(apps) {
            (pm.queryIntentServices(Intent("android.media.browse.MediaBrowserService"), 0).map { it.serviceInfo.packageName } +
                pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC), 0)
                    .map { it.activityInfo.packageName }).toSet()
        }

        val s = settingsState
        if (s == null) {
            // Preferenze non ancora lette (pochi millisecondi): schermo nero, nessun valore inventato.
            Box(Modifier.fillMaxSize().background(Color.Black))
            return
        }

        LaunchedEffect(s.navApp) { NavGuidanceStore.setPreferredPackage(s.navApp?.packageName) }

        val repo = driveApp.settingsRepository
        val mediaRepo = driveApp.mediaRepository
        val navEntry = apps.firstOrNull { it.key == s.navApp }
        val musicEntry = apps.firstOrNull { it.key == s.musicApp }
        val sourceEntry = media?.let { m -> apps.firstOrNull { it.key.packageName == m.packageName } }
        val guidance = s.navApp?.packageName?.let { guidanceMap[it] } ?: guidanceMap.values.firstOrNull()
        val audioSources = remember(apps, declaredAudio, s.musicApp) {
            apps.filter { MediaSources.isLikelyAudio(it.label, it.key.packageName, declaredAudio, s.musicApp?.packageName) }
        }

        fun report(result: LaunchResult) {
            if (result is LaunchResult.Error) message(result.message)
        }

        fun openOrPick(key: AppKey?, target: PickerTarget) {
            if (key == null) picker = target else report(Launcher.launchApp(context, key))
        }

        fun navigateTo(destination: String) {
            val nav = s.navApp
            if (nav == null) {
                picker = PickerTarget.NAV
                return
            }
            val result = Launcher.navigate(context, nav.packageName, destination)
            if (result is LaunchResult.Ok) {
                scope.launch { repo.addRecentDestination(destination) }
                navQuery = ""
            }
            report(result)
        }

        fun goHome() {
            if (s.homeAddress.isBlank()) textDialog = TextTarget.HOME_ADDRESS else navigateTo(s.homeAddress)
        }

        fun goWork() {
            if (s.workAddress.isBlank()) textDialog = TextTarget.WORK_ADDRESS else navigateTo(s.workAddress)
        }

        fun openNotificationAccess() {
            report(Launcher.openSystemSettings(context, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            message("Attiva DriveOS nell'elenco, poi torna indietro.")
        }

        fun connectCalendar() {
            if (driveApp.calendarRepository.hasPermission()) driveApp.calendarRepository.refresh()
            else calendarPermission.launch(Manifest.permission.READ_CALENDAR)
        }

        fun startVoice() {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Dove vuoi andare?")
            try {
                voiceLauncher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                message("Riconoscimento vocale non disponibile.")
            }
        }

        fun openSource() {
            if (sourceEntry != null) report(Launcher.launchApp(context, sourceEntry.key))
            else openOrPick(s.musicApp, PickerTarget.MUSIC)
        }

        // Indietro: dalle viste a tutto schermo si torna alla Home; sulla Home non fa nulla.
        BackHandler(enabled = true) {
            screen = when (screen) {
                Screen.DEVICE_INFO -> Screen.SETTINGS
                else -> Screen.DASHBOARD
            }
        }

        DriveOSTheme(s.themeMode, s.accentIndex) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (screen) {
                            Screen.DASHBOARD -> DashboardScreen(s.layout) { type ->
                                when (type) {
                                    CardType.CLOCK -> ClockCard(onClick = { screen = Screen.CLOCK })
                                    CardType.NAV -> NavCard(
                                        navEntry = navEntry,
                                        configured = s.navApp != null,
                                        guidance = guidance,
                                        recent = s.recentDestinations,
                                        onOpenDetail = { screen = Screen.NAV },
                                        onChooseApp = { picker = PickerTarget.NAV },
                                        onResume = { openOrPick(s.navApp, PickerTarget.NAV) },
                                        onHome = { goHome() },
                                        onWork = { goWork() },
                                        onDestination = { navigateTo(it) },
                                    )
                                    CardType.MUSIC -> MusicCard(
                                        access = mediaAccess,
                                        media = media,
                                        sourceEntry = sourceEntry,
                                        musicApp = musicEntry,
                                        onOpenDetail = { screen = Screen.MUSIC },
                                        onGrantAccess = { openNotificationAccess() },
                                        onOpenMusicApp = { openOrPick(s.musicApp, PickerTarget.MUSIC) },
                                        onPlayPause = { mediaRepo.playPause() },
                                        onNext = { mediaRepo.next() },
                                        onPrevious = { mediaRepo.previous() },
                                    )
                                    CardType.CALENDAR -> CalendarCard(
                                        state = calendar,
                                        onOpenDetail = { screen = Screen.CALENDAR },
                                        onConnect = { connectCalendar() },
                                    )
                                }
                            }

                            Screen.NAV -> NavDetailScreen(
                                navEntry = navEntry,
                                guidance = guidance,
                                homeAddress = s.homeAddress,
                                workAddress = s.workAddress,
                                recent = s.recentDestinations,
                                query = navQuery,
                                onQueryChange = { navQuery = it },
                                voiceAvailable = voiceAvailable,
                                onVoice = { startVoice() },
                                onNavigate = { navigateTo(it) },
                                onHome = { goHome() },
                                onWork = { goWork() },
                                onEditHome = { textDialog = TextTarget.HOME_ADDRESS },
                                onEditWork = { textDialog = TextTarget.WORK_ADDRESS },
                                onRemoveRecent = { dest -> scope.launch { repo.removeRecentDestination(dest) } },
                                onOpenApp = { openOrPick(s.navApp, PickerTarget.NAV) },
                                onChooseApp = { picker = PickerTarget.NAV },
                                onBack = { screen = Screen.DASHBOARD },
                            )

                            Screen.MUSIC -> MusicDetailScreen(
                                access = mediaAccess,
                                media = media,
                                sourceEntry = sourceEntry,
                                musicApp = musicEntry,
                                sources = audioSources,
                                onGrantAccess = { openNotificationAccess() },
                                onPlayPause = { mediaRepo.playPause() },
                                onNext = { mediaRepo.next() },
                                onPrevious = { mediaRepo.previous() },
                                onOpenSource = { openSource() },
                                onLaunch = { report(Launcher.launchApp(context, it)) },
                                onBack = { screen = Screen.DASHBOARD },
                            )

                            Screen.CALENDAR -> CalendarDetailScreen(
                                state = calendar,
                                onConnect = { connectCalendar() },
                                onOpenCalendarApp = { report(Launcher.openCalendar(context)) },
                                onOpenEvent = { e -> report(Launcher.openCalendarEvent(context, e.eventId, e.begin, e.end)) },
                                onNavigate = { navigateTo(it) },
                                onBack = { screen = Screen.DASHBOARD },
                            )

                            Screen.CLOCK -> ClockDetailScreen(
                                nextEvent = AgendaFormatter.upcoming(calendar.events, Instant.now(), ZoneId.systemDefault()).firstOrNull(),
                                onBack = { screen = Screen.DASHBOARD },
                            )

                            Screen.APPS -> AppDrawerScreen(
                                apps = apps,
                                loaded = loaded,
                                favorites = s.favorites,
                                onLaunch = { report(Launcher.launchApp(context, it)) },
                                onToggleFavorite = { key -> scope.launch { repo.toggleFavorite(key) } },
                                onBack = { screen = Screen.DASHBOARD },
                            )

                            Screen.SETTINGS -> SettingsScreen(
                                settings = s,
                                appLabel = { key -> apps.firstOrNull { it.key == key }?.label },
                                onPickNav = { picker = PickerTarget.NAV },
                                onEditHome = { textDialog = TextTarget.HOME_ADDRESS },
                                onEditWork = { textDialog = TextTarget.WORK_ADDRESS },
                                onLayout = { layout -> scope.launch { repo.setLayout(layout) } },
                                notificationAccess = mediaAccess,
                                onNotificationAccess = { openNotificationAccess() },
                                calendarAccess = calendar.permission,
                                onCalendarAccess = {
                                    if (calendar.permission) report(Launcher.openAppDetails(context)) else connectCalendar()
                                },
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

                TextTarget.WORK_ADDRESS -> TextInputDialog(
                    title = "Indirizzo di lavoro",
                    initial = s.workAddress,
                    placeholder = "Via, numero civico, città",
                    confirmLabel = "Salva",
                    onConfirm = { value ->
                        scope.launch { repo.setWorkAddress(value) }
                        textDialog = null
                    },
                    onDismiss = { textDialog = null },
                )

                null -> Unit
            }
        }
    }
}

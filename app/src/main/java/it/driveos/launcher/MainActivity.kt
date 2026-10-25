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
import it.driveos.launcher.ui.ControlsPanel
import it.driveos.launcher.ui.DashboardScreen
import it.driveos.launcher.ui.DeviceInfoScreen
import it.driveos.launcher.ui.QuickBar
import it.driveos.launcher.ui.RadioPresetDialog
import it.driveos.launcher.ui.RadioScreen
import it.driveos.launcher.ui.SettingsScreen
import it.driveos.launcher.ui.TextInputDialog
import it.driveos.launcher.ui.VehicleScreen
import it.driveos.launcher.ui.theme.AutoThemeState
import it.driveos.launcher.radio.RadioBand
import it.driveos.launcher.radio.RadioPreset
import it.driveos.launcher.radio.RadioPresetLogic
import it.driveos.launcher.settings.ThemeMode
import it.driveos.launcher.vehicle.ActionDiscovery
import it.driveos.launcher.vehicle.CandidateFinder
import it.driveos.launcher.vehicle.LightsResolver
import it.driveos.launcher.vehicle.SignalBinding
import it.driveos.launcher.vehicle.SignalIds
import it.driveos.launcher.vehicle.SignalRole
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
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

enum class Screen { DASHBOARD, APPS, SETTINGS, DEVICE_INFO, NAV, MUSIC, CALENDAR, CLOCK, VEHICLE, RADIO }

private enum class PickerTarget(val title: String) {
    NAV("App di navigazione"),
    MUSIC("App musica"),
    PHONE("App telefono"),
    RADIO("App radio"),
}

private enum class TextTarget { HOME_ADDRESS, WORK_ADDRESS, LOCATION }

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
        driveApp.vehicleRepository.start()
        driveApp.systemControls.refresh()
    }

    override fun onPause() {
        super.onPause()
        driveApp.vehicleRepository.stop()
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
        val vehicleSignals by driveApp.vehicleRepository.signals.collectAsStateWithLifecycle()
        val vehicleChanges by driveApp.vehicleRepository.changeCounts.collectAsStateWithLifecycle()
        val volumeState by driveApp.systemControls.volume.collectAsStateWithLifecycle()
        val windowBrightness by driveApp.systemControls.windowBrightness.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        var picker by remember { mutableStateOf<PickerTarget?>(null) }
        var textDialog by remember { mutableStateOf<TextTarget?>(null) }
        var navQuery by rememberSaveable { mutableStateOf("") }
        var controlsOpen by remember { mutableStateOf(false) }
        var radioDialog by remember { mutableStateOf(false) }
        // Calibrazione fari: istantanee prese con fari accesi / spenti.
        var onSnaps by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
        var offSnaps by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
        var scanning by remember { mutableStateOf(false) }
        var scanResult by remember { mutableStateOf<String?>(null) }
        val context: Context = this
        val pm = context.packageManager

        // Aggiorna i segnali del veicolo a intervalli regolari mentre la Home è in primo piano.
        LaunchedEffect(Unit) {
            while (true) {
                driveApp.vehicleRepository.pollOnce()
                driveApp.systemControls.refresh()
                kotlinx.coroutines.delay(5000)
            }
        }

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

        // Mette in ascolto le azioni broadcast scoperte (oltre a quelle di sistema).
        LaunchedEffect(s.discoveredActions, s.lightsBinding?.id) {
            val fromBindings = s.signalBindings.mapNotNull { SignalIds.actionOf(it.id) }
            driveApp.vehicleRepository.updateListenedActions((s.discoveredActions + fromBindings).toSet())
        }

        // Stato dei fari e tema automatico.
        val lightsResolved = s.lightsBinding?.let { LightsResolver.resolve(it, vehicleSignals[it.id]) }
        var autoTick by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) { kotlinx.coroutines.delay(60_000); autoTick++ }
        }
        val autoResult = remember(s.themeMode, s.lightsBinding?.id, vehicleSignals, s.latitude, s.longitude, autoTick) {
            if (s.themeMode == ThemeMode.AUTO) AutoThemeState.compute(context, s, vehicleSignals) else null
        }
        val autoDark = autoResult?.dark

        // Applica la luminosità scelta alla finestra di DriveOS (nessun permesso).
        LaunchedEffect(windowBrightness) {
            window.attributes = window.attributes.apply {
                screenBrightness = if (windowBrightness < 0f)
                    android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else windowBrightness
            }
        }

        fun recordSnapshot(on: Boolean) {
            val snap = driveApp.vehicleRepository.snapshot()
            if (on) onSnaps = onSnaps + snap else offSnaps = offSnaps + snap
        }
        val candidates = remember(onSnaps, offSnaps, vehicleChanges) {
            CandidateFinder.find(onSnaps, offSnaps, vehicleChanges)
        }

        fun scanVehicleApps() {
            scanning = true
            scanResult = null
            scope.launch {
                val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching { ActionDiscovery.discover(context) }.getOrNull()
                }
                scanning = false
                if (result == null) { scanResult = "Scansione non riuscita."; return@launch }
                driveApp.settingsRepository.setDiscoveredActions(result.actions)
                scanResult = "Trovate ${result.actions.size} azioni in ${result.scannedApps.size} app. " +
                    "Ora muovi il veicolo: i valori compariranno nell'elenco."
            }
        }

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

        // Indietro: chiude i controlli; dalle viste a tutto schermo torna alla Home.
        BackHandler(enabled = true) {
            if (controlsOpen) { controlsOpen = false } else screen = when (screen) {
                Screen.DEVICE_INFO -> Screen.SETTINGS
                Screen.VEHICLE, Screen.RADIO -> Screen.SETTINGS
                else -> Screen.DASHBOARD
            }
        }

        DriveOSTheme(s.themeMode, s.accentIndex, autoDark) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    if (controlsOpen) {
                        ControlsPanel(
                            volume = volumeState,
                            brightness = windowBrightness,
                            canSystemBrightness = driveApp.systemControls.canWriteSystemBrightness(),
                            onVolume = { driveApp.systemControls.setVolumePercent(it) },
                            onVolumeStep = { driveApp.systemControls.nudgeVolume(it) },
                            onBrightness = { frac ->
                                driveApp.systemControls.setWindowBrightness(frac)
                                driveApp.systemControls.setSystemBrightness(frac)
                            },
                            onOpenSoundSettings = {
                                report(Launcher.openSystemSettings(context, Settings.ACTION_SOUND_SETTINGS))
                            },
                            onClose = { controlsOpen = false },
                        )
                    }
                    if (screen == Screen.DASHBOARD) {
                        QuickBar(
                            volumePercent = volumeState.percent,
                            autoThemeLabel = autoResult?.source?.label,
                            onControls = { controlsOpen = !controlsOpen },
                            onVehicle = { screen = Screen.VEHICLE },
                            onRadio = { screen = Screen.RADIO },
                        )
                    }
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
                                onPickRadio = { picker = PickerTarget.RADIO },
                                onThemeMode = { mode -> scope.launch { repo.setThemeMode(mode) } },
                                onAccent = { index -> scope.launch { repo.setAccent(index) } },
                                autoThemeSource = autoResult?.source?.label,
                                onOpenVehicle = { screen = Screen.VEHICLE },
                                onOpenRadio = { screen = Screen.RADIO },
                                onEditLocation = { textDialog = TextTarget.LOCATION },
                                onHomeSettings = { report(Launcher.openSystemSettings(context, Settings.ACTION_HOME_SETTINGS)) },
                                onAndroidSettings = { report(Launcher.openSystemSettings(context, Settings.ACTION_SETTINGS)) },
                                onDeviceInfo = { screen = Screen.DEVICE_INFO },
                                onBack = { screen = Screen.DASHBOARD },
                            )

                            Screen.DEVICE_INFO -> DeviceInfoScreen(
                                extraSections = {
                                    org.json.JSONObject().apply {
                                        put("segnali", org.json.JSONObject().also { o ->
                                            driveApp.vehicleRepository.signals.value.forEach { (k, v) -> o.put(k, v) }
                                        })
                                        put("azioniScoperte", org.json.JSONArray(s.discoveredActions))
                                        put("collegamenti", org.json.JSONArray(s.signalBindings.map { "${it.role}:${it.id}" }))
                                    }
                                },
                                onBack = { screen = Screen.SETTINGS },
                            )

                            Screen.VEHICLE -> VehicleScreen(
                                settings = s,
                                signals = vehicleSignals,
                                scanning = scanning,
                                scanResult = scanResult,
                                lightsResolved = lightsResolved,
                                candidates = candidates,
                                recordedOn = onSnaps.size,
                                recordedOff = offSnaps.size,
                                onScanApps = { scanVehicleApps() },
                                onRecordOn = { recordSnapshot(true) },
                                onRecordOff = { recordSnapshot(false) },
                                onAssignLights = { c ->
                                    scope.launch {
                                        driveApp.settingsRepository.upsertBinding(
                                            SignalBinding(c.id, SignalRole.LIGHTS, "Fari", c.onValue, c.offValue)
                                        )
                                        if (s.themeMode != ThemeMode.AUTO) driveApp.settingsRepository.setThemeMode(ThemeMode.AUTO)
                                    }
                                    message("Fari collegati. Tema automatico attivo.")
                                },
                                onAddGauge = { id ->
                                    scope.launch {
                                        driveApp.settingsRepository.upsertBinding(
                                            SignalBinding(id, SignalRole.GAUGE, SignalIds.describe(id).take(28))
                                        )
                                    }
                                },
                                onRemoveBinding = { b -> scope.launch { driveApp.settingsRepository.removeBinding(b.id, b.role) } },
                                onBack = { screen = Screen.SETTINGS },
                            )

                            Screen.RADIO -> RadioScreen(
                                presets = s.radioPresets,
                                radioConfigured = s.radioApp != null,
                                canTune = false,
                                onPlay = { openOrPick(s.radioApp, PickerTarget.RADIO) },
                                onAdd = { radioDialog = true },
                                onRemove = { p -> scope.launch {
                                    driveApp.settingsRepository.setRadioPresets(RadioPresetLogic.remove(s.radioPresets, p.band, p.khz))
                                } },
                                onOpenRadioApp = { openOrPick(s.radioApp, PickerTarget.RADIO) },
                                onChooseRadioApp = { picker = PickerTarget.RADIO },
                                onBack = { screen = Screen.SETTINGS },
                            )
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
                        PickerTarget.RADIO -> s.radioApp
                    },
                    onPick = { entry ->
                        scope.launch {
                            when (target) {
                                PickerTarget.NAV -> repo.setNavApp(entry?.key)
                                PickerTarget.MUSIC -> repo.setMusicApp(entry?.key)
                                PickerTarget.PHONE -> repo.setPhoneApp(entry?.key)
                                PickerTarget.RADIO -> repo.setRadioApp(entry?.key)
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

                TextTarget.LOCATION -> TextInputDialog(
                    title = "Posizione (latitudine, longitudine)",
                    initial = if (s.latitude != 0.0 || s.longitude != 0.0) "${s.latitude}, ${s.longitude}" else "",
                    placeholder = "es. 45.07, 7.69",
                    confirmLabel = "Salva",
                    onConfirm = { value ->
                        val parts = value.split(",", ";").mapNotNull { it.trim().toDoubleOrNull() }
                        if (parts.size == 2 && parts[0] in -90.0..90.0 && parts[1] in -180.0..180.0) {
                            scope.launch { repo.setLocation(parts[0], parts[1]) }
                            textDialog = null
                        } else {
                            message("Formato: latitudine, longitudine (es. 45.07, 7.69)")
                        }
                    },
                    onDismiss = { textDialog = null },
                )

                null -> Unit
            }

            if (radioDialog) {
                RadioPresetDialog(
                    onConfirm = { frequency, name ->
                        val khz = RadioPresetLogic.parseFm(frequency)
                        if (khz == null) {
                            message("Frequenza non valida (87.5 – 108.0 MHz).")
                        } else {
                            val label = name.ifBlank { "FM ${RadioPreset(khz, "").displayFrequency()}" }
                            scope.launch {
                                driveApp.settingsRepository.setRadioPresets(
                                    RadioPresetLogic.add(s.radioPresets, RadioPreset(khz, label, RadioBand.FM))
                                )
                            }
                            radioDialog = false
                        }
                    },
                    onDismiss = { radioDialog = false },
                )
            }
        }
    }
}

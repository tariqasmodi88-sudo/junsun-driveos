package it.driveos.launcher

import it.driveos.launcher.i18n.tr
import it.driveos.launcher.i18n.trf

import kotlinx.coroutines.flow.first
import android.Manifest
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import it.driveos.launcher.voice.VoiceCommand
import it.driveos.launcher.voice.VoiceCommands
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
import androidx.compose.runtime.mutableStateMapOf
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
import it.driveos.launcher.automation.ConnectionTrigger
import it.driveos.launcher.automation.SmartAction
import it.driveos.launcher.automation.SmartStart
import it.driveos.launcher.automation.SmartStartConfig
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
import it.driveos.launcher.ui.RadioPresetDialog
import it.driveos.launcher.ui.RadioScreen
import it.driveos.launcher.ui.SettingsScreen
import it.driveos.launcher.ui.SmartStartScreen
import it.driveos.launcher.ui.TextInputDialog
import it.driveos.launcher.ui.VehicleScreen
import it.driveos.launcher.ui.VehicleStatusStrip
import it.driveos.launcher.ui.VehicleOverviewScreen
import it.driveos.launcher.ui.VehicleOverviewState
import it.driveos.launcher.vehicle.CanDecoder
import it.driveos.launcher.vehicle.KeyAction
import it.driveos.launcher.vehicle.KeyBindingCodec
import it.driveos.launcher.vehicle.KeyEvents
import it.driveos.launcher.ui.KeysScreen
import it.driveos.launcher.ui.theme.AutoThemeState
import it.driveos.launcher.radio.RadioBand
import it.driveos.launcher.radio.RadioPreset
import it.driveos.launcher.radio.RadioState
import it.driveos.launcher.radio.RadioPresetLogic
import it.driveos.launcher.settings.ThemeMode
import it.driveos.launcher.vehicle.KnownVehicleSignals
import it.driveos.launcher.vehicle.LogcatParser
import it.driveos.launcher.vehicle.SignalBinding
import it.driveos.launcher.vehicle.SignalIds
import it.driveos.launcher.vehicle.SignalRole
import it.driveos.launcher.vehicle.VehicleLog
import it.driveos.launcher.vehicle.VehicleLogService
import it.driveos.launcher.vehicle.VehicleStatus
import it.driveos.launcher.vehicle.WindowRules
import it.driveos.launcher.junsun.TsBridge
import it.driveos.launcher.vehicle.NativeRadio
import it.driveos.launcher.vehicle.WindowWatcher
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

enum class Screen { DASHBOARD, APPS, SETTINGS, DEVICE_INFO, NAV, MUSIC, CALENDAR, CLOCK, VEHICLE, RADIO, SMART_START, OVERVIEW, KEYS, CLIMATE, REAR_CAM }

private enum class PickerTarget(val title: String) {
    NAV(tr("App di navigazione")),
    MUSIC(tr("App musica")),
    PHONE(tr("App telefono")),
    RADIO(tr("App radio")),
    ONLINE_MUSIC(tr("App musica online (streaming)")),
    OFFLINE_MUSIC(tr("App musica offline (locale)")),
}

private enum class TextTarget { HOME_ADDRESS, WORK_ADDRESS, LOCATION, SEARCH }

// App di proiezione telefono presenti su questi firmware: Android Auto e il dongle SpeedPlay.
private val PROJECTION_PKGS = listOf("com.google.android.projection.gearhead", "com.suding.speedplay")

/**
 * Unica activity: schermata Home (categoria HOME) e voce nell'elenco app (LAUNCHER).
 * Non tocca radio, CAN, retrocamera o impostazioni di sistema: avvia solo altre app.
 */
/** La radio nativa e' gia' stata avviata dietro DriveOS in questo avvio. */
private var nativeRadioWarmed = false

class MainActivity : ComponentActivity() {

    private var screen by mutableStateOf(Screen.DASHBOARD)
    private val driveApp: DriveOSApp get() = application as DriveOSApp
    private var receiverRegistered = false
    /** DriveOS e' davanti (tra onResume e onPause). */
    private var isForeground by mutableStateOf(false)

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
        startReverseGuard()
        setContent { DriveOSRoot() }
    }

    /** Limite tocchi in marcia (come Android Auto): i tocchi oltre la riserva vengono bloccati. */
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        val lock = driveApp.driveLock
        // un tocco su DriveOS dopo l'accensione: da qui in poi le app Junsun aperte sono volute
        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) it.driveos.launcher.system.WakeGuard.userActed()
        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
            if (!lock.onTouch()) return true
        } else if (lock.isLocked()) return true
        return super.dispatchTouchEvent(ev)
    }

    /**
     * Retromarcia: il firmware porta davanti la sua app (forfan.want2start = com.ts.MainUI). Questo
     * controllo vive nell'Activity (non nella UI Compose, che in secondo piano e' ferma) e riporta
     * DriveOS davanti durante e dopo la retromarcia.
     */
    @Volatile private var lastProjectionLaunch = 0L

    /**
     * Telefono collegato (vivavoce/Bluetooth): si apre AutoLink in automatico, anche se DriveOS non
     * e' davanti, poi si torna alla Home. Cosi' non serve toccare il tasto Telefono.
     */
    private fun startAutoProjection() {
        lifecycleScope.launch {
            var was = false
            driveApp.vehicleRepository.signals.collect { sig ->
                val phone = it.driveos.launcher.vehicle.KnownVehicleSignals.phoneConnected(sig) == true
                val rising = phone && !was
                was = phone
                if (!rising) return@collect
                val cfg = runCatching { driveApp.settingsRepository.settings.first() }.getOrNull() ?: return@collect
                if (!cfg.smartStartEnabled || !cfg.smartStartAndroidAuto) return@collect
                kotlinx.coroutines.delay(4000) // SpeedPlay si aggancia al telefono
                if (it.driveos.launcher.vehicle.KnownVehicleSignals.projectionActive(driveApp.vehicleRepository.signals.value)) return@collect
                if (System.currentTimeMillis() - lastProjectionLaunch < 60_000) return@collect
                val pkg = PROJECTION_PKGS.firstOrNull { packageManager.getLaunchIntentForPackage(it) != null } ?: return@collect
                lastProjectionLaunch = System.currentTimeMillis()
                runCatching { startActivity(packageManager.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                var waited = 0
                while (!it.driveos.launcher.vehicle.KnownVehicleSignals.projectionActive(driveApp.vehicleRepository.signals.value) && waited < 20_000) {
                    kotlinx.coroutines.delay(500); waited += 500
                }
                kotlinx.coroutines.delay(1500)
                bringHomeFromBackground()
            }
        }
    }

    private fun startReverseGuard() {
        startAutoProjection()
        lifecycleScope.launch {
            var lastReverse: Boolean? = null
            var lastReverseMs = 0L
            var lastFg: String? = null
            var lastWin: String? = null
            var pendingReturn = false
            driveApp.vehicleRepository.signals.collect { sig ->
                val rev = it.driveos.launcher.vehicle.KnownVehicleSignals.reverseOn(sig)
                val fg = sig["prop:forfan.want2start"]
                val keep = runCatching { driveApp.settingsRepository.settings.first().keepHome }.getOrDefault(true)
                if (rev != lastReverse) {
                    lastReverseMs = System.currentTimeMillis()
                    if (rev == false && lastReverse == true && pendingReturn) {
                        launch { kotlinx.coroutines.delay(900); bringHomeFromBackground(); pendingReturn = false }
                    }
                    lastReverse = rev
                }
                val win = sig["win:last"]
                if (win != lastWin) {
                    lastWin = win
                    val kind = it.driveos.launcher.vehicle.WindowRules.classify(null, win)
                    if (keep && (kind == it.driveos.launcher.vehicle.WindowKind.RADAR || kind == it.driveos.launcher.vehicle.WindowKind.REVERSE)) {
                        launch { kotlinx.coroutines.delay(700); bringHomeFromBackground() }
                    }
                }
                if (fg != lastFg) {
                    lastFg = fg
                    if (keep && fg == "com.ts.MainUI" && System.currentTimeMillis() - lastReverseMs < 4000) {
                        pendingReturn = true
                        launch { kotlinx.coroutines.delay(500); bringHomeFromBackground() }
                    }
                }
            }
        }
        // Finche' DriveOS e' in secondo piano la UI non rilegge i segnali: lo fa questo ciclo.
        lifecycleScope.launch {
            while (true) {
                kotlinx.coroutines.delay(2000)
                if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                    // fuori dal thread dell'interfaccia: la lettura lancia `getprop` e aspetta la risposta
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { driveApp.vehicleRepository.pollOnce() } }
                }
            }
        }
    }

    private fun bringHomeFromBackground() {
        if (it.driveos.launcher.vehicle.WindowWatcher.goHome()) return
        runCatching {
            getSystemService(android.app.ActivityManager::class.java)?.appTasks?.firstOrNull()?.moveToFront()
        }
        runCatching {
            startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
    }

    override fun onResume() {
        super.onResume()
        isForeground = true
        it.driveos.launcher.system.WakeGuard.homeInFront = true
        // Se la radio si e' risvegliata senza avvisare (nessun segnale di riaccensione), la Home visibile
        // vale come risveglio: DriveOS si ricollega comunque 20 s dopo.
        if (!it.driveos.launcher.system.PowerGate.screenOn &&
            (getSystemService(android.os.PowerManager::class.java)?.isInteractive != false)) {
            sendBroadcast(Intent("com.ts.main.uiaccon").setPackage(packageName))
        }
        // Al ritorno dalle impostazioni di Android i permessi potrebbero essere cambiati.
        driveApp.mediaRepository.start()
        driveApp.calendarRepository.refresh()
        driveApp.vehicleRepository.start()
        driveApp.systemControls.refresh()
        driveApp.bluetoothWatcher.start()
        driveApp.connectivityWatcher.start()
        driveApp.logcatReader.start() // legge portiere/sensori/radio dal log se READ_LOGS è concesso
        driveApp.speedWatcher.start()  // velocità GPS per la schermata 'auto ferma' (se c'è il permesso)
    }

    override fun onPause() {
        super.onPause()
        isForeground = false
        it.driveos.launcher.system.WakeGuard.homeInFront = false
        // vehicleRepository resta attivo anche in secondo piano: serve a riprendere lo schermo dopo la retromarcia
        driveApp.bluetoothWatcher.stop()
        driveApp.speedWatcher.stop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Tasto Home premuto mentre DriveOS e' gia' aperto: torna alla dashboard.
        // Ma non se il "Home" l'ha mandato DriveOS stesso per tornare davanti (es. in retromarcia):
        // altrimenti la schermata retro veniva subito sostituita dalla dashboard/Veicolo (07/11).
        if (intent.hasCategory(Intent.CATEGORY_HOME) && System.currentTimeMillis() > WindowWatcher.selfHomeUntil &&
            KnownVehicleSignals.reverseOn(driveApp.vehicleRepository.signals.value) != true) screen = Screen.DASHBOARD
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
        var bridgeKhz by remember { mutableStateOf<Int?>(null) }
        var bridgeRds by remember { mutableStateOf<Boolean?>(null) }
        // nomi RDS ricevuti, per frequenza: restano visibili anche se il log non li ripete
        val stationNames by driveApp.logcatReader.names.collectAsStateWithLifecycle()
        val rdsState by driveApp.logcatReader.rds.collectAsStateWithLifecycle()
        // Clima: quando cambi un comando dell'auto si apre la schermata Clima (come fa la radio nativa)
        // e dopo qualche secondo senza modifiche si torna dove eri.
        var climateAuto by remember { mutableStateOf(false) }
        var climateBack by remember { mutableStateOf<Screen?>(null) }
        val climateSig = listOf(CanDecoder.AC_POWER, CanDecoder.AC_COMP, CanDecoder.AC_RECIRC, CanDecoder.AC_AUTO, CanDecoder.AC_DUAL,
            CanDecoder.AC_MAXDEF, CanDecoder.AC_REARDEF, CanDecoder.AC_AIR_UP, CanDecoder.AC_AIR_MID, CanDecoder.AC_AIR_DOWN,
            CanDecoder.AC_FAN, CanDecoder.AC_TEMP_L, CanDecoder.AC_TEMP_R, CanDecoder.SEAT_HEAT_L, CanDecoder.SEAT_HEAT_R)
            .joinToString(",") { vehicleSignals[it] ?: "" }
        val climateSeen = remember { arrayOfNulls<String>(1) }
        LaunchedEffect(climateSig) {
            val prev = climateSeen[0]
            climateSeen[0] = climateSig
            if (prev == null || prev.all { it == ',' } || prev == climateSig) return@LaunchedEffect
            if (screen != Screen.CLIMATE && screen != Screen.OVERVIEW) { climateBack = screen; climateAuto = true; screen = Screen.CLIMATE }
            if (climateAuto) {
                kotlinx.coroutines.delay(6000)
                if (screen == Screen.CLIMATE && climateAuto) { climateAuto = false; screen = climateBack ?: Screen.DASHBOARD }
            }
        }
        val logName = vehicleSignals[LogcatParser.RADIO_NAME]
        val logKhz = vehicleSignals[LogcatParser.RADIO_FREQ]?.toIntOrNull()
        LaunchedEffect(screen) {
            if (screen == Screen.MUSIC) {
                // passando a Musica: spegni la radio e riprendi i brani (se erano stati fermati per la radio)
                if (driveApp.tsBridge.workMode() == 1) driveApp.tsBridge.radioNow { it.CloseRadioCh() }
                if (!driveApp.mediaRepository.resumeIfPausedForRadio()) driveApp.mediaRepository.resumeLocalIfIdle()
            }
            if (screen != Screen.RADIO) return@LaunchedEffect
            driveApp.logcatReader.resetLive()
            // entrando in Radio: ferma i file/lo streaming e accendi il sintonizzatore
            driveApp.mediaRepository.pauseIfPlaying()
            val br = driveApp.tsBridge
            // Accensione pulita: la radio riparte dall'ultima stazione che ricorda LEI, che puo' non essere
            // quella scelta in DriveOS. Si sintonizza prima, con l'audio muto, e si apre il canale solo quando
            // la frequenza e' giusta: niente "lampo" di un'altra stazione.
            val last = driveApp.settingsRepository.settings.first().lastRadioKhz
            var savedKhz = last
            fun cur(i: Pair<Int, Int>?) = i?.takeIf { it.second < 3 }?.let { TsBridge.toKhz(it.first) }
            val alreadyOn = br.workMode() == 1 && (last <= 0 || cur(br.radioInfo()) == last)
            if (!alreadyOn) {
                val wasMuted = br.isMuted() ?: true
                if (!wasMuted) br.setMuted(true)
                try {
                    if (last > 0) br.radioNow { it.TurnBandAndFq(0, TsBridge.fromKhz(last, br.rawSample)) }
                    else br.radioNow { it.onRadioFM() }
                    kotlinx.coroutines.delay(150)
                    br.radioNow { it.OpenRadioCh() }
                    if (last > 0) {
                        for (i in 0 until 10) {
                            kotlinx.coroutines.delay(150)
                            val now = cur(br.radioInfo())
                            if (now == last) break
                            if (i == 2 || i == 6) br.radioNow { it.TurnBandAndFq(0, TsBridge.fromKhz(last, br.rawSample)) }
                        }
                    }
                    kotlinx.coroutines.delay(150)
                } finally {
                    if (!wasMuted) br.setMuted(false)
                }
            }
            while (true) {
                val info = driveApp.tsBridge.radioInfo()
                bridgeKhz = if (info != null && info.second < 3) TsBridge.toKhz(info.first) else null
                bridgeRds = driveApp.tsBridge.rdsOn()
                bridgeKhz?.let { driveApp.logcatReader.onTuned(it) }
                // nome RDS dalla radio Junsun (anche con DriveOS davanti)
                withContext(kotlinx.coroutines.Dispatchers.IO) { driveApp.tsBridge.psName() }?.let { driveApp.logcatReader.feedPs(it) }
                bridgeKhz?.let { k -> if (k != savedKhz) { savedKhz = k; driveApp.settingsRepository.setLastRadioKhz(k) } }
                kotlinx.coroutines.delay(1000)
            }
        }
        val volumeState by driveApp.systemControls.volume.collectAsStateWithLifecycle()
        val windowBrightness by driveApp.systemControls.windowBrightness.collectAsStateWithLifecycle()
        val btConnected by driveApp.bluetoothWatcher.connected.collectAsStateWithLifecycle()
        val online by driveApp.connectivityWatcher.online.collectAsStateWithLifecycle()
        val radioState by driveApp.radioController.state.collectAsStateWithLifecycle()
        val radioDetected by driveApp.radioController.detected.collectAsStateWithLifecycle()
        val loggingOn by VehicleLog.running.collectAsStateWithLifecycle()
        val loggedChanges by VehicleLog.changes.collectAsStateWithLifecycle()
        val speedKmh by driveApp.speedWatcher.speedKmh.collectAsStateWithLifecycle()
        // velocita' per il limite tocchi: la piu' alta tra GPS e centralina dell'auto
        val canKmh = vehicleSignals[CanDecoder.SPEED]?.toDoubleOrNull()
        val lockSpeed = listOfNotNull(speedKmh, canKmh).maxOrNull()
        LaunchedEffect(lockSpeed, settingsState?.touchLimit) {
            val lock = driveApp.driveLock
            lock.enabled = settingsState?.touchLimit != false
            when {
                lockSpeed == null || lockSpeed < 3.0 -> lock.onStopped()
                lockSpeed >= it.driveos.launcher.system.DriveLock.MOVING_KMH -> lock.moving = true
            }
        }
        val parked by driveApp.speedWatcher.parked.collectAsStateWithLifecycle()
        var debugFromOverview by remember { mutableStateOf(false) }
        var overviewAuto by remember { mutableStateOf(false) } // aperta in automatico (si richiude da sola)
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        var picker by remember { mutableStateOf<PickerTarget?>(null) }
        var textDialog by remember { mutableStateOf<TextTarget?>(null) }
        var navQuery by rememberSaveable { mutableStateOf("") }
        var controlsOpen by remember { mutableStateOf(false) }
        var radioDialog by remember { mutableStateOf(false) }
        // Calibrazione fari: istantanee prese con fari accesi / spenti.
        var vehicleExportStatus by remember { mutableStateOf<String?>(null) }
        val smartTrigger = remember { ConnectionTrigger() }
        val context: Context = this
        val pm = context.packageManager

        // Aggiorna i segnali del veicolo a intervalli regolari mentre la Home è in primo piano.
        LaunchedEffect(Unit) {
            while (true) {
                // La lettura lancia `getprop` e ne aspetta la risposta (decine di millisecondi): fatta sul
                // thread dell'interfaccia bloccava lo schermo per un attimo ogni 2,5 s (animazioni a scatti).
                withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { driveApp.vehicleRepository.pollOnce() } }
                driveApp.systemControls.refresh()
                kotlinx.coroutines.delay(2500)
            }
        }

        fun message(text: String) {
            scope.launch { snackbar.showSnackbar(text) }
        }

        val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) driveApp.speedWatcher.start()
            else message(tr("Senza posizione la schermata veicolo si apre solo quando apri una portiera."))
        }
        val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        var showFolders by remember { mutableStateOf(false) }
        var pendingFolders by remember { mutableStateOf(false) }
        if (showFolders) it.driveos.launcher.ui.detail.FileBrowserDialog(
            start = driveApp.mediaRepository.lastFolder()?.takeIf { it.startsWith("/") },
            onPlay = { files, i, folder -> showFolders = false; driveApp.mediaRepository.playFiles(files, i, folder) },
            onDismiss = { showFolders = false },
        )
        val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && pendingFolders) { pendingFolders = false; showFolders = true }
            else if (granted) driveApp.mediaRepository.let { mr -> if (mr.lastFolder() != null) mr.playLastFolder() else mr.playFolder("") }
            else message(tr("Senza accesso ai file audio la musica del dispositivo non parte."))
        }
        val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            driveApp.calendarRepository.refresh()
            if (!granted) message(tr("Permesso negato. Puoi concederlo in Impostazioni Android → App → DriveOS → Autorizzazioni."))
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

        // Radio integrata: ascolta lo stato dai broadcast (azioni scoperte che sembrano "radio/fm").
        LaunchedEffect(s.discoveredActions) {
            val radioActions = s.discoveredActions.filter {
                val l = it.lowercase(); "radio" in l || ".fm" in l || "fm_" in l || "tuner" in l
            }.toSet()
            driveApp.radioController.start(radioActions)
        }

        // Registrazione continua: avvia/ferma il servizio in base all'impostazione.
        LaunchedEffect(s.canSerial) {
            if (s.canSerial) driveApp.canReader.start() else driveApp.canReader.stop()
        }
        LaunchedEffect(s.vehicleLogging, s.discoveredActions) {
            // registrazione continua (debug) dismessa: si ferma se era rimasta accesa
            VehicleLogService.stop(context)
        }

        val projectionPkg = PROJECTION_PKGS.firstOrNull { pm.getLaunchIntentForPackage(it) != null }
        // Telefono collegato: Bluetooth Android OPPURE vivavoce della radio (stack TS, hfpStatus).
        // Nel registro reale il collegamento del telefono arriva dal vivavoce TS.
        val phoneLinked = btConnected || KnownVehicleSignals.phoneConnected(vehicleSignals) == true
        val projectionOn = KnownVehicleSignals.projectionActive(vehicleSignals)

        // Lettore musica locale della radio Junsun (USB/SD): non ha sessione media, si apre l'attività.
        val junsunLocalPlayer = true // lettore interno di DriveOS (LocalMusicService)
        fun hasLocalMusic() = true // c'e' sempre il lettore interno di DriveOS
        // Riporta DriveOS davanti (CarPlay/Android Auto o il lettore restano attivi in secondo piano).
        fun bringHomeToFront() {
            if (WindowWatcher.goHome()) return
            runCatching {
                context.getSystemService(android.app.ActivityManager::class.java)
                    ?.appTasks?.firstOrNull()?.moveToFront()
            }
            runCatching {
                context.startActivity(Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }
        }
        var projectionBounceUntil by remember { mutableStateOf(0L) }
        val projectionLive by androidx.compose.runtime.rememberUpdatedState(projectionOn)
        fun playLocalMusic() {
            // sempre il lettore interno di DriveOS (non piu' l'app Files o altre)
            // Lettore interno di DriveOS: tutti i brani audio del dispositivo, mai video.
            if (!it.driveos.launcher.media.LocalMusicService.hasPermission(context)) {
                storagePermission.launch(it.driveos.launcher.media.LocalMusicService.permission())
                return
            }
            // lettore interno di DriveOS: riprende l'ultima cartella, altrimenti tutti i brani
            val mr = driveApp.mediaRepository
            if (mr.lastFolder() != null) mr.playLastFolder() else mr.playFolder("")
        }

        fun runSmartStart() {
            val config = SmartStartConfig(
                enabled = s.smartStartEnabled,
                playMusic = s.smartStartMusic,
                startNav = s.smartStartNav,
                launchAndroidAuto = s.smartStartAndroidAuto && projectionPkg != null,
                hasOnlineMusic = s.onlineMusicApp != null,
                hasOfflineMusic = hasLocalMusic(),
                hasNav = s.navApp != null,
                projectionActive = projectionOn,
            )
            val plan = SmartStart.plan(config, online)
            if (plan.isEmpty()) {
                message(if (projectionOn) tr("CarPlay/Android Auto è già attivo: l'avvio intelligente non fa nulla per non sovrapporsi.")
                    else tr("Avvio intelligente: niente da fare (controlla le app scelte)."))
                return
            }
            plan.forEach { action ->
                when (action) {
                    SmartAction.PLAY_ONLINE -> driveApp.mediaRepository.resumeOrPlay(s.onlineMusicApp?.packageName)
                    SmartAction.PLAY_OFFLINE -> playLocalMusic()
                    SmartAction.START_NAV -> s.navApp?.let { nav -> Launcher.launchApp(context, nav) }
                    SmartAction.LAUNCH_ANDROID_AUTO ->
                        projectionPkg?.takeIf { System.currentTimeMillis() - lastProjectionLaunch > 60_000 }?.let { pkg ->
                            lastProjectionLaunch = System.currentTimeMillis()
                            pm.getLaunchIntentForPackage(pkg)?.let { intent ->
                                runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            }
                            // Lascia collegare CarPlay/AA e torna subito alla Home DriveOS.
                            projectionBounceUntil = System.currentTimeMillis() + 30_000
                            scope.launch {
                                var waited = 0
                                while (!projectionLive && waited < 20_000) { kotlinx.coroutines.delay(500); waited += 500 }
                                kotlinx.coroutines.delay(1500)
                                bringHomeToFront()
                            }
                        }
                }
            }
        }

        // Scatto automatico: il telefono si collega → avvio intelligente.
        LaunchedEffect(phoneLinked) {
            if (smartTrigger.onState(phoneLinked) && s.smartStartEnabled) {
                driveApp.connectivityWatcher.refresh()
                kotlinx.coroutines.delay(4000) // lascia a SpeedPlay il tempo di agganciarsi, poi decide
                runSmartStart()
            }
        }

        // Quando CarPlay/Android Auto si collega (anche da solo) la sua interfaccia non deve
        // prendere possesso: si resta sulla Home DriveOS, la proiezione continua in secondo piano.
        LaunchedEffect(projectionOn) {
            if (projectionOn && s.smartStartEnabled && s.smartStartAndroidAuto) {
                kotlinx.coroutines.delay(1500)
                bringHomeToFront()
            }
        }

        // Finestre Junsun che prendono lo schermo (portiere, radar, retro) e SpeedPlay: si torna alla
        // Home DriveOS (serve il servizio di accessibilità, attivato in Impostazioni).
        val screenNow by androidx.compose.runtime.rememberUpdatedState(screen)
        val settingsLive by androidx.compose.runtime.rememberUpdatedState(s)
        // Scelta del brano nell'app file: appena parte il lettore dell'app, si torna a DriveOS (la musica continua).
        val pickUntil = remember { longArrayOf(0L) }
        val pickInfo = remember { arrayOfNulls<String>(2) } // [0]=pacchetto, [1]=prima finestra
        LaunchedEffect(Unit) {
            WindowWatcher.events.collect { e ->
                if (System.currentTimeMillis() < pickUntil[0] && e.pkg == pickInfo[0]) {
                    if (pickInfo[1] == null) pickInfo[1] = e.cls
                    else if (e.cls != pickInfo[1]) {
                        pickUntil[0] = 0L
                        kotlinx.coroutines.delay(600)
                        WindowWatcher.goHome()
                        screen = Screen.MUSIC
                        return@collect
                    }
                }
                val cur = settingsLive
                if (!cur.keepHome) return@collect
                val includeProj = System.currentTimeMillis() < projectionBounceUntil
                if (WindowRules.bounceHome(e.kind, includeProj)) {
                    kotlinx.coroutines.delay(250)
                    bringHomeToFront()
                    if (e.kind != it.driveos.launcher.vehicle.WindowKind.PROJECTION && screenNow != Screen.VEHICLE) {
                        overviewAuto = true; screen = Screen.OVERVIEW
                    }
                }
            }
        }
        // Sensori di parcheggio attivi o retromarcia mentre DriveOS e' dietro (finestra radar/retro della
        // radio Junsun): DriveOS torna davanti con la sua schermata. Funziona anche senza Accessibilita'.
        val sensorsActive = (CanDecoder.sensorLevels(vehicleSignals, CanDecoder.REAR_SENSORS)?.any { it > 0 } == true) ||
            (s?.frontSensors == true && CanDecoder.sensorLevels(vehicleSignals, CanDecoder.FRONT_SENSORS)?.any { it > 0 } == true)
        val reverseSig = KnownVehicleSignals.reverseOn(vehicleSignals) == true
        // Solo quando sensori/retro SI ACCENDONO: se poi esci tu da DriveOS (es. impostazioni Junsun)
        // non ti riporta indietro.
        val fgNow by androidx.compose.runtime.rememberUpdatedState(isForeground)
        LaunchedEffect(sensorsActive, reverseSig) {
            val cur = settingsState ?: return@LaunchedEffect
            if (!cur.keepHome || fgNow || !(sensorsActive || reverseSig)) return@LaunchedEffect
            kotlinx.coroutines.delay(400) // lascia aprire la finestra Junsun, poi la si copre
            bringHomeToFront()
            if (!reverseSig && screen != Screen.VEHICLE) { overviewAuto = true; screen = Screen.OVERVIEW }
        }
        // riportata davanti dall'app per i sensori (es. da AutoLink): si apre la schermata Veicolo
        LaunchedEffect(isForeground) {
            if (isForeground && DriveOSApp.openOverview) {
                DriveOSApp.openOverview = false
                if (KnownVehicleSignals.reverseOn(vehicleSignals) != true && screen != Screen.VEHICLE) { overviewAuto = true; screen = Screen.OVERVIEW }
            }
        }
        var a11yOn by remember { mutableStateOf(WindowWatcher.enabled()) }
        LaunchedEffect(Unit) { while (true) { a11yOn = WindowWatcher.enabled(); kotlinx.coroutines.delay(2000) } }

        // Retromarcia: il firmware porta davanti la sua app (forfan.want2start = com.ts.MainUI, come
        // nel registro). La telecamera non c'è: si resta/si torna sulla Home DriveOS.
        val reverseNow = KnownVehicleSignals.reverseOn(vehicleSignals) == true
        val foregroundPkg = vehicleSignals["prop:forfan.want2start"]
        // Retromarcia inserita: subito la schermata Veicolo (e DriveOS davanti a qualunque finestra Junsun).
        // Tolta la retro: si torna alla Home, salvo che l'auto sia ferma.
        val parkedNow by androidx.compose.runtime.rememberUpdatedState(parked)
        var reverseOpened by remember { mutableStateOf(false) }
        val settingsNowRev by androidx.compose.runtime.rememberUpdatedState(s)
        LaunchedEffect(reverseNow) {
            driveApp.canReader.steerWanted = reverseNow
            if (reverseNow) {
                reverseOpened = true
                // schermata retromarcia (telecamera o solo sensori)
                val target = if (settingsNowRev.rearCamera) Screen.REAR_CAM else Screen.OVERVIEW
                if (screen != target && screen != Screen.VEHICLE) { overviewAuto = true; screen = target }
                if (!fgNow) WindowWatcher.goHome()
            } else if (reverseOpened) {
                reverseOpened = false
                kotlinx.coroutines.delay(1500)
                if (screen == Screen.REAR_CAM) { if (parkedNow) { screen = Screen.OVERVIEW; overviewAuto = true } else { screen = Screen.DASHBOARD; overviewAuto = false } }
                else if (!parkedNow && screen == Screen.OVERVIEW) { screen = Screen.DASHBOARD; overviewAuto = false }
            }
        }
        val overlayGranted = remember(a11yOn, screen) { android.provider.Settings.canDrawOverlays(context) }

        // Posizione (velocità GPS per "auto ferma"): la chiede da sola una volta se manca.
        LaunchedEffect(s.autoOverview) {
            if (s.autoOverview && !driveApp.speedWatcher.hasPermission()) {
                kotlinx.coroutines.delay(3000)
                locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        // Nessun telefono dopo l'accensione: parte da sola la musica locale della radio.
        val phoneLinkedNow by androidx.compose.runtime.rememberUpdatedState(phoneLinked)
        val playingNow by androidx.compose.runtime.rememberUpdatedState(media?.isPlaying == true)
        val settingsNow by androidx.compose.runtime.rememberUpdatedState(s)
        val projectionNow by androidx.compose.runtime.rememberUpdatedState(projectionOn)
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(25_000) // tempo a Bluetooth/vivavoce per agganciarsi
            if (!phoneLinkedNow) {
                val cur = settingsNow
                val config = SmartStartConfig(
                    enabled = cur.smartStartEnabled, playMusic = cur.smartStartMusic,
                    hasOfflineMusic = true, // c'e' sempre il lettore interno di DriveOS
                    projectionActive = projectionNow,
                )
                if (SmartStart.planWithoutPhone(config, playingNow).isNotEmpty()) playLocalMusic()
            }
        }

        // Auto-seleziona il navigatore impostato nel sistema (es. forfan.navi.name = Maps).
        LaunchedEffect(apps, vehicleSignals[KnownVehicleSignals.SYSTEM_NAV_PROP]) {
            if (s.navApp == null && apps.isNotEmpty()) {
                val pkg = KnownVehicleSignals.systemNavPackage(vehicleSignals)
                if (pkg != null) apps.firstOrNull { it.key.packageName == pkg }?.let {
                    driveApp.settingsRepository.setNavApp(it.key)
                }
            }
        }

        // Stato dei fari (collegamento esplicito o segnale noto automatico) e tema automatico.
        val lightsResolved = VehicleStatus.lightsOn(s.lightsBinding, vehicleSignals)

        // Schermata veicolo automatica: si apre quando l'auto è ferma (GPS) o si apre una portiera,
        // solo partendo dalla Home (non interrompe navigazione, radio, impostazioni);
        // si richiude da sola quando riparti, se era stata aperta in automatico.
        val handbrakeOn = vehicleSignals[CanDecoder.HANDBRAKE] == "1"
        // Porte: dalla seriale (si perde qualche messaggio, la linea e' condivisa con la radio) OPPURE dal
        // registro (finestra porte di MainUI): basta una delle due.
        val doorOpenNow = CanDecoder.openDoors(vehicleSignals)?.isNotEmpty() == true || vehicleSignals[LogcatParser.DOOR] == "aperta"
        LaunchedEffect(parked, handbrakeOn, doorOpenNow, reverseNow, s.autoOverview) {
            if (!s.autoOverview) return@LaunchedEffect
            if ((parked || handbrakeOn || doorOpenNow || reverseNow) && screen == Screen.DASHBOARD) {
                screen = Screen.OVERVIEW; overviewAuto = true
            } else if (!parked && !handbrakeOn && !doorOpenNow && !reverseNow && overviewAuto && screen == Screen.OVERVIEW) {
                screen = Screen.DASHBOARD; overviewAuto = false
            }
        }
        // In marcia (velocita' dalla centralina, non serve il GPS): la schermata aperta da sola si richiude.
        val movingCan = (vehicleSignals[CanDecoder.SPEED]?.toDoubleOrNull() ?: 0.0) >= 8.0
        // Porta o baule aperti (anche in marcia): si apre la schermata Veicolo; richiusi, si torna dov'eri.
        val doorLive by androidx.compose.runtime.rememberUpdatedState(doorOpenNow)
        var doorPrev by remember { mutableStateOf<Screen?>(null) }
        LaunchedEffect(doorOpenNow) {
            if (!s.autoOverview) return@LaunchedEffect
            if (doorOpenNow) {
                if (screen != Screen.OVERVIEW && screen != Screen.REAR_CAM && screen != Screen.VEHICLE) {
                    doorPrev = screen; screen = Screen.OVERVIEW; overviewAuto = true
                }
            } else doorPrev?.let { prev ->
                kotlinx.coroutines.delay(1500)
                val movingNow = (driveApp.vehicleRepository.signals.value[CanDecoder.SPEED]?.toDoubleOrNull() ?: 0.0) >= 8.0
                if (!doorLive && screen == Screen.OVERVIEW && (!parkedNow || movingNow)) { screen = prev; overviewAuto = false }
                doorPrev = null
            }
        }
        LaunchedEffect(movingCan) {
            if (movingCan && overviewAuto && !reverseNow && !doorLive && (screen == Screen.OVERVIEW || screen == Screen.REAR_CAM)) {
                screen = Screen.DASHBOARD; overviewAuto = false
            }
        }
        val vehicleGauges = remember(s.signalBindings, vehicleSignals) {
            VehicleStatus.knownStatus(vehicleSignals)
        }
        val vehicleAlerts = remember(s.signalBindings, vehicleSignals) {
            VehicleStatus.knownActiveAlerts(vehicleSignals)
        }
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

        /** Tasto Radio della Home: apre la radio DriveOS (o l'app radio che hai scelto). */
        val bridge = driveApp.tsBridge
        var readingNames by remember { mutableStateOf(false) }
        // Giro sui preferiti con la radio nativa davanti: e' la sua schermata a scrivere il nome RDS nel registro.
        fun readNames() {
            if (readingNames) return
            readingNames = true
            driveApp.logcatReader.readingNames = true
            scope.launch {
                // riparte da zero: i nomi sbagliati vengono sostituiti da quelli letti
                driveApp.settingsRepository.setRadioPresets(s.radioPresets.map { if (it.khz > 0) it.copy(name = "FM " + RadioPreset(it.khz, "").displayFrequency()) else it })
                driveApp.logcatReader.clearNames()
                val list = s.radioPresets.filter { it.khz > 0 }
                runCatching {
                    context.startActivity(Intent().setClassName("com.ts.MainUI", "com.ts.main.radio.RadioMainActivity")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                kotlinx.coroutines.delay(1500)
                for (p in list) { bridge.tuneFm(p.khz); kotlinx.coroutines.delay(6000) }
                if (!WindowWatcher.goHome()) {
                    runCatching { context.startActivity(Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
                }
                screen = Screen.RADIO
                driveApp.logcatReader.readingNames = false
                readingNames = false
            }
        }
        // Come "Leggi nomi" ma solo per la stazione sintonizzata ora (anche se non e' nei preferiti).
        fun readOne() {
            val k = bridgeKhz ?: return
            if (readingNames) return
            readingNames = true
            driveApp.logcatReader.expectName(k)
            scope.launch {
                runCatching {
                    context.startActivity(Intent().setClassName("com.ts.MainUI", "com.ts.main.radio.RadioMainActivity")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                kotlinx.coroutines.delay(4000)
                if (!WindowWatcher.goHome()) {
                    runCatching { context.startActivity(Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
                }
                screen = Screen.RADIO
                readingNames = false
            }
        }
        fun radioPress(id: String, viaBridge: (() -> Boolean)? = null) {
            if (viaBridge?.invoke() == true) return
            if (!NativeRadio.press(context, id)) {
                message(if (!WindowWatcher.enabled()) tr("Per comandare la radio attiva il servizio Accessibilità di DriveOS (Impostazioni → Veicolo).") else tr("Comando radio ancora in corso, riprova tra un attimo."))
            }
        }

        fun openRadio() {
            screen = Screen.RADIO
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
            message(tr("Attiva DriveOS nell'elenco, poi torna indietro."))
        }

        fun connectCalendar() {
            if (driveApp.calendarRepository.hasPermission()) driveApp.calendarRepository.refresh()
            else calendarPermission.launch(Manifest.permission.READ_CALENDAR)
        }

        // --- Comandi vocali offline (Vosk) ---
        var voiceOpen by remember { mutableStateOf(false) }
        var langDialog by remember { mutableStateOf(false) }
        LaunchedEffect(s.voiceLang) {
            driveApp.voice.language = s.voiceLang
            // lingua dell'interfaccia: si ridisegna tutto con le nuove scritte
            if (it.driveos.launcher.i18n.I18n.lang != s.voiceLang) { it.driveos.launcher.i18n.I18n.lang = s.voiceLang; this@MainActivity.recreate() }
        }
        if (langDialog) androidx.compose.material3.AlertDialog(
            onDismissRequest = { langDialog = false },
            title = { Text(tr("Lingua comandi vocali")) },
            text = {
                Column {
                    it.driveos.launcher.voice.VoiceLanguages.all.forEach { l ->
                        Text((if (l.code == s.voiceLang) "● " else "○ ") + l.label, fontSize = 22.sp,
                            modifier = Modifier.fillMaxWidth().clickable {
                                langDialog = false
                                scope.launch { repo.setVoiceLang(l.code) }
                            }.padding(vertical = 12.dp))
                    }
                    Text(tr("La nuova lingua si scarica (circa 40-50 MB) la prima volta che premi Voce."), fontSize = 14.sp)
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { langDialog = false }) { Text(tr("Chiudi")) } },
        )
        var voiceHeard by remember { mutableStateOf<String?>(null) }
        val voiceState by driveApp.voice.state.collectAsStateWithLifecycle()
        var voiceForNav by remember { mutableStateOf(false) }
        // Mentre parli: musica/app in pausa e audio di sistema (radio, AutoLink) muto; alla fine si ripristina.
        val voiceHold = remember { object { var held = false; var muted = false; var muteJob: kotlinx.coroutines.Job? = null } }
        fun holdAudio() {
            if (voiceHold.held) return
            voiceHold.held = true
            driveApp.mediaRepository.pauseForVoice()
            voiceHold.muteJob = scope.launch {
                if (driveApp.tsBridge.isMuted() == false) { driveApp.tsBridge.setMuted(true); voiceHold.muted = true }
            }
        }
        /** Ritorna true se della musica era stata messa in pausa. */
        fun releaseAudio(resumeMedia: Boolean = true, unmute: Boolean = true): Boolean {
            if (!voiceHold.held) return false
            voiceHold.held = false
            val had = driveApp.mediaRepository.resumeAfterVoice(resumeMedia)
            val job = voiceHold.muteJob
            scope.launch {
                job?.join()
                if (voiceHold.muted && !voiceHold.held) { voiceHold.muted = false; if (unmute) driveApp.tsBridge.setMuted(false) }
            }
            return had
        }
        fun handleVoice(text: String) {
            val cmd = VoiceCommands.parse(text, s.voiceLang).let { c ->
                // dal tasto microfono della ricerca: una frase "libera" e' una destinazione
                if (voiceForNav && c is VoiceCommand.Unknown) VoiceCommand.Navigate(text) else c
            }
            val weMuted = voiceHold.muted
            val hadMusic = releaseAudio(
                resumeMedia = cmd !is VoiceCommand.Station && cmd !is VoiceCommand.Frequency && cmd != VoiceCommand.Radio && cmd != VoiceCommand.Pause,
                unmute = cmd != VoiceCommand.Mute,
            )
            voiceForNav = false
            fun tuneTo(khz: Int) {
                scope.launch { driveApp.settingsRepository.setLastRadioKhz(khz) }
                if (screen == Screen.RADIO) driveApp.tsBridge.tuneFm(khz) else screen = Screen.RADIO
            }
            when (cmd) {
                is VoiceCommand.Navigate -> navigateTo(cmd.destination)
                VoiceCommand.GoHome -> goHome()
                VoiceCommand.GoWork -> goWork()
                VoiceCommand.StopNav -> { guidance?.let { g -> NavGuidanceStore.stop(g) } }
                VoiceCommand.Radio -> openRadio()
                is VoiceCommand.Station -> {
                    val q = VoiceCommands.normalize(cmd.name).replace(" ", "")
                    val preset = s.radioPresets.firstOrNull { p -> p.khz > 0 && VoiceCommands.normalize(p.name).replace(" ", "").let { n -> n.isNotEmpty() && (n.contains(q) || q.contains(n)) } }
                    val learned = driveApp.logcatReader.names.value.entries.firstOrNull { e -> VoiceCommands.normalize(e.value).replace(" ", "").let { n -> n.isNotEmpty() && (n.contains(q) || q.contains(n)) } }?.key
                    val khz = preset?.khz ?: learned
                    if (khz != null) tuneTo(khz) else { openRadio(); message(trf("Non trovo la stazione \"{0}\" tra quelle conosciute.", cmd.name)) }
                }
                is VoiceCommand.Frequency -> tuneTo(cmd.khz)
                VoiceCommand.Music -> { screen = Screen.MUSIC; if (!hadMusic && driveApp.mediaRepository.media.value?.isPlaying != true && driveApp.mediaRepository.lastFolder() != null) driveApp.mediaRepository.playLastFolder() }
                VoiceCommand.Next -> if (screen == Screen.RADIO) driveApp.tsBridge.radioCall { r -> r.SeekUp() } else driveApp.mediaRepository.next()
                VoiceCommand.Previous -> if (screen == Screen.RADIO) driveApp.tsBridge.radioCall { r -> r.SeekDn() } else driveApp.mediaRepository.previous()
                VoiceCommand.Pause -> if (driveApp.mediaRepository.media.value?.isPlaying == true) driveApp.mediaRepository.playPause()
                VoiceCommand.Play -> if (!hadMusic && driveApp.mediaRepository.media.value?.isPlaying != true) driveApp.mediaRepository.playPause()
                VoiceCommand.VolumeUp -> driveApp.tsBridge.functionCall { f -> f.onVolumeUp(); f.onVolumeUp() }
                VoiceCommand.VolumeDown -> driveApp.tsBridge.functionCall { f -> f.onVolumeDown(); f.onVolumeDown() }
                VoiceCommand.Mute -> if (!weMuted) driveApp.tsBridge.functionCall { f -> if (f.isVolumeMute()) f.onVolumeUnmute() else f.onVolumeMute() }
                VoiceCommand.Climate -> screen = Screen.CLIMATE
                VoiceCommand.Vehicle -> { overviewAuto = false; screen = Screen.OVERVIEW }
                VoiceCommand.Home -> screen = Screen.DASHBOARD
                VoiceCommand.Phone -> openOrPick(s.phoneApp, PickerTarget.PHONE)
                is VoiceCommand.Unknown -> { voiceHeard = text.ifBlank { null }; return }
            }
            voiceOpen = false
        }
        fun listenNow() {
            voiceHeard = null
            holdAudio()
            driveApp.voice.listen { text -> if (text.isBlank()) { voiceHeard = null; releaseAudio() } else handleVoice(text) }
        }
        val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) listenNow() else { voiceOpen = false; message(tr("Senza il permesso del microfono i comandi vocali non funzionano.")) }
        }
        fun startOfflineVoice(forNav: Boolean = false) {
            voiceForNav = forNav
            voiceOpen = true
            if (!driveApp.voice.installed()) return // la finestra propone il download
            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            else listenNow()
        }
        // errore microfono o finestra chiusa in altro modo: l'audio torna com'era
        LaunchedEffect(voiceState, voiceOpen) {
            if (!voiceOpen || voiceState is it.driveos.launcher.voice.OfflineVoice.State.Error) releaseAudio()
        }
        if (voiceOpen) it.driveos.launcher.ui.VoiceOverlay(
            state = voiceState,
            lastHeard = voiceHeard,
            onInstall = { driveApp.voice.install { ok -> if (ok) startOfflineVoice(voiceForNav) } },
            onRetry = { driveApp.voice.dismissError(); startOfflineVoice(voiceForNav) },
            onClose = { driveApp.voice.stop(); releaseAudio(); voiceOpen = false },
        )

        fun startVoice() = startOfflineVoice(forNav = true)

        fun openSource() {
            val pkg = sourceEntry?.key?.packageName ?: s.musicApp?.packageName
            if (pkg != null && WindowWatcher.enabled()) {
                pickUntil[0] = System.currentTimeMillis() + 5 * 60_000L; pickInfo[0] = pkg; pickInfo[1] = null
            }
            if (sourceEntry != null) report(Launcher.launchApp(context, sourceEntry.key))
            else openOrPick(s.musicApp, PickerTarget.MUSIC)
        }

        // Tasti dell'autoradio/volante imparati dalla seriale che richiedono la schermata.
        LaunchedEffect(Unit) {
            KeyEvents.flow.collect { a ->
                when (a) {
                    KeyAction.HOME -> { screen = Screen.DASHBOARD; overviewAuto = false; bringHomeFromBackground() }
                    KeyAction.RADIO -> openRadio()
                    // settingsLive: le app scelte ADESSO (questo blocco parte una volta sola, "s" resterebbe quello dell'avvio)
                    KeyAction.NAV -> openOrPick(settingsLive.navApp, PickerTarget.NAV)
                    KeyAction.PHONE -> openOrPick(settingsLive.phoneApp, PickerTarget.PHONE)
                    KeyAction.MUSIC -> openOrPick(settingsLive.musicApp, PickerTarget.MUSIC)
                    KeyAction.VOICE -> startOfflineVoice()
                    // tasto Indietro dell'autoradio letto da DriveOS (la radio non l'ha visto): indietro di un livello
                    KeyAction.BACK -> if (isForeground) this@MainActivity.onBackPressedDispatcher.onBackPressed()
                        else WindowWatcher.service?.back()
                    else -> {}
                }
            }
        }

        // Indietro: chiude i controlli; dalle viste a tutto schermo torna alla Home.
        BackHandler(enabled = true) {
            // Indietro mandato da DriveOS per chiudere la finestra porte Junsun: non e' per noi
            if (System.currentTimeMillis() < WindowWatcher.ignoreBackUntil) return@BackHandler
            if (controlsOpen) { controlsOpen = false } else screen = when (screen) {
                Screen.DEVICE_INFO -> Screen.SETTINGS
                Screen.VEHICLE -> if (debugFromOverview) { debugFromOverview = false; Screen.OVERVIEW } else Screen.SETTINGS
                Screen.RADIO, Screen.SMART_START -> Screen.SETTINGS
                Screen.KEYS -> Screen.VEHICLE
                Screen.OVERVIEW -> { overviewAuto = false; Screen.DASHBOARD }
                else -> Screen.DASHBOARD
            }
        }

        DriveOSTheme(s.themeMode, s.accentIndex, autoDark) {
            val appInfo = remember {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
            }
            val appVersion = appInfo?.longVersionCode ?: 0L
            if (s.termsAccepted != appVersion) {
                // condizioni d'uso: a ogni installazione e a ogni aggiornamento
                it.driveos.launcher.ui.TermsScreen(
                    version = appInfo?.versionName ?: "",
                    onAccept = { scope.launch { repo.setTermsAccepted(appVersion) } },
                    onDecline = {
                        // senza accettazione DriveOS non si usa: si apre la scelta della schermata Home
                        runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        this@MainActivity.finish()
                    },
                )
            } else if (!s.setupDone) {
                // primo avvio: lingua, avvertenze e preferenze
                it.driveos.launcher.ui.SetupWizard(
                    current = it.driveos.launcher.ui.SetupChoices(s.voiceLang, s.rearCamera, s.frontSensors, s.touchLimit, s.canSerial),
                    onLanguage = { code -> scope.launch { repo.setVoiceLang(code) } },
                    onPickNav = { picker = PickerTarget.NAV },
                    navLabel = s.navApp?.let { k -> apps.firstOrNull { a -> a.key == k }?.label },
                    onFinish = { ch ->
                        scope.launch {
                            repo.setRearCamera(ch.rearCamera); repo.setFrontSensors(ch.frontSensors)
                            repo.setTouchLimit(ch.touchLimit); repo.setCanSerial(ch.canSerial)
                            repo.setSetupDone(true)
                        }
                        if (ch.rearCamera && context.checkSelfPermission(Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) cameraPermission.launch(Manifest.permission.CAMERA)
                    },
                )
                picker?.let { target ->
                    AppPickerDialog(
                        title = target.title,
                        apps = apps,
                        selected = s.navApp,
                        onPick = { app -> scope.launch { repo.setNavApp(app?.key) }; picker = null },
                        onDismiss = { picker = null },
                    )
                }
                return@DriveOSTheme
            }
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
                        // Sulla Home solo le spie attive; il resto è nella schermata Veicolo.
                        if (vehicleAlerts.isNotEmpty()) VehicleStatusStrip(
                            alerts = vehicleAlerts,
                            onOpenVehicle = { overviewAuto = false; screen = Screen.OVERVIEW },
                        )
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (screen) {
                            Screen.DASHBOARD -> DashboardScreen(s.layout) { type ->
                                when (type) {
                                    CardType.CLOCK -> ClockCard(onClick = { screen = Screen.CLOCK }, outTemp = vehicleSignals[CanDecoder.OUT_TEMP])
                                    CardType.NAV -> NavCard(
                                        navEntry = navEntry,
                                        configured = s.navApp != null,
                                        guidance = guidance,
                                        recent = s.recentDestinations,
                                        onOpenDetail = { textDialog = TextTarget.SEARCH },
                                        onChooseApp = { picker = PickerTarget.NAV },
                                        onResume = { openOrPick(s.navApp, PickerTarget.NAV) },
                                        onHome = { goHome() },
                                        onWork = { goWork() },
                                        onDestination = { navigateTo(it) },
                                        onStop = {
                                            guidance?.let { g -> if (!NavGuidanceStore.stop(g)) message(tr("L'app di navigazione non offre il comando per terminare: aprila e chiudi il percorso da lì.")) }
                                        },
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
                                onSeek = { mediaRepo.seekTo(it) },
                                shuffle = driveApp.mediaRepository.shuffle.collectAsStateWithLifecycle().value,
                                repeat = driveApp.mediaRepository.repeat.collectAsStateWithLifecycle().value,
                                onShuffle = { if (!driveApp.mediaRepository.toggleShuffle()) message(tr("Questo lettore non permette di cambiare la riproduzione casuale.")) },
                                onRepeat = { if (!driveApp.mediaRepository.cycleRepeat()) message(tr("Questo lettore non permette di cambiare la ripetizione.")) },
                                onOpenSource = { openSource() },
                                onPickFolder = {
                                    if (!it.driveos.launcher.media.LocalMusicService.hasPermission(context)) { pendingFolders = true; storagePermission.launch(it.driveos.launcher.media.LocalMusicService.permission()) }
                                    else showFolders = true
                                },
                                onPlayLast = if (driveApp.mediaRepository.lastFolder() != null) ({ driveApp.mediaRepository.playLastFolder() }) else null,
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
                                onOpenSettings = { screen = Screen.SETTINGS },
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
                                onOpenSmartStart = { screen = Screen.SMART_START },
                                smartStartEnabled = s.smartStartEnabled,
                                autoOverview = s.autoOverview,
                                autoOverviewHasGps = driveApp.speedWatcher.hasPermission(),
                                onToggleAutoOverview = {
                                    val gps = driveApp.speedWatcher.hasPermission()
                                    when {
                                        // attiva ma senza GPS: il tocco chiede la posizione (non la spegne)
                                        s.autoOverview && !gps -> locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                        s.autoOverview -> scope.launch { repo.setAutoOverview(false) }
                                        else -> {
                                            scope.launch { repo.setAutoOverview(true) }
                                            if (!gps) locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                        }
                                    }
                                },
                                onOpenOverview = { overviewAuto = false; screen = Screen.OVERVIEW },
                                keepHome = s.keepHome,
                                accessibilityOn = a11yOn,
                                overlayGranted = overlayGranted,
                                onOverlay = {
                                    runCatching {
                                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    }.onFailure { message(tr("Non riesco ad aprire la schermata dei permessi.")) }
                                },
                                onAccessibility = { report(Launcher.openSystemSettings(context, Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                                onToggleKeepHome = { scope.launch { repo.setKeepHome(!s.keepHome) } },
                                onEditLocation = { textDialog = TextTarget.LOCATION },
                                onHomeSettings = { report(Launcher.openSystemSettings(context, Settings.ACTION_HOME_SETTINGS)) },
                                onAndroidSettings = { report(Launcher.openSystemSettings(context, Settings.ACTION_SETTINGS)) },
                                voiceLanguage = it.driveos.launcher.voice.VoiceLanguages.of(s.voiceLang).label,
                                voiceModel = driveApp.voice.installedModel(),
                                onVoiceLanguage = { langDialog = true },
                                onVoiceUpdate = {
                                    scope.launch {
                                        if (!driveApp.voice.installed()) { startOfflineVoice(); return@launch }
                                        message(tr("Controllo se c'e' una voce piu' recente…"))
                                        if (driveApp.voice.updateAvailable()) { voiceOpen = true; driveApp.voice.install() }
                                        else message(tr("La voce e' gia' la piu' recente."))
                                    }
                                },
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
                                signals = vehicleSignals,
                                logsGranted = driveApp.logcatReader.available(),
                                onAutoSetup = {
                                    message(tr("Configuro i permessi…"))
                                    Thread { it.driveos.launcher.system.SelfSetup.run(driveApp) }.start()
                                },
                                loggingOn = loggingOn,
                                loggedChanges = loggedChanges,
                                onToggleLogging = { on -> scope.launch { driveApp.settingsRepository.setVehicleLogging(on) } },
                                canSerialOn = s.canSerial,
                                onToggleCanSerial = { on -> scope.launch { driveApp.settingsRepository.setCanSerial(on) } },
                                frontSensorsOn = s.frontSensors,
                                onToggleFrontSensors = { on -> scope.launch { driveApp.settingsRepository.setFrontSensors(on) } },
                                touchLimitOn = s.touchLimit,
                                rearCameraOn = s.rearCamera,
                                onToggleRearCamera = { on ->
                                    scope.launch { driveApp.settingsRepository.setRearCamera(on) }
                                    if (on && context.checkSelfPermission(Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) cameraPermission.launch(Manifest.permission.CAMERA)
                                },
                                onToggleTouchLimit = { on -> scope.launch { driveApp.settingsRepository.setTouchLimit(on) } },
                                onOpenKeys = { screen = Screen.KEYS },
                                onExportLog = {
                                    scope.launch {
                                        val stamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                                        val bytes = withContext(kotlinx.coroutines.Dispatchers.IO) { VehicleLog.readAll(context) }
                                        val r = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            it.driveos.launcher.system.Exporter.save(context, "driveos_registro_$stamp.csv", "text/csv", bytes)
                                        }
                                        vehicleExportStatus = if (r.ok) trf("Registro ({0} variazioni) salvato in: ", loggedChanges) + r.saved.joinToString(" · ")
                                        else tr("Export registro non riuscito. ") + r.errors.firstOrNull().orEmpty()
                                    }
                                },
                                onClearLog = {
                                    scope.launch { withContext(kotlinx.coroutines.Dispatchers.IO) { VehicleLog.clear(context) } }
                                    vehicleExportStatus = tr("Registro azzerato.")
                                },
                                exportStatus = vehicleExportStatus,
                                onBack = { if (debugFromOverview) { debugFromOverview = false; screen = Screen.OVERVIEW } else screen = Screen.SETTINGS },
                            )

                            Screen.KEYS -> {
                                val learning by driveApp.keyMapper.learning.collectAsStateWithLifecycle()
                                val keyMessage by driveApp.keyMapper.message.collectAsStateWithLifecycle()
                                KeysScreen(
                                    bindings = s.keyBindings,
                                    learning = learning,
                                    message = keyMessage,
                                    onLearn = { a -> driveApp.keyMapper.startLearning(a) },
                                    onClear = { a -> scope.launch { repo.setKeyBindings(KeyBindingCodec.clear(s.keyBindings, a)) } },
                                    onCancel = { driveApp.keyMapper.cancelLearning() },
                                    onBack = { screen = Screen.VEHICLE },
                                )
                            }

                            Screen.RADIO -> RadioScreen(
                                state = bridgeKhz?.let { RadioState(khz = it, source = "radio") } ?: radioState ?: vehicleSignals[LogcatParser.RADIO_FREQ]?.toIntOrNull()?.let { k ->
                                    RadioState(khz = k, source = "log")
                                },
                                detected = radioDetected,
                                presets = s.radioPresets,
                                currentKhz = bridgeKhz ?: radioState?.khz,
                                onTune = { p ->
                                    val slot = s.radioPresets.indexOf(p) + 1
                                    radioPress(NativeRadio.preset(slot)) { if (p.band == RadioBand.FM) bridge.tuneFm(p.khz) else false }
                                },
                                onSeekDown = { radioPress(NativeRadio.SEEK_DOWN) { bridge.radioCall { it.SeekDn() } } },
                                onSeekUp = { radioPress(NativeRadio.SEEK_UP) { bridge.radioCall { it.SeekUp() } } },
                                onFm = { radioPress(NativeRadio.FM) { bridge.radioCall { it.onRadioFM() } } },
                                onAm = { radioPress(NativeRadio.AM) { bridge.radioCall { it.onRadioAM() } } },
                                onAdd = { radioDialog = true },
                                onSaveCurrent = {
                                    val khz = bridgeKhz ?: radioState?.khz ?: vehicleSignals[LogcatParser.RADIO_FREQ]?.toIntOrNull()
                                    if (khz != null) {
                                        val p = RadioPreset(khz, stationNames[khz] ?: "FM ${RadioPreset(khz, "").displayFrequency()}", RadioBand.FM)
                                        if (RadioPresetLogic.isFull(s.radioPresets, p)) message(tr("Hai già 6 stazioni: rimuovine una."))
                                        else scope.launch { driveApp.settingsRepository.setRadioPresets(RadioPresetLogic.add(s.radioPresets, p)) }
                                    }
                                },
                                onSaveSlot = { slot ->
                                    val khz = bridgeKhz ?: radioState?.khz ?: vehicleSignals[LogcatParser.RADIO_FREQ]?.toIntOrNull()
                                    if (khz != null) {
                                        val name = stationNames[khz] ?: "FM ${RadioPreset(khz, "").displayFrequency()}"
                                        scope.launch { driveApp.settingsRepository.setRadioPresets(RadioPresetLogic.setSlot(s.radioPresets, slot, RadioPreset(khz, name, RadioBand.FM))) }
                                        message("P$slot: $name ${RadioPreset(khz, "").displayFrequency()}")
                                    }
                                },
                                onRemove = { p -> scope.launch {
                                    driveApp.settingsRepository.setRadioPresets(RadioPresetLogic.remove(s.radioPresets, p.band, p.khz))
                                } },
                                onOpenExternal = {
                                    // Radio Junsun (com.ts.MainUI) se non hai scelto un'altra app radio
                                    val ok = s.radioApp == null && runCatching {
                                        context.startActivity(Intent().setClassName("com.ts.MainUI", "com.ts.main.radio.RadioMainActivity")
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    }.isSuccess
                                    if (!ok) openOrPick(s.radioApp, PickerTarget.RADIO)
                                },
                                stationName = (bridgeKhz ?: logKhz)?.let { stationNames[it] },
                                logsGranted = driveApp.logcatReader.available(),
                                rdsOn = bridgeRds,
                                rdsStatus = rdsState.takeIf { it.khz == (bridgeKhz ?: logKhz) }?.status?.name,
                                onBack = { screen = Screen.SETTINGS },
                            )

                            Screen.REAR_CAM -> it.driveos.launcher.ui.RearCameraScreen(
                                rearLevels = CanDecoder.sensorLevels(vehicleSignals, CanDecoder.REAR_SENSORS),
                                rearRaw = CanDecoder.sensorRaw(vehicleSignals, CanDecoder.REAR_SENSORS),
                                cameraEnabled = s.rearCamera,
                                onHome = { overviewAuto = false; screen = Screen.DASHBOARD },
                                steer = vehicleSignals[CanDecoder.STEER]?.toIntOrNull(),
                            )

                            Screen.CLIMATE -> it.driveos.launcher.ui.ClimateScreen(
                                signals = vehicleSignals,
                                onBack = { climateAuto = false; screen = Screen.DASHBOARD },
                            )

                            Screen.OVERVIEW -> VehicleOverviewScreen(
                                state = VehicleOverviewState(
                                    lightsOn = lightsResolved,
                                    reverse = KnownVehicleSignals.reverseOn(vehicleSignals),
                                    rearview = KnownVehicleSignals.rearviewOn(vehicleSignals),
                                    doorOpen = doorOpenNow,
                                    openDoors = CanDecoder.openDoors(vehicleSignals),
                                    handbrake = vehicleSignals[CanDecoder.HANDBRAKE]?.let { it == "1" },
                                    batteryV = vehicleSignals[CanDecoder.BATTERY_V] ?: vehicleSignals[CanDecoder.VOLTAGE],
                                    rpm = vehicleSignals[CanDecoder.RPM],
                                    fuelL = vehicleSignals[CanDecoder.FUEL_L],
                                    odometerKm = vehicleSignals[CanDecoder.ODOMETER_KM],
                                    outTemp = vehicleSignals[CanDecoder.OUT_TEMP],
                                    belt = vehicleSignals[CanDecoder.BELT]?.let { it == "1" },
                                    beltPassenger = vehicleSignals[CanDecoder.BELT_PASSENGER]?.let { it == "1" },
                                    rearLevels = CanDecoder.sensorLevels(vehicleSignals, CanDecoder.REAR_SENSORS),
                                    rearRaw = CanDecoder.sensorRaw(vehicleSignals, CanDecoder.REAR_SENSORS),
                                    frontLevels = if (s.frontSensors) CanDecoder.sensorLevels(vehicleSignals, CanDecoder.FRONT_SENSORS) else null,
                                    parking = vehicleSignals[LogcatParser.PARKING]?.let { v -> v == "1" },
                                    phone = KnownVehicleSignals.phoneConnected(vehicleSignals),
                                    projection = KnownVehicleSignals.projectionActive(vehicleSignals),
                                    radioKhz = vehicleSignals[LogcatParser.RADIO_FREQ]?.toIntOrNull(),
                                    lightsDetail = run {
                                        // verificato in auto il 07/11: 0x7D/01 bit 0x40 = anabbaglianti; le luci di posizione
                                        // non passano dalla CAN ma accendono l'illuminazione della radio (forfan_light_state)
                                        val low = vehicleSignals[CanDecoder.LIGHTS]; val high = vehicleSignals[CanDecoder.LOWBEAM]
                                        val fog = vehicleSignals[CanDecoder.FOG]; val ill = KnownVehicleSignals.lightsOn(vehicleSignals)
                                        if (low == null && ill == null) null else {
                                            val base = when { low == "1" -> "anabbaglianti"; ill == true -> "posizione"; else -> "spente" }
                                            base + (if (high == "1") " + abbaglianti" else "") + (if (fog == "1") " + retronebbia" else "")
                                        }
                                    },
                                    speedKmh = speedKmh,
                                    alerts = vehicleAlerts,
                                    logsGranted = driveApp.logcatReader.available(),
                                    gpsPermission = driveApp.speedWatcher.hasPermission(),
                                ),
                                onDebug = { overviewAuto = false; screen = Screen.DASHBOARD },
                            )

                            Screen.SMART_START -> SmartStartScreen(
                                settings = s,
                                appLabel = { key -> apps.firstOrNull { it.key == key }?.label },
                                btConnected = phoneLinked,
                                online = online,
                                androidAutoInstalled = projectionPkg != null,
                                onToggleEnabled = { on -> scope.launch { repo.setSmartStartEnabled(on) } },
                                onToggleMusic = { on -> scope.launch { repo.setSmartStartMusic(on) } },
                                onToggleNav = { on -> scope.launch { repo.setSmartStartNav(on) } },
                                onToggleAndroidAuto = { on -> scope.launch { repo.setSmartStartAndroidAuto(on) } },
                                onPickOnline = { picker = PickerTarget.ONLINE_MUSIC },
                                onPickOffline = { picker = PickerTarget.OFFLINE_MUSIC },
                                onRunNow = { runSmartStart() },
                                onBack = { screen = Screen.SETTINGS },
                            )
                        }
                    }
                    if (screen != Screen.OVERVIEW && screen != Screen.REAR_CAM) BottomBar(
                        current = screen,
                        onHome = { screen = Screen.DASHBOARD },
                        onVehicle = { overviewAuto = false; screen = Screen.OVERVIEW },
                        onNav = { openOrPick(s.navApp, PickerTarget.NAV) },
                        onRadio = { openRadio() },
                        onMusic = { screen = Screen.MUSIC },
                        onPhone = { openOrPick(s.phoneApp, PickerTarget.PHONE) },
                        onApps = { screen = Screen.APPS },
                        onClimate = { climateAuto = false; screen = Screen.CLIMATE },
                        onVoice = { startOfflineVoice() },
                    )
                }
            }

            // blocco tocchi in marcia (sopra a tutto)
            val lockedUntil by driveApp.driveLock.lockedUntil.collectAsStateWithLifecycle()
            if (lockedUntil > 0L) it.driveos.launcher.ui.DriveLockOverlay(lockedUntil)

            picker?.let { target ->
                AppPickerDialog(
                    title = target.title,
                    apps = apps,
                    selected = when (target) {
                        PickerTarget.NAV -> s.navApp
                        PickerTarget.MUSIC -> s.musicApp
                        PickerTarget.PHONE -> s.phoneApp
                        PickerTarget.RADIO -> s.radioApp
                        PickerTarget.ONLINE_MUSIC -> s.onlineMusicApp
                        PickerTarget.OFFLINE_MUSIC -> s.offlineMusicApp
                    },
                    onPick = { entry ->
                        scope.launch {
                            when (target) {
                                PickerTarget.NAV -> repo.setNavApp(entry?.key)
                                PickerTarget.MUSIC -> repo.setMusicApp(entry?.key)
                                PickerTarget.PHONE -> repo.setPhoneApp(entry?.key)
                                PickerTarget.RADIO -> repo.setRadioApp(entry?.key)
                                PickerTarget.ONLINE_MUSIC -> repo.setOnlineMusicApp(entry?.key)
                                PickerTarget.OFFLINE_MUSIC -> repo.setOfflineMusicApp(entry?.key)
                            }
                        }
                        picker = null
                    },
                    onDismiss = { picker = null },
                )
            }

            when (textDialog) {
                TextTarget.SEARCH -> TextInputDialog(
                    title = tr("Dove andiamo?"),
                    initial = "",
                    placeholder = tr("Indirizzo o luogo"),
                    confirmLabel = tr("Vai"),
                    onConfirm = { value -> textDialog = null; if (value.isNotBlank()) navigateTo(value) },
                    onDismiss = { textDialog = null },
                )

                TextTarget.HOME_ADDRESS -> TextInputDialog(
                    title = tr("Indirizzo di casa"),
                    initial = s.homeAddress,
                    placeholder = tr("Via, numero civico, città"),
                    confirmLabel = tr("Salva"),
                    onConfirm = { value ->
                        scope.launch { repo.setHomeAddress(value) }
                        textDialog = null
                    },
                    onDismiss = { textDialog = null },
                )

                TextTarget.WORK_ADDRESS -> TextInputDialog(
                    title = tr("Indirizzo di lavoro"),
                    initial = s.workAddress,
                    placeholder = tr("Via, numero civico, città"),
                    confirmLabel = tr("Salva"),
                    onConfirm = { value ->
                        scope.launch { repo.setWorkAddress(value) }
                        textDialog = null
                    },
                    onDismiss = { textDialog = null },
                )

                TextTarget.LOCATION -> TextInputDialog(
                    title = tr("Posizione (latitudine, longitudine)"),
                    initial = if (s.latitude != 0.0 || s.longitude != 0.0) "${s.latitude}, ${s.longitude}" else "",
                    placeholder = tr("es. 45.07, 7.69"),
                    confirmLabel = tr("Salva"),
                    onConfirm = { value ->
                        val parts = value.split(",", ";").mapNotNull { it.trim().toDoubleOrNull() }
                        if (parts.size == 2 && parts[0] in -90.0..90.0 && parts[1] in -180.0..180.0) {
                            scope.launch { repo.setLocation(parts[0], parts[1]) }
                            textDialog = null
                        } else {
                            message(tr("Formato: latitudine, longitudine (es. 45.07, 7.69)"))
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
                            message(tr("Frequenza non valida (87.5 – 108.0 MHz)."))
                        } else {
                            val label = name.ifBlank { "FM ${RadioPreset(khz, "").displayFrequency()}" }
                            if (RadioPresetLogic.isFull(s.radioPresets, RadioPreset(khz, label, RadioBand.FM))) {
                                message(tr("Hai già 6 stazioni: rimuovine una."))
                                radioDialog = false
                                return@RadioPresetDialog
                            }
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

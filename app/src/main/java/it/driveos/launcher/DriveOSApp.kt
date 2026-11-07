package it.driveos.launcher

import it.driveos.launcher.system.WakeGuard
import android.app.Application
import it.driveos.launcher.apps.AppRepository
import it.driveos.launcher.automation.BluetoothWatcher
import it.driveos.launcher.automation.ConnectivityWatcher
import it.driveos.launcher.calendar.CalendarRepository
import it.driveos.launcher.controls.SystemControls
import it.driveos.launcher.media.MediaRepository
import it.driveos.launcher.radio.RadioController
import it.driveos.launcher.settings.SettingsRepository
import it.driveos.launcher.system.CrashLog
import it.driveos.launcher.vehicle.CanSerialReader
import it.driveos.launcher.vehicle.KeyBindingCodec
import it.driveos.launcher.vehicle.KeyMapper
import it.driveos.launcher.vehicle.WindowWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import it.driveos.launcher.vehicle.LogcatReader
import it.driveos.launcher.vehicle.SpeedWatcher
import it.driveos.launcher.vehicle.VehicleRepository

/** Contenitore delle dipendenze condivise (nessun framework DI: non serve a questa scala). */
class DriveOSApp : Application() {
    companion object {
        /** DriveOS riportata davanti per i sensori: la Home apre la schermata Veicolo. */
        @Volatile var openOverview = false
    }

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var appRepository: AppRepository
        private set
    lateinit var mediaRepository: MediaRepository
        private set
    lateinit var calendarRepository: CalendarRepository
        private set
    lateinit var vehicleRepository: VehicleRepository
        private set
    lateinit var systemControls: SystemControls
    lateinit var tsBridge: it.driveos.launcher.junsun.TsBridge
        private set
    lateinit var bluetoothWatcher: BluetoothWatcher
        private set
    lateinit var connectivityWatcher: ConnectivityWatcher
        private set
    lateinit var radioController: RadioController
        private set
    lateinit var logcatReader: LogcatReader
        private set
    lateinit var speedWatcher: SpeedWatcher
        private set
    lateinit var canReader: CanSerialReader
        private set
    val driveLock = it.driveos.launcher.system.DriveLock()
    private val wakeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeJob: kotlinx.coroutines.Job? = null
    lateinit var voice: it.driveos.launcher.voice.OfflineVoice
        private set
    lateinit var keyMapper: KeyMapper
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        settingsRepository = SettingsRepository(this)
        it.driveos.launcher.i18n.I18n.load(this)
        it.driveos.launcher.i18n.I18n.lang = runCatching { kotlinx.coroutines.runBlocking { settingsRepository.settings.first().voiceLang } }.getOrDefault("it")
        appRepository = AppRepository(this)
        mediaRepository = MediaRepository(this)
        calendarRepository = CalendarRepository(this)
        vehicleRepository = VehicleRepository(this)
        tsBridge = it.driveos.launcher.junsun.TsBridge(this) // si collega dopo il cancello di accensione (vedi wake)
        systemControls = SystemControls(this).also { it.bridge = tsBridge }
        bluetoothWatcher = BluetoothWatcher(this)
        connectivityWatcher = ConnectivityWatcher(this)
        radioController = RadioController(this)
        logcatReader = LogcatReader(this, vehicleRepository)
        speedWatcher = SpeedWatcher(this)
        voice = it.driveos.launcher.voice.OfflineVoice(this)
        // far partire la musica spegne la radio Junsun (altrimenti tiene l'audio e mette in pausa i brani)
        mediaRepository.beforePlay = { tsBridge.radioCall { r -> r.CloseRadioCh() } }
        keyMapper = KeyMapper(this).also { it.bridge = tsBridge }
        canReader = CanSerialReader().also { r ->
            val pm = getSystemService(android.os.PowerManager::class.java)
            r.screenOn = { pm?.isInteractive != false }
            r.addSink { vehicleRepository.ingest(it) }
            r.addSink { keyMapper.onChanges(it) }
        }
        // A quadro spento lo schermo si spegne: DriveOS smette di scrivere alla centralina CAN e di
        // chiedere dati alla radio, cosi' radio e auto possono andare in standby normalmente.
        runCatching {
            registerReceiver(object : android.content.BroadcastReceiver() {
                override fun onReceive(c: android.content.Context, i: android.content.Intent) {
                    when (i.action) {
                        android.content.Intent.ACTION_SCREEN_OFF, "com.cusc.action.ACC_OFF", "com.ts.main.uiaccoff",
                        "autochips.intent.action.QB_POWEROFF" -> {
                            // quadro spento: si stacca TUTTO dalla radio Junsun
                            it.driveos.launcher.system.PowerGate.screenOn = false
                            wakeJob?.cancel(); canReader.pause(); voice.stop(); mediaRepository.pauseForPower()
                            logcatReader.stop(); speedWatcher.stop(); tsBridge.disconnect()
                        }
                        android.content.Intent.ACTION_SCREEN_ON, "com.ts.main.uiaccon", "autochips.intent.action.QB_POWERON" -> {
                            wakeJob?.cancel()
                            it.driveos.launcher.system.PowerGate.screenOn = true
                            it.driveos.launcher.system.PowerGate.notBefore = System.currentTimeMillis() + 20_000
                            wakeJob = wakeScope.launch {
                                // lascia alla radio Junsun il tempo di riagganciare l'MCU, poi riprende la seriale
                                canReader.notBefore = System.currentTimeMillis() + 20_000
                                kotlinx.coroutines.delay(8_000)
                                mediaRepository.resumeAfterPower()
                                kotlinx.coroutines.delay(12_100)
                                reconnectAll()
                            }
                            // al risveglio la radio rimette davanti la sua app: si torna sulla Home DriveOS
                            scheduleWakeReturn()
                        }
                    }
                }
            }, android.content.IntentFilter().apply {
                addAction(android.content.Intent.ACTION_SCREEN_OFF); addAction(android.content.Intent.ACTION_SCREEN_ON)
                // segnali della radio Junsun: quadro spento / riacceso (arrivano prima dello schermo)
                addAction("com.cusc.action.ACC_OFF"); addAction("com.ts.main.uiaccoff"); addAction("com.ts.main.uiaccon")
                addAction("autochips.intent.action.QB_POWEROFF"); addAction("autochips.intent.action.QB_POWERON")
            })
        }
        it.driveos.launcher.system.PowerGate.screenOn = getSystemService(android.os.PowerManager::class.java)?.isInteractive != false
        // processo appena partito (avvio radio, o processo chiuso durante la sospensione): stesso controllo
        scheduleWakeReturn()
        wakeScope.launch {
            // avvio (anche dopo che la radio ha chiuso le app): prima si lascia finire la radio
            while (!it.driveos.launcher.system.PowerGate.ready()) kotlinx.coroutines.delay(1000)
            reconnectAll()
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch { settingsRepository.settings.collect { keyMapper.bindings = it.keyBindings; WakeGuard.keepHome = it.keepHome } }
        scope.launch {
            logcatReader.names.collect { m ->
                val cur = settingsRepository.settings.first().radioPresets
                val upd = cur.map { p ->
                    val n = m[p.khz]
                    if (n != null && p.khz > 0 && n != p.name && (logcatReader.readingNames || p.name.isBlank() || p.name.startsWith("FM "))) p.copy(name = n) else p
                }
                if (upd != cur) settingsRepository.setRadioPresets(upd)
            }
        }
        // Retromarcia/sensori e AutoLink controllati qui, a livello di app: quando davanti c'e' un'altra app
        // (es. AutoLink) la Home DriveOS e' ferma e non si accorgerebbe della retro.
        scope.launch {
            var wasRev = false; var wasSens = false; var wasProj = false
            vehicleRepository.signals.collect { sig ->
                val rev = it.driveos.launcher.vehicle.KnownVehicleSignals.reverseOn(sig) == true
                val sens = it.driveos.launcher.vehicle.CanDecoder.sensorLevels(sig, it.driveos.launcher.vehicle.CanDecoder.REAR_SENSORS)?.any { l -> l > 0 } == true
                val proj = it.driveos.launcher.vehicle.KnownVehicleSignals.projectionActive(sig)
                if (rev != wasRev) android.util.Log.i("DriveOS", "retromarcia=$rev davanti=${WakeGuard.homeInFront} schermo=${it.driveos.launcher.system.PowerGate.screenOn} proiezione=$proj")
                if ((rev && !wasRev) || (sens && !wasSens)) {
                    val keep = runCatching { settingsRepository.settings.first().keepHome }.getOrDefault(true)
                    if (keep && !WakeGuard.homeInFront && it.driveos.launcher.system.PowerGate.screenOn) {
                        if (!rev) openOverview = true
                        bringMainToFront()
                    }
                }
                // AutoLink appena collegato: porta il suo audio, il lettore interno si ferma
                if (proj && !wasProj) mediaRepository.pauseLocalForOther()
                wasRev = rev; wasSens = sens; wasProj = proj
            }
        }
        watchOtherPlayers()
        keyMapper.onLearned = { b ->
            scope.launch {
                val cur = settingsRepository.settings.first().keyBindings
                settingsRepository.setKeyBindings(KeyBindingCodec.set(cur, b))
            }
        }
    }

    /** Apre/porta davanti la Home DriveOS da qualunque app (anche AutoLink). */
    fun bringMainToFront() {
        // Android 10 blocca in silenzio le app in secondo piano che aprono schermate, salvo eccezioni:
        // il servizio di Accessibilita' (legato dal sistema) e' una di queste, quindi si parte da li'.
        val ctx: android.content.Context = WindowWatcher.service ?: this
        val i = android.content.Intent(this, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
        android.util.Log.i("DriveOS", "retro/sensori: porto davanti DriveOS (via ${if (ctx === this) "app" else "accessibilita'"})")
        runCatching { ctx.startActivity(i) }.onFailure { e -> android.util.Log.w("DriveOS", "apertura bloccata: $e"); WindowWatcher.goHome() }
        // seconda prova: se dopo 1,5 s la Home non e' ancora davanti
        wakeScope.launch {
            kotlinx.coroutines.delay(1500)
            if (!WakeGuard.homeInFront) {
                android.util.Log.w("DriveOS", "DriveOS non e' davanti dopo 1,5 s: riprovo")
                runCatching { (WindowWatcher.service ?: this@DriveOSApp).startActivity(i) }
            }
        }
    }

    /**
     * Se mentre suona il lettore interno parte l'audio di un'altra app (AutoLink, telefono, Spotify…),
     * il lettore interno si mette in pausa. Il lettore DriveOS non usa il "focus audio" (lo perdeva da solo),
     * quindi si guardano i lettori audio attivi del sistema: piu' di uno = qualcun altro sta suonando.
     */
    private fun watchOtherPlayers() {
        val am = getSystemService(android.media.AudioManager::class.java) ?: return
        runCatching {
            am.registerAudioPlaybackCallback(object : android.media.AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: MutableList<android.media.AudioPlaybackConfiguration>) {
                    if (!mediaRepository.isLocalPlaying()) return
                    val media = configs.count { c ->
                        val u = c.audioAttributes.usage
                        u == android.media.AudioAttributes.USAGE_MEDIA || u == android.media.AudioAttributes.USAGE_GAME ||
                            u == android.media.AudioAttributes.USAGE_UNKNOWN
                    }
                    if (media >= 2) mediaRepository.pauseLocalForOther()
                }
            }, android.os.Handler(android.os.Looper.getMainLooper()))
        }
    }

    private var wakeReturnJob: kotlinx.coroutines.Job? = null
    @Volatile private var setupTried = false

    /**
     * Dopo l'accensione la radio può rimettere davanti la sua app anche più volte e con ritardo
     * (riaggancio MCU). Si controlla a più riprese per [WakeGuard.WINDOW_MS]; si smette appena tocchi
     * DriveOS. Le finestre che compaiono tra un controllo e l'altro le copre il servizio di Accessibilità.
     */
    fun scheduleWakeReturn() {
        WakeGuard.arm(System.currentTimeMillis())
        wakeReturnJob?.cancel()
        wakeReturnJob = wakeScope.launch {
            for (wait in longArrayOf(3_000, 3_000, 3_000, 4_000, 5_000, 6_000)) {
                kotlinx.coroutines.delay(wait)
                val now = System.currentTimeMillis()
                if (!WakeGuard.active(now)) break
                // senza Accessibilita' non si vede quale app e' davanti: se la Home DriveOS non e' visibile
                // si assume la radio Junsun (e' lei che il firmware rimette davanti al risveglio)
                val fg = it.driveos.launcher.vehicle.WindowWatcher.currentPackage()
                    ?: if (!it.driveos.launcher.vehicle.WindowWatcher.enabled()) "com.ts.MainUI" else null
                val reverse = it.driveos.launcher.vehicle.KnownVehicleSignals.reverseOn(vehicleRepository.signals.value) == true
                if (WakeGuard.shouldBounce(fg, null, now, WakeGuard.homeInFront, reverse)) {
                    WakeGuard.bounced()
                    if (!it.driveos.launcher.vehicle.WindowWatcher.goHome()) runCatching {
                        startActivity(android.content.Intent(this@DriveOSApp, MainActivity::class.java)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    }
                }
            }
        }
    }

    /** Dopo il cancello di accensione: riaggancia servizi Junsun, log, GPS e seriale. */
    private fun reconnectAll() {
        if (!it.driveos.launcher.system.PowerGate.ready()) return
        // dopo un reset di Android il permesso ai log sparisce: DriveOS se lo ridà da solo (una volta per avvio)
        if (!setupTried && !it.driveos.launcher.system.SelfSetup.logsGranted(this)) {
            setupTried = true
            Thread { it.driveos.launcher.system.SelfSetup.run(this) }.start()
        }
        tsBridge.connect()
        logcatReader.start()
        speedWatcher.start()
        canReader.resumeIfPaused()
        mediaRepository.resumeAfterPower()
        // Al risveglio la radio Junsun riaccende da sola il sintonizzatore: se intanto e' ripartita la
        // musica di DriveOS si sentirebbero tutte e due. Si chiude la radio (anche se riparte in ritardo).
        wakeScope.launch {
            for (wait in longArrayOf(1_500, 3_000, 5_000, 10_000)) {
                kotlinx.coroutines.delay(wait)
                if (!it.driveos.launcher.system.PowerGate.ready()) return@launch
                if (mediaRepository.media.value?.isPlaying != true) continue
                if (runCatching { tsBridge.workMode() }.getOrNull() == 1) tsBridge.radioNow { r -> r.CloseRadioCh() }
            }
        }
    }
}

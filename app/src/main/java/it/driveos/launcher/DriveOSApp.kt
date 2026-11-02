package it.driveos.launcher

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
                            wakeScope.launch {
                                kotlinx.coroutines.delay(6_000)
                                val keep = runCatching { settingsRepository.settings.first().keepHome }.getOrDefault(true)
                                if (keep && it.driveos.launcher.vehicle.WindowWatcher.currentPackage() == "com.ts.MainUI") {
                                    runCatching {
                                        startActivity(android.content.Intent(this@DriveOSApp, MainActivity::class.java)
                                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                                    }
                                }
                            }
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
        wakeScope.launch {
            // avvio (anche dopo che la radio ha chiuso le app): prima si lascia finire la radio
            while (!it.driveos.launcher.system.PowerGate.ready()) kotlinx.coroutines.delay(1000)
            reconnectAll()
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch { settingsRepository.settings.collect { keyMapper.bindings = it.keyBindings } }
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
        keyMapper.onLearned = { b ->
            scope.launch {
                val cur = settingsRepository.settings.first().keyBindings
                settingsRepository.setKeyBindings(KeyBindingCodec.set(cur, b))
            }
        }
    }

    /** Dopo il cancello di accensione: riaggancia servizi Junsun, log, GPS e seriale. */
    private fun reconnectAll() {
        if (!it.driveos.launcher.system.PowerGate.ready()) return
        tsBridge.connect()
        logcatReader.start()
        speedWatcher.start()
        canReader.resumeIfPaused()
        mediaRepository.resumeAfterPower()
    }
}

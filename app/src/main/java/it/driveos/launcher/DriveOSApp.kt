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
    lateinit var keyMapper: KeyMapper
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        settingsRepository = SettingsRepository(this)
        appRepository = AppRepository(this)
        mediaRepository = MediaRepository(this)
        calendarRepository = CalendarRepository(this)
        vehicleRepository = VehicleRepository(this)
        tsBridge = it.driveos.launcher.junsun.TsBridge(this).also { it.connect() }
        systemControls = SystemControls(this).also { it.bridge = tsBridge }
        bluetoothWatcher = BluetoothWatcher(this)
        connectivityWatcher = ConnectivityWatcher(this)
        radioController = RadioController(this)
        logcatReader = LogcatReader(this, vehicleRepository)
        speedWatcher = SpeedWatcher(this)
        keyMapper = KeyMapper(this).also { it.bridge = tsBridge }
        canReader = CanSerialReader().also { r ->
            r.addSink { vehicleRepository.ingest(it) }
            r.addSink { keyMapper.onChanges(it) }
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
}

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
        private set
    lateinit var bluetoothWatcher: BluetoothWatcher
        private set
    lateinit var connectivityWatcher: ConnectivityWatcher
        private set
    lateinit var radioController: RadioController
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        settingsRepository = SettingsRepository(this)
        appRepository = AppRepository(this)
        mediaRepository = MediaRepository(this)
        calendarRepository = CalendarRepository(this)
        vehicleRepository = VehicleRepository(this)
        systemControls = SystemControls(this)
        bluetoothWatcher = BluetoothWatcher(this)
        connectivityWatcher = ConnectivityWatcher(this)
        radioController = RadioController(this)
    }
}

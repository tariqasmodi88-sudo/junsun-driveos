package it.driveos.launcher

import android.app.Application
import it.driveos.launcher.apps.AppRepository
import it.driveos.launcher.settings.SettingsRepository

/** Contenitore delle dipendenze condivise (nessun framework DI: non serve a questa scala). */
class DriveOSApp : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var appRepository: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        appRepository = AppRepository(this)
    }
}

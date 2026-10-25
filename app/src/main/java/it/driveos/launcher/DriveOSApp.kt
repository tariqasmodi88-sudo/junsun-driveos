package it.driveos.launcher

import android.app.Application
import it.driveos.launcher.apps.AppRepository
import it.driveos.launcher.calendar.CalendarRepository
import it.driveos.launcher.media.MediaRepository
import it.driveos.launcher.settings.SettingsRepository

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

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        appRepository = AppRepository(this)
        mediaRepository = MediaRepository(this)
        calendarRepository = CalendarRepository(this)
    }
}

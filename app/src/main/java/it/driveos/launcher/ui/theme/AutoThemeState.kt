package it.driveos.launcher.ui.theme

import android.content.Context
import android.content.res.Configuration
import it.driveos.launcher.settings.DriveSettings
import it.driveos.launcher.vehicle.LightsResolver
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Calcola, in modalita' AUTO, se usare il tema scuro, e da dove viene la decisione. */
object AutoThemeState {

    // Roma, usata solo se l'utente non ha impostato una posizione.
    private const val DEFAULT_LAT = 41.9
    private const val DEFAULT_LON = 12.5

    data class Result(val dark: Boolean, val source: AutoSource)

    fun compute(context: Context, settings: DriveSettings, signals: Map<String, String>, now: ZonedDateTime = ZonedDateTime.now()): Result {
        val lightsBinding = settings.lightsBinding
        val lightsValue = lightsBinding?.let { signals[it.id] }
        val lightsOn = LightsResolver.resolve(lightsBinding, lightsValue)

        val systemNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        val lat = if (settings.latitude != 0.0 || settings.longitude != 0.0) settings.latitude else DEFAULT_LAT
        val lon = if (settings.latitude != 0.0 || settings.longitude != 0.0) settings.longitude else DEFAULT_LON
        val utc = now.withZoneSameInstant(ZoneId.of("UTC"))
        val utcMin = utc.toLocalTime().toSecondOfDay() / 60.0
        val sunNight = SunTimes.isNight(utc.toLocalDate(), utcMin, lat, lon)

        val (dark, source) = AutoThemeResolver.resolve(lightsOn, systemNight, sunNight)
        return Result(dark, source)
    }

    @Suppress("unused")
    private fun midday() = LocalTime.NOON to LocalDate.now()
}

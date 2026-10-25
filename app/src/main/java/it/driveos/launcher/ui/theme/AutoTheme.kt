package it.driveos.launcher.ui.theme

import java.time.LocalDate
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Alba e tramonto (algoritmo NOAA semplificato, precisione di qualche minuto).
 * Restituisce i minuti dalla mezzanotte UTC, oppure null nei giorni senza alba/tramonto.
 */
object SunTimes {
    data class Times(val sunriseUtcMin: Double, val sunsetUtcMin: Double)

    fun compute(date: LocalDate, lat: Double, lon: Double): Times? {
        val n = date.dayOfYear
        val gamma = 2 * Math.PI / 365.0 * (n - 1)
        val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
            0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) +
            0.000907 * sin(2 * gamma) - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        val latR = Math.toRadians(lat)
        val zenith = Math.toRadians(90.833)
        val cosHa = cos(zenith) / (cos(latR) * cos(decl)) - tan(latR) * tan(decl)
        if (cosHa < -1 || cosHa > 1) return null
        val ha = Math.toDegrees(acos(cosHa))
        val sunrise = 720 - 4 * (lon + ha) - eqTime
        val sunset = 720 - 4 * (lon - ha) - eqTime
        return Times(sunrise, sunset)
    }

    /** Notte = prima dell'alba o dopo il tramonto (minuti UTC del giorno). */
    fun isNight(date: LocalDate, utcMinuteOfDay: Double, lat: Double, lon: Double): Boolean {
        val t = compute(date, lat, lon) ?: return false
        return utcMinuteOfDay < t.sunriseUtcMin || utcMinuteOfDay > t.sunsetUtcMin
    }
}

/** Da dove viene la decisione chiaro/scuro in modalita' automatica (mostrato in Impostazioni). */
enum class AutoSource(val label: String) {
    LIGHTS("fari (segnale CAN collegato)"),
    SYSTEM("modalità notte di Android"),
    SUN("orario di alba e tramonto"),
}

object AutoThemeResolver {
    /**
     * Priorita': 1) stato dei fari se il segnale e' collegato e leggibile;
     * 2) modalita' notte del sistema (alcuni firmware la attivano con il filo ILL);
     * 3) alba/tramonto.
     */
    fun resolve(lightsOn: Boolean?, systemNight: Boolean, sunNight: Boolean): Pair<Boolean, AutoSource> = when {
        lightsOn != null -> lightsOn to AutoSource.LIGHTS
        systemNight -> true to AutoSource.SYSTEM
        else -> sunNight to AutoSource.SUN
    }
}

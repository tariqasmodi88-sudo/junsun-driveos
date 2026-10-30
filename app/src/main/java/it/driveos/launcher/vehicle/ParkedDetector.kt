package it.driveos.launcher.vehicle

/**
 * Decide se l'auto è FERMA o IN MARCIA dalla velocità GPS, con isteresi:
 *  - diventa FERMA solo dopo [stopSeconds] secondi consecutivi sotto [stopKmh]
 *    (così non scatta a ogni rallentamento);
 *  - torna IN MARCIA appena supera [moveKmh] (soglia più alta: niente sfarfallio).
 * Logica pura, testabile.
 */
class ParkedDetector(
    private val stopKmh: Double = 2.0,
    private val moveKmh: Double = 6.0,
    private val stopSeconds: Long = 4,
) {
    var parked: Boolean = false
        private set
    private var slowSinceMs: Long? = null

    /** Aggiorna con un nuovo campione. Restituisce true se lo stato è cambiato. */
    fun update(speedKmh: Double, nowMs: Long): Boolean {
        val before = parked
        if (speedKmh > moveKmh) {
            parked = false
            slowSinceMs = null
        } else if (speedKmh < stopKmh) {
            val since = slowSinceMs ?: nowMs.also { slowSinceMs = it }
            if (nowMs - since >= stopSeconds * 1000) parked = true
        } else {
            // zona intermedia: non cambia stato, azzera solo il conteggio "fermo"
            slowSinceMs = null
        }
        return parked != before
    }
}

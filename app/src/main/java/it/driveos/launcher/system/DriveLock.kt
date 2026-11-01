package it.driveos.launcher.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Limite di interazione in marcia, come lo "speed bump" di Android Auto / Android Automotive:
 * una riserva di tocchi ([MAX_PERMITS]) che si consuma a ogni tocco e si ricarica da sola
 * ([PERMITS_PER_SECOND] al secondo, dopo [FILL_DELAY_MS] senza toccare). Se la riserva finisce
 * mentre l'auto si muove, lo schermo si blocca per [LOCK_SECONDS] secondi con l'avviso
 * "Concentrati sulla strada". Da fermi (o senza velocità nota) nessun limite.
 */
class DriveLock(private val now: () -> Long = { android.os.SystemClock.elapsedRealtime() }) {
    private var permits = MAX_PERMITS
    private var lastTouch = 0L
    private var lastRefill = 0L
    private val _lockedUntil = MutableStateFlow(0L)
    /** Istante (elapsedRealtime) fino a cui lo schermo e' bloccato; 0 = libero. */
    val lockedUntil: StateFlow<Long> = _lockedUntil
    @Volatile var enabled = true
    @Volatile var moving = false

    fun isLocked(t: Long = now()) = t < _lockedUntil.value

    private fun refill(t: Long) {
        val from = maxOf(lastRefill, lastTouch + FILL_DELAY_MS)
        if (t > from) {
            permits = minOf(MAX_PERMITS, permits + (t - from) / 1000.0 * PERMITS_PER_SECOND)
            lastRefill = t
        }
    }

    /** Un tocco sullo schermo (dito appoggiato). true = lascialo passare, false = bloccato. */
    @Synchronized fun onTouch(): Boolean {
        val t = now()
        if (isLocked(t)) return false
        if (!enabled || !moving) { permits = MAX_PERMITS; return true }
        refill(t)
        lastTouch = t; lastRefill = t
        if (permits >= 1.0) { permits -= 1.0; return true }
        _lockedUntil.value = t + LOCK_SECONDS * 1000L
        permits = MAX_PERMITS
        return false
    }

    /** Fermi: il blocco si toglie subito. */
    fun onStopped() { moving = false; _lockedUntil.value = 0L; permits = MAX_PERMITS }

    companion object {
        const val MAX_PERMITS = 6.0          // come i "6 passi" di Android Auto
        const val PERMITS_PER_SECOND = 0.5
        const val FILL_DELAY_MS = 600L
        const val LOCK_SECONDS = 10
        const val MOVING_KMH = 8.0
    }
}

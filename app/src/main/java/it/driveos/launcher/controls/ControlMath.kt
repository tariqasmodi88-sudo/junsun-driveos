package it.driveos.launcher.controls

/** Conversioni per i cursori di volume e luminosita' (logica pura, testabile). */
object ControlMath {
    /** Indice volume (0..max) → percentuale 0..100. */
    fun toPercent(index: Int, max: Int): Int =
        if (max <= 0) 0 else Math.round(index * 100.0 / max).toInt().coerceIn(0, 100)

    /** Percentuale 0..100 → indice volume (0..max). */
    fun fromPercent(percent: Int, max: Int): Int =
        if (max <= 0) 0 else Math.round(percent.coerceIn(0, 100) / 100.0 * max).toInt().coerceIn(0, max)

    /** Passo successivo/precedente del volume, senza uscire dai limiti. */
    fun step(index: Int, delta: Int, max: Int): Int = (index + delta).coerceIn(0, max)

    /** Luminosita' di sistema (0..255) → frazione 0f..1f per la finestra dell'app. */
    fun brightnessFraction(system0to255: Int): Float = (system0to255.coerceIn(0, 255)) / 255f

    /** Frazione 0f..1f → valore di sistema 0..255 (minimo 1 per non spegnere lo schermo). */
    fun toSystemBrightness(fraction: Float): Int =
        Math.round(fraction.coerceIn(0f, 1f) * 255).toInt().coerceIn(1, 255)
}

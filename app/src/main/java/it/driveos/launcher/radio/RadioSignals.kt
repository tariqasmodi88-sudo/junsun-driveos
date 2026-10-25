package it.driveos.launcher.radio

/**
 * Lettura dello stato della radio dai broadcast che il firmware può pubblicare.
 * Diversi firmware MTK/FYT mandano la frequenza come intero in kHz (es. 101500),
 * in "decihertz di MHz" (1015 = 101.5) o come stringa "101.5". Qui la normalizziamo
 * in kHz. Se non riusciamo a interpretare un valore, lo ignoriamo (niente dati finti).
 */
object RadioSignals {

    /** Nomi di extra che, nei vari firmware, contengono la frequenza. */
    val frequencyExtraHints = listOf(
        "freq", "frequency", "frequence", "curfreq", "cur_freq", "current_freq",
        "fm_freq", "radio_freq", "khz", "mhz", "channel_freq",
    )

    /** Nomi di extra che indicano se la radio sta suonando. */
    val playingExtraHints = listOf("play", "playing", "is_play", "state", "status", "power", "on")

    /**
     * Interpreta un valore grezzo come frequenza FM in kHz, se plausibile (87.5–108 MHz).
     * Accetta: 101500 (kHz), 101.5 / "101,5" (MHz), 1015 (MHz*10), 10150 (MHz*100).
     */
    fun parseFrequencyKhz(raw: String): Int? {
        val t = raw.trim().replace(',', '.')
        val d = t.toDoubleOrNull() ?: return null
        val candidates = when {
            d >= 87_000 && d <= 108_000 -> d                 // già kHz
            d >= 87.0 && d <= 108.0 -> d * 1000               // MHz
            d >= 875 && d <= 1080 -> d * 100                  // MHz*10 (1015 → 101500)
            d >= 8750 && d <= 10800 -> d * 10                 // MHz*100 (10150 → 101500)
            else -> return null
        }
        val khz = Math.round(candidates).toInt()
        val snapped = Math.round(khz / 100.0).toInt() * 100
        return if (snapped in 87_500..108_000) snapped else null
    }

    /** true/false/null dal valore grezzo di uno "stato di riproduzione". */
    fun parsePlaying(raw: String): Boolean? = when (raw.trim().lowercase()) {
        "1", "true", "play", "playing", "on", "start", "started", "resumed" -> true
        "0", "false", "pause", "paused", "stop", "stopped", "off", "idle" -> false
        else -> null
    }

    fun looksLikeFrequencyExtra(name: String): Boolean {
        val n = name.lowercase()
        return frequencyExtraHints.any { n.contains(it) }
    }

    fun looksLikePlayingExtra(name: String): Boolean {
        val n = name.lowercase()
        return playingExtraHints.any { n == it || n.endsWith("_$it") || n.contains(it) }
    }
}

/** Stato corrente della radio integrata, per quello che il firmware ci lascia leggere. */
data class RadioState(
    val khz: Int? = null,
    val playing: Boolean? = null,
    val band: RadioBand = RadioBand.FM,
    val source: String = "", // azione broadcast da cui arriva (diagnostica)
) {
    val hasFrequency get() = khz != null
}

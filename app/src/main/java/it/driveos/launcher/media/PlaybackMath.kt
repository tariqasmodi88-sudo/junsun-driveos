package it.driveos.launcher.media

/** Calcoli sulla posizione di riproduzione, indipendenti da Android. */
object PlaybackMath {
    /**
     * Posizione stimata ora, partendo dall'ultima posizione riportata dalla sessione.
     * [durationMs] <= 0 significa durata sconosciuta (es. radio in streaming).
     */
    fun currentPosition(
        positionMs: Long,
        updateTimeMs: Long,
        nowMs: Long,
        speed: Float,
        playing: Boolean,
        durationMs: Long,
    ): Long {
        val elapsed = if (playing) ((nowMs - updateTimeMs).coerceAtLeast(0) * speed).toLong() else 0L
        val pos = (positionMs + elapsed).coerceAtLeast(0)
        return if (durationMs > 0) pos.coerceAtMost(durationMs) else pos
    }

    fun progress(positionMs: Long, durationMs: Long): Float =
        if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    fun format(ms: Long): String {
        val total = (ms.coerceAtLeast(0) / 1000)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}

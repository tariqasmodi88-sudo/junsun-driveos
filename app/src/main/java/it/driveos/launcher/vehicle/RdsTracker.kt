package it.driveos.launcher.vehicle

/**
 * Stato del nome stazione (RDS PS) per la frequenza sintonizzata.
 *
 * Il sintonizzatore (st7708) e la radio Junsun assemblano gia' i blocchi da 2 caratteri del
 * gruppo RDS 0A/0B: nel registro arriva la stringa PS intera da 8 caratteri. Qui si fa il resto:
 *  - a ogni cambio frequenza lo stato riparte da SEARCHING (mai il nome della stazione precedente);
 *  - una PS e' accettata solo se valida (non vuota, non solo spazi/riempitivi, non "* RDS *")
 *    e se resta stabile per [debounceMs] (filtra frammenti e righe spurie);
 *  - PS dinamica (testo che scorre, es. "MONEL E" / "BELLLA E"): resta il primo nome valido;
 *  - se entro [timeoutMs] non arriva nulla: NO_RDS, si mostra solo la frequenza.
 */
class RdsTracker(
    private val now: () -> Long = { System.currentTimeMillis() },
    private val debounceMs: Long = 1200,
    private val timeoutMs: Long = 10_000,
) {
    enum class Status { IDLE, SEARCHING, OK, NO_RDS }
    data class State(val khz: Int? = null, val status: Status = Status.IDLE, val name: String? = null)

    var state = State(); private set

    private var tunedAt = 0L
    private var candidate: String? = null
    private var candidateAt = 0L
    private var dynamic = false

    /** Nuova frequenza (kHz). Reset immediato se diversa da quella attuale (o se [force]). */
    @Synchronized fun onTune(khz: Int, force: Boolean = false): Boolean {
        if (!force && khz == state.khz) return false
        state = State(khz, Status.SEARCHING, null)
        tunedAt = now(); candidate = null; candidateAt = 0L; dynamic = false
        return true
    }

    /** Riga PS dal registro. Ignorata se non c'e' una frequenza o se non valida. */
    @Synchronized fun onPs(raw: String) {
        if (state.khz == null) return
        val ps = clean(raw) ?: return
        when {
            candidate == null -> { candidate = ps; candidateAt = now() }
            candidate != ps -> dynamic = true // testo che scorre: si tiene il primo
        }
        if (state.status == Status.NO_RDS) state = state.copy(status = Status.SEARCHING)
    }

    /**
     * Da chiamare periodicamente. Restituisce (kHz, nome) quando un nome viene confermato,
     * cosi' chi lo usa puo' memorizzarlo.
     */
    @Synchronized fun tick(): Pair<Int, String>? {
        val khz = state.khz ?: return null
        if (state.status != Status.SEARCHING) return null
        val c = candidate
        val t = now()
        if (c != null && t - candidateAt >= debounceMs) {
            state = State(khz, Status.OK, c)
            return khz to c
        }
        if (c == null && t - tunedAt >= timeoutMs) state = State(khz, Status.NO_RDS, null)
        return null
    }

    val isDynamic: Boolean get() = dynamic

    companion object {
        /** PS valida o null: niente vuoti, solo spazi/riempitivi, segnaposto o caratteri di controllo. */
        fun clean(raw: String): String? {
            val s = raw.filter { it >= ' ' && it != '\u007f' }.replace(Regex("\\s+"), " ").trim()
            if (s.length < 2) return null
            if (s.contains("RDS", ignoreCase = true)) return null
            if (s.all { !it.isLetterOrDigit() }) return null
            if (s.toSet().size == 1) return null
            return s
        }
    }
}

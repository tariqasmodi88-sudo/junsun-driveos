package it.driveos.launcher.radio

/**
 * Preset radio salvato dall'utente: una frequenza FM (o DAB) con un nome.
 * DriveOS memorizza i canali in modo indipendente dall'app radio Junsun.
 *
 * Nota onesta sui limiti: cambiare davvero stazione richiede che l'app radio Junsun
 * accetti un "intent" con la frequenza. Se durante la scansione (schermata Veicolo)
 * si trova tale azione, DriveOS la usa per sintonizzarsi; altrimenti apre l'app radio
 * e la frequenza resta un promemoria. In nessun caso DriveOS mostra una frequenza finta.
 */
data class RadioPreset(
    val khz: Int,          // es. 101500 = 101.5 MHz; per DAB puo' essere un indice canale
    val name: String,
    val band: RadioBand = RadioBand.FM,
) {
    /** "101.5" per FM. */
    fun displayFrequency(): String = when (band) {
        RadioBand.FM -> {
            val mhz = khz / 1000.0
            if (mhz % 1.0 == 0.0) mhz.toInt().toString() else String.format("%.1f", mhz)
        }
        RadioBand.AM -> khz.toString()
        RadioBand.DAB -> name
    }

    fun displayUnit(): String = when (band) { RadioBand.FM -> "MHz"; RadioBand.AM -> "kHz"; RadioBand.DAB -> "" }
}

enum class RadioBand { FM, AM, DAB }

object RadioPresetLogic {
    const val FM_MIN_KHZ = 87_500
    const val FM_MAX_KHZ = 108_000
    const val FM_STEP_KHZ = 100 // 0,1 MHz
    /** Massimo di stazioni preferite. */
    const val MAX_PRESETS = 6

    /** Converte "101.5" o "101,5" in kHz; null se non valido. */
    fun parseFm(text: String): Int? {
        val mhz = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        val khz = Math.round(mhz * 1000).toInt()
        val snapped = Math.round(khz.toDouble() / FM_STEP_KHZ).toInt() * FM_STEP_KHZ
        return if (snapped in FM_MIN_KHZ..FM_MAX_KHZ) snapped else null
    }

    /** Posto vuoto: i preferiti hanno posti fissi P1…P6, un posto libero non fa slittare gli altri. */
    val EMPTY = RadioPreset(0, "")
    fun RadioPreset.isEmpty() = khz <= 0

    private fun trim(list: List<RadioPreset>): List<RadioPreset> = list.dropLastWhile { it.isEmpty() }.take(MAX_PRESETS)

    /** Stessa frequenza in due posti: resta solo il primo, l'altro diventa vuoto (nessuno slittamento). */
    fun normalize(list: List<RadioPreset>): List<RadioPreset> {
        val seen = HashSet<Pair<RadioBand, Int>>()
        return trim(list.map { p -> if (p.isEmpty() || seen.add(p.band to p.khz)) p else EMPTY })
    }

    /** Aggiunge nel primo posto libero (o aggiorna il nome se la frequenza c'è già). */
    fun add(list: List<RadioPreset>, preset: RadioPreset): List<RadioPreset> {
        val i = list.indexOfFirst { !it.isEmpty() && it.band == preset.band && it.khz == preset.khz }
        if (i >= 0) return normalize(list.toMutableList().also { it[i] = preset })
        val free = list.indexOfFirst { it.isEmpty() }
        if (free >= 0) return normalize(list.toMutableList().also { it[free] = preset })
        if (list.size >= MAX_PRESETS) return list // pieno
        return normalize(list + preset)
    }

    /** Mette [preset] nel posto [slot] (1-based). Se la frequenza era in un altro posto, quello si svuota. */
    fun setSlot(list: List<RadioPreset>, slot: Int, preset: RadioPreset): List<RadioPreset> {
        val idx = (slot - 1).coerceIn(0, MAX_PRESETS - 1)
        val out = list.map { if (!it.isEmpty() && it.band == preset.band && it.khz == preset.khz) EMPTY else it }.toMutableList()
        while (out.size <= idx) out.add(EMPTY)
        out[idx] = preset
        return trim(out)
    }

    fun isFull(list: List<RadioPreset>, preset: RadioPreset) =
        list.size >= MAX_PRESETS && list.none { it.isEmpty() } && list.none { it.band == preset.band && it.khz == preset.khz }

    fun remove(list: List<RadioPreset>, band: RadioBand, khz: Int): List<RadioPreset> =
        trim(list.map { if (it.band == band && it.khz == khz) EMPTY else it })
}

object RadioPresetCodec {
    private const val FIELD = '\u001F'
    private const val RECORD = '\u001E'

    fun encode(list: List<RadioPreset>): String =
        list.joinToString(RECORD.toString()) { "${it.khz}$FIELD${it.band.name}$FIELD${it.name}" }

    fun decode(raw: String?): List<RadioPreset> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(RECORD).mapNotNull { rec ->
            val f = rec.split(FIELD)
            if (f.size < 3) return@mapNotNull null
            val khz = f[0].toIntOrNull() ?: return@mapNotNull null
            val band = RadioBand.entries.firstOrNull { it.name == f[1] } ?: RadioBand.FM
            RadioPreset(khz, f[2], band)
        }
    }
}

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

    /** Converte "101.5" o "101,5" in kHz; null se non valido. */
    fun parseFm(text: String): Int? {
        val mhz = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        val khz = Math.round(mhz * 1000).toInt()
        val snapped = Math.round(khz.toDouble() / FM_STEP_KHZ).toInt() * FM_STEP_KHZ
        return if (snapped in FM_MIN_KHZ..FM_MAX_KHZ) snapped else null
    }

    /** Ordina per banda e frequenza; evita doppioni della stessa frequenza nella stessa banda. */
    fun normalize(list: List<RadioPreset>): List<RadioPreset> =
        list.distinctBy { it.band to it.khz }.sortedWith(compareBy({ it.band.ordinal }, { it.khz }))

    fun add(list: List<RadioPreset>, preset: RadioPreset): List<RadioPreset> =
        normalize(list.filterNot { it.band == preset.band && it.khz == preset.khz } + preset)

    fun remove(list: List<RadioPreset>, band: RadioBand, khz: Int): List<RadioPreset> =
        list.filterNot { it.band == band && it.khz == khz }
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

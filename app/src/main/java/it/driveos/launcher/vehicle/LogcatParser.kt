package it.driveos.launcher.vehicle

/**
 * Interpreta le righe del registro di sistema (logcat) dell'app Junsun `com.ts.MainUI`
 * e ne ricava segnali del veicolo. Serve perché su questo firmware portiere, sensori e
 * radio sono gestiti DENTRO quell'app e non escono come broadcast: l'unico punto in cui
 * compaiono, in chiaro, è il suo log di debug.
 *
 * DriveOS può leggere il logcat di altre app SOLO se l'utente gli concede, una volta, il
 * permesso Android `READ_LOGS` via ADB (nessun root). Finché non è concesso, non legge nulla.
 *
 * Esempi reali dal registro del 27/10/2026:
 *   CanDoorMsgbox: SetShowFlg = true        → una portiera è aperta
 *   CanDoorMsgbox: SetShowFlg = false       → portiere chiuse
 *   MainUI : BROADCAST_ADAS_LIGHT_STATE  on → luci accese
 *   CanFunc : CanFunc.mbRadarUIUpdate = true→ sensori di parcheggio attivi
 */
object LogcatParser {

    // id dei segnali ricavati dal log (prefisso "log:")
    const val DOOR = "log:porta"
    const val LIGHTS = "log:luci"
    const val PARKING = "log:sensori_parcheggio"
    const val REVERSE = "log:retromarcia"
    const val RADIO_FREQ = "log:radio_freq_khz"

    private val doorOn = Regex("CanDoorMsgbox.*SetShowFlg\\s*=\\s*true", RegexOption.IGNORE_CASE)
    private val doorOff = Regex("CanDoorMsgbox.*SetShowFlg\\s*=\\s*false", RegexOption.IGNORE_CASE)
    private val light = Regex("BROADCAST_ADAS_LIGHT_STATE\\s+(on|off)", RegexOption.IGNORE_CASE)
    private val radar = Regex("mbRadarUIUpdate\\s*=\\s*(true|false)", RegexOption.IGNORE_CASE)
    private val backcarIn = Regex("BackcarMainActivity")
    // frequenza radio: cerca "101.5", "101,5" o "10150"/"101500" vicino a FM/radio/freq
    // Solo contesti davvero radio: "freq" da solo no (cpu/gpu freq riempiono il log).
    private val freqCtx = Regex("(\\bfm\\b|radio|tuner|station)", RegexOption.IGNORE_CASE)
    private val windowName = Regex("CarClient/Service.{0,20}onReceive: name = (\\S+)")
    private val freqNum = Regex("(\\d{2,3}[.,]\\d)|(\\b\\d{5,6}\\b)")

    /** Interpreta una riga. Restituisce (id, valore) oppure null se non rilevante. */
    fun parse(line: String): Pair<String, String>? {
        // Il servizio CarClient scrive ogni cambio di finestra: funziona anche senza Accessibilità.
        windowName.find(line)?.let { return "win:last" to it.groupValues[1] }
        when {
            doorOn.containsMatchIn(line) -> return DOOR to "aperta"
            doorOff.containsMatchIn(line) -> return DOOR to "chiuse"
        }
        light.find(line)?.let { return LIGHTS to if (it.groupValues[1].equals("on", true)) "on" else "off" }
        radar.find(line)?.let { return PARKING to if (it.groupValues[1].equals("true", true)) "1" else "0" }
        if (backcarIn.containsMatchIn(line)) return REVERSE to "1"
        // Radio: solo se il contesto parla di FM/radio, per non prendere numeri a caso
        if (freqCtx.containsMatchIn(line)) {
            freqNum.find(line)?.let { m ->
                val khz = RadioFreq.toKhz(m.value)
                if (khz != null) return RADIO_FREQ to khz.toString()
            }
        }
        return null
    }

    /** I tag da passare a logcat per leggere solo ciò che serve (meno rumore, meno batteria). */
    val tags = listOf("CanDoorMsgbox", "CanFunc", "MainUI", "com.ts.MainUI", "ActivityTaskManager")
}

/** Normalizza una frequenza FM scritta in vari modi a kHz (87.5–108 MHz). */
object RadioFreq {
    fun toKhz(raw: String): Int? {
        val t = raw.trim().replace(',', '.')
        val d = t.toDoubleOrNull() ?: return null
        val khz = when {
            d in 87.0..108.0 -> d * 1000
            d in 8750.0..10800.0 -> d * 10
            d in 87000.0..108000.0 -> d
            else -> return null
        }
        val snapped = Math.round(khz / 100.0).toInt() * 100
        return if (snapped in 87_500..108_000) snapped else null
    }
}

package it.driveos.launcher.vehicle

/**
 * Formato del registro delle variazioni (CSV). Ogni riga è un cambiamento di un segnale:
 *   epochMillis,data_ora,id,valore_precedente,valore_nuovo
 * Virgole, virgolette e a-capo nei valori vengono messi tra virgolette (CSV standard),
 * così il file resta pulito anche con valori strani.
 */
object LogFormat {
    const val HEADER = "epoch_ms,data_ora,segnale,precedente,nuovo"

    fun line(epochMs: Long, isoTime: String, id: String, old: String?, new: String?): String =
        listOf(epochMs.toString(), isoTime, id, old ?: "", new ?: "").joinToString(",") { escape(it) }

    fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' })
            "\"" + field.replace("\"", "\"\"") + "\""
        else field

    /**
     * Confronta due istantanee e produce le righe-cambiamento (solo ciò che è cambiato,
     * comparso o sparito). Non registra nulla se non è cambiato niente.
     */
    fun diffLines(epochMs: Long, isoTime: String, before: Map<String, String>, after: Map<String, String>): List<String> {
        val out = ArrayList<String>()
        for ((k, v) in after) {
            val old = before[k]
            if (old != v) out.add(line(epochMs, isoTime, k, old, v))
        }
        for ((k, v) in before) if (k !in after) out.add(line(epochMs, isoTime, k, v, null))
        return out
    }
}

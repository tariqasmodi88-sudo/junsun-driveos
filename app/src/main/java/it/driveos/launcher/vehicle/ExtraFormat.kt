package it.driveos.launcher.vehicle

/**
 * Rende leggibile il contenuto degli "extra" di un broadcast. Fondamentale per il CAN:
 * i dati di `com.ts.can.BROADCAST_CAN_INFO` arrivano spesso come array di byte/interi,
 * che senza conversione risulterebbero illeggibili (es. "[I@3f2a1b"). Qui diventano
 * "12,0,88,255,…" così nel registro si vede il contenuto reale e lo possiamo decodificare.
 */
object ExtraFormat {
    private const val MAX = 400

    fun stringify(value: Any?): String? {
        val s = when (value) {
            null -> return null
            is ByteArray -> value.joinToString(",") { (it.toInt() and 0xFF).toString() }
            is IntArray -> value.joinToString(",")
            is ShortArray -> value.joinToString(",")
            is LongArray -> value.joinToString(",")
            is BooleanArray -> value.joinToString(",") { if (it) "1" else "0" }
            is FloatArray -> value.joinToString(",")
            is DoubleArray -> value.joinToString(",")
            is CharArray -> String(value)
            is Array<*> -> value.joinToString(",") { it?.toString() ?: "" }
            else -> value.toString()
        }
        return if (s.length > MAX) s.substring(0, MAX) + "…" else s
    }
}

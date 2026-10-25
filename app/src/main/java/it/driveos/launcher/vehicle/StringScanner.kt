package it.driveos.launcher.vehicle

/**
 * Estrae sequenze di testo leggibile da dati binari (come il comando `strings`).
 * Serve a trovare i nomi delle azioni broadcast dentro APK, dex, vdex e manifest
 * delle app Junsun, qualunque sia il formato del file. Funziona a blocchi: si
 * possono passare file grandi un pezzo alla volta.
 */
class StringScanner(private val minLength: Int = 8, private val maxLength: Int = 200) {
    private val ascii = StringBuilder()
    private val utf16 = StringBuilder()
    private var utf16Pending: Int = -1 // byte basso in attesa del byte alto
    private val found = LinkedHashSet<String>()

    fun feed(bytes: ByteArray, length: Int = bytes.size) {
        for (i in 0 until length) {
            val b = bytes[i].toInt() and 0xFF
            // ASCII (dex, vdex, testo)
            if (isPrintable(b)) {
                if (ascii.length < maxLength) ascii.append(b.toChar()) else ascii.setLength(0)
            } else {
                flush(ascii)
            }
            // UTF-16LE (manifest binario): carattere stampabile seguito da byte 0
            if (utf16Pending < 0) {
                utf16Pending = b
            } else {
                val low = utf16Pending
                utf16Pending = -1
                if (b == 0 && isPrintable(low)) {
                    if (utf16.length < maxLength) utf16.append(low.toChar()) else utf16.setLength(0)
                } else {
                    flush(utf16)
                    // riallineamento: il byte corrente potrebbe essere l'inizio di un carattere
                    utf16Pending = b
                }
            }
        }
    }

    /** Chiude le sequenze aperte e restituisce tutte le stringhe trovate (senza duplicati). */
    fun finish(): Set<String> {
        flush(ascii)
        flush(utf16)
        return found
    }

    private fun flush(sb: StringBuilder) {
        if (sb.length >= minLength) found.add(sb.toString())
        sb.setLength(0)
    }

    private fun isPrintable(b: Int) = b in 0x20..0x7E

    companion object {
        fun scan(bytes: ByteArray, minLength: Int = 8): Set<String> =
            StringScanner(minLength).apply { feed(bytes) }.finish()
    }
}

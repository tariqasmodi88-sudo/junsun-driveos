package it.driveos.launcher.vehicle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.util.concurrent.CopyOnWriteArrayList

/** Un messaggio della seriale auto: '#', tipo ('C' o 'S'), lunghezza, contenuto, somma di controllo. */
class CanFrame(val type: Char, val payload: ByteArray)

/**
 * Taglia in messaggi il flusso di byte di /dev/ttyS1 (MCU/CAN → radio, registrato il 30/10).
 * Struttura: 0x23 ('#'), 'C' o 'S', n, n byte di contenuto, 1 byte di controllo.
 * Se il flusso inizia a meta' di un messaggio, riparte dal primo '#' valido.
 */
class CanStreamParser {
    private var buf = ByteArray(0)

    fun feed(data: ByteArray, len: Int = data.size): List<CanFrame> {
        buf += data.copyOfRange(0, len)
        val out = ArrayList<CanFrame>()
        val junk = java.io.ByteArrayOutputStream()
        fun flushJunk() {
            // Byte che non fanno parte di nessun messaggio noto (es. tasti dell'autoradio): li teniamo, "X".
            val b = junk.toByteArray(); junk.reset()
            if (b.isNotEmpty() && b.any { it.toInt() != 0 }) out.add(CanFrame('X', b.copyOfRange(0, minOf(b.size, 24))))
        }
        var i = 0
        while (i + 3 <= buf.size) {
            val t = buf[i + 1].toInt()
            if (buf[i].toInt() == 0x23 && (t == 'C'.code || t == 'S'.code) && (buf[i + 2].toInt() and 0xFF) in 1..40) {
                val n = buf[i + 2].toInt() and 0xFF
                if (i + 4 + n > buf.size) break // messaggio incompleto: aspetta altri byte
                flushJunk()
                out.add(CanFrame(t.toChar(), buf.copyOfRange(i + 3, i + 3 + n)))
                i += 4 + n
            } else { junk.write(buf[i].toInt()); i++ }
        }
        flushJunk()
        buf = buf.copyOfRange(i, buf.size)
        if (buf.size > 4096) buf = ByteArray(0)
        return out
    }
}

/**
 * Traduce i messaggi in segnali (id → valore). Decodifica verificata sul giro del 30/10 (Tiguan 2016):
 *  - C 0x24 sotto-tipo 04: maschera porte (1 ant.sx, 2 ant.dx, 4 post.sx, 8 post.dx, 0x10 baule);
 *  - C 0x41 sotto-tipo 01: bit 0x20 a 0 = freno a mano tirato (verificato dall'utente);
 *  - C 0x22 / 0x23: 4 sensori di parcheggio posteriori / anteriori (1 = nessun ostacolo, sale avvicinandosi);
 *  - S 0x41: bit 0 del 2° byte = retromarcia;
 *  - C 0x7d sotto-tipo 01 (luci, prova 30/10): bit 0x40 luci accese, 0x20 anabbaglianti, 0x04 retronebbia;
 *    0x10/0x08 sono le frecce (lampeggiano: non vengono registrate);
 *  - C 0x7d sotto-tipo 06: bit 0x20 = cintura guidatore allacciata (da verificare);
 *  - C 0x7d sotto-tipo 0b: tensione batteria in mV (little endian).
 * Tutto il resto viene registrato grezzo come `can:C<id>/<sotto>` o `can:S<tipo>` per scoprire
 * giri, carburante, cinture… (solo le variazioni, il filtro e' nel chiamante).
 */
object CanDecoder {
    const val DOOR_FL = "can:door_fl"
    const val DOOR_FR = "can:door_fr"
    const val DOOR_RL = "can:door_rl"
    const val DOOR_RR = "can:door_rr"
    const val TRUNK = "can:trunk"
    const val HANDBRAKE = "can:handbrake"
    const val REVERSE = "can:reverse"
    const val BATTERY_V = "can:battery_v"
    const val DOORS_MASK = "can:doors"
    const val LIGHTS = "can:lights"
    const val LOWBEAM = "can:lowbeam"
    const val FOG = "can:fog"
    const val BELT = "can:belt"
    const val REAR_SENSORS = "can:rear"
    const val FRONT_SENSORS = "can:front"
    /** Byte della seriale che non sono un messaggio noto (probabili tasti dell'autoradio). */
    const val RAW_BYTES = "can:X"

    fun decode(f: CanFrame): Map<String, String> {
        val p = f.payload
        val out = LinkedHashMap<String, String>()
        if (f.type == 'X') {
            out[RAW_BYTES] = hex(p, 0)
            return out
        }
        if (f.type == 'S') {
            if (p.isEmpty()) return out
            val t = p[0].toInt() and 0xFF
            if (t == 0x41 && p.size >= 2) out[REVERSE] = if ((p[1].toInt() and 1) != 0) "1" else "0"
            out["can:S%02x".format(t)] = hex(p, 1)
            return out
        }
        if (p.size < 3 || (p[0].toInt() and 0xFF) != 0x2E) {
            out["can:C?"] = hex(p, 0)
            return out
        }
        val id = p[1].toInt() and 0xFF
        val len = p[2].toInt() and 0xFF
        if (len < 1 || p.size < 3 + len) {
            out["can:C%02x".format(id)] = hex(p, 2)
            return out
        }
        val d = p.copyOfRange(3, 3 + len)
        val sub = d[0].toInt() and 0xFF
        val v = if (len >= 2) d[1].toInt() and 0xFF else -1
        when {
            id == 0x24 && sub == 0x04 && v >= 0 -> doors(v, out)
            id == 0x41 && sub == 0x01 && v >= 0 -> out[HANDBRAKE] = if ((v and 0x20) == 0) "1" else "0" // verificato in auto: bit a 0 = tirato
            id == 0x7D && sub == 0x01 && v >= 0 -> {
                out[LIGHTS] = if ((v and 0x40) != 0) "1" else "0"
                out[LOWBEAM] = if ((v and 0x20) != 0) "1" else "0"
                out[FOG] = if ((v and 0x04) != 0) "1" else "0"
                return out // le frecce lampeggiano: niente riga grezza
            }
            id == 0x7D && sub == 0x06 && v >= 0 -> out[BELT] = if ((v and 0x20) != 0) "1" else "0"
            id == 0x7D && sub == 0x0B && len >= 3 -> {
                val mv = (d[1].toInt() and 0xFF) or ((d[2].toInt() and 0xFF) shl 8)
                out[BATTERY_V] = "%.1f".format(java.util.Locale.US, mv / 1000.0)
                return out // cambia di continuo: niente riga grezza
            }
        }
        if ((id == 0x22 || id == 0x23) && len == 4) {
            // 4 valori, uno per sensore (sinistra → destra)
            out[if (id == 0x22) REAR_SENSORS else FRONT_SENSORS] = d.joinToString(" ") { (it.toInt() and 0xFF).toString() }
        }
        out["can:C%02x/%02x".format(id, sub)] = hex(d, 1)
        return out
    }

    private fun doors(mask: Int, out: MutableMap<String, String>) {
        out[DOORS_MASK] = (mask and 0x1F).toString()
        out[DOOR_FL] = bit(mask, 0x01)
        out[DOOR_FR] = bit(mask, 0x02)
        out[DOOR_RL] = bit(mask, 0x04)
        out[DOOR_RR] = bit(mask, 0x08)
        out[TRUNK] = bit(mask, 0x10)
    }

    private fun bit(mask: Int, b: Int) = if ((mask and b) != 0) "1" else "0"
    private fun hex(b: ByteArray, from: Int) = (from until b.size).joinToString(" ") { "%02x".format(b[it]) }

    /** Livelli 0..3 dei 4 sensori (0 = libero). La scala grezza non e' ancora nota: 1 = libero, poi sale. */
    fun sensorLevels(signals: Map<String, String>, key: String): List<Int>? {
        val raw = signals[key]?.split(" ")?.mapNotNull { it.toIntOrNull() } ?: return null
        if (raw.size != 4) return null
        return raw.map { (it - 1).coerceIn(0, 3) }
    }

    /** Etichette leggibili delle porte aperte, es. ["anteriore sinistra", "baule"]. */
    fun openDoors(signals: Map<String, String>): List<String>? {
        if (!signals.containsKey(DOORS_MASK)) return null
        return buildList {
            if (signals[DOOR_FL] == "1") add("ant. sx")
            if (signals[DOOR_FR] == "1") add("ant. dx")
            if (signals[DOOR_RL] == "1") add("post. sx")
            if (signals[DOOR_RR] == "1") add("post. dx")
            if (signals[TRUNK] == "1") add("baule")
        }
    }
}

/**
 * Ascolta /dev/ttyS1 (sola lettura, in primo piano o in background) e pubblica le variazioni.
 * Se la porta non si apre (permessi, altro firmware) riprova ogni 30 s senza fare danni.
 */
class CanSerialReader(private val path: String = "/dev/ttyS1") {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sinks = CopyOnWriteArrayList<(Map<String, String>) -> Unit>()
    @Volatile private var job: Job? = null
    @Volatile private var stream: FileInputStream? = null
    @Volatile var framesSeen: Long = 0
        private set

    fun addSink(s: (Map<String, String>) -> Unit) { if (!sinks.contains(s)) sinks.add(s) }
    fun removeSink(s: (Map<String, String>) -> Unit) { sinks.remove(s) }

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            val last = HashMap<String, String>()
            while (isActive) {
                try {
                    FileInputStream(path).use { ins ->
                        stream = ins
                        val parser = CanStreamParser()
                        val buf = ByteArray(512)
                        while (isActive) {
                            val n = ins.read(buf)
                            if (n < 0) break
                            val changes = LinkedHashMap<String, String>()
                            for (f in parser.feed(buf, n)) {
                                framesSeen++
                                for ((k, v) in CanDecoder.decode(f)) {
                                    // I tasti (byte "X") sono eventi: ogni pressione conta, anche se identica alla precedente.
                                    if (k == CanDecoder.RAW_BYTES) changes[k] = v
                                    else if (last[k] != v) { last[k] = v; changes[k] = v }
                                }
                            }
                            if (changes.isNotEmpty()) sinks.forEach { runCatching { it(changes) } }
                        }
                    }
                } catch (_: Throwable) { }
                stream = null
                if (isActive) delay(30_000)
            }
        }
    }

    fun stop() {
        job?.cancel(); job = null
        runCatching { stream?.close() }
    }
}

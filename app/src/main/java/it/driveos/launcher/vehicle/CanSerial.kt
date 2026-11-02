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
 * Comandi verso la centralina CAN, con lo stesso formato che usa la radio Junsun
 * (libts67xhw.so c_SendCanMsg + libcan59.so CCan::MakeCmd):
 *   esterno: '#', 'C', n, [n byte], XOR di ('C', n, contenuto)
 *   interno (protocollo Raise): 0x2E, comando, lunghezza, dati, NOT(somma di comando+lunghezza+dati)
 * La schermata dati auto della radio, all'apertura, chiede 0x90 [0x41, 0x01] e 0x90 [0x41, 0x02].
 */
object CanQuery {
    fun raise(cmd: Int, vararg data: Int): ByteArray {
        val b = ByteArray(4 + data.size)
        b[0] = 0x2E; b[1] = cmd.toByte(); b[2] = data.size.toByte()
        data.forEachIndexed { i, v -> b[3 + i] = v.toByte() }
        var sum = 0
        for (i in 1 until 3 + data.size) sum += b[i].toInt() and 0xFF
        b[3 + data.size] = (sum.inv() and 0xFF).toByte()
        return b
    }

    fun frame(payload: ByteArray): ByteArray {
        val b = ByteArray(payload.size + 4)
        b[0] = 0x23; b[1] = 'C'.code.toByte(); b[2] = payload.size.toByte()
        payload.copyInto(b, 3)
        var x = 0
        for (i in 1 until 3 + payload.size) x = x xor (b[i].toInt() and 0xFF)
        b[3 + payload.size] = x.toByte()
        return b
    }

    /** Richiesta dati motore (giri, velocita', tensione, temperatura esterna, autonomia, carburante). */
    val ENGINE: ByteArray = frame(raise(0x90, 0x41, 0x02))
    /** Richiesta stato/avvisi (riserva, batteria, freno a mano…). */
    val STATUS: ByteArray = frame(raise(0x90, 0x41, 0x01))
}

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
        // Gli ultimi 1-2 byte che non possono iniziare un messaggio ('#') non vanno tenuti in attesa.
        if (i + 3 > buf.size) while (i < buf.size && buf[i].toInt() != 0x23) { junk.write(buf[i].toInt()); i++ }
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
 *  - C 0x22 / 0x23: 4 sensori di parcheggio posteriori / anteriori (valore basso = ostacolo vicino; 0 o >= 10 = nulla);
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
    /** Passeggero anteriore: "1" = allacciata, "0" = slacciata. */
    const val BELT_PASSENGER = "can:belt_passenger"
    const val REAR_SENSORS = "can:rear"
    const val FRONT_SENSORS = "can:front"
    // Dati motore (C 0x41 sotto-tipo 02): la centralina li manda SOLO su richiesta, vedi [CanQuery].
    const val RPM = "can:rpm"
    const val SPEED = "can:speed"
    const val VOLTAGE = "can:voltage"
    const val OUT_TEMP = "can:out_temp"
    const val ODOMETER_KM = "can:odometer_km"
    const val FUEL_L = "can:fuel_l"
    // Clima (C 0x21) e sedili riscaldati (C 0x28), schema da libcan59.so CCanVolkswagen::HandleCanCmd
    const val AC_POWER = "can:ac_power"
    const val AC_COMP = "can:ac_ac"
    const val AC_RECIRC = "can:ac_recirc"
    const val AC_AUTO = "can:ac_auto"
    const val AC_DUAL = "can:ac_dual"
    const val AC_MAXDEF = "can:ac_maxdef"
    const val AC_REARDEF = "can:ac_reardef"
    const val AC_AIR_UP = "can:ac_air_up"
    const val AC_AIR_MID = "can:ac_air_mid"
    const val AC_AIR_DOWN = "can:ac_air_down"
    const val AC_FAN = "can:ac_fan"
    const val AC_TEMP_L = "can:ac_temp_l"
    const val AC_TEMP_R = "can:ac_temp_r"
    const val SEAT_HEAT_L = "can:seat_heat_l"
    const val SEAT_HEAT_R = "can:seat_heat_r"

    /** Temperatura impostata: 0 = LO, 31 = HI, altrimenti 18 °C + mezzi gradi. */
    fun acTemp(raw: String?): String? {
        val v = raw?.toIntOrNull() ?: return null
        return when (v) {
            0 -> "LO"
            0x1F -> "HI"
            else -> { val t = 18 + (v - 1) * 0.5; if (t % 1.0 == 0.0) t.toInt().toString() else String.format(java.util.Locale.ITALY, "%.1f", t) }
        }
    }

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
            id == 0x21 && len >= 4 -> {
                val a = sub; val w = v
                fun b(x: Int, m: Int) = if ((x and m) != 0) "1" else "0"
                out[AC_POWER] = b(a, 0x80); out[AC_COMP] = b(a, 0x40); out[AC_RECIRC] = b(a, 0x20)
                out[AC_AUTO] = when { (a and 0x10) != 0 -> "2"; (a and 0x08) != 0 -> "1"; else -> "0" }
                out[AC_DUAL] = b(a, 0x04); out[AC_MAXDEF] = b(a, 0x02); out[AC_REARDEF] = b(a, 0x01)
                out[AC_AIR_UP] = b(w, 0x80); out[AC_AIR_MID] = b(w, 0x40); out[AC_AIR_DOWN] = b(w, 0x20)
                out[AC_FAN] = (w and 0x07).toString()
                out[AC_TEMP_L] = (d[2].toInt() and 0xFF).toString()
                out[AC_TEMP_R] = (d[3].toInt() and 0xFF).toString()
                return out
            }
            id == 0x28 && len >= 1 -> {
                out[SEAT_HEAT_L] = ((sub shr 3) and 0x07).toString()
                out[SEAT_HEAT_R] = (sub and 0x07).toString()
                return out
            }
            id == 0x41 && sub == 0x02 && len >= 13 -> {
                // Schema letto dalla libreria della radio (libcan59.so, CCanVolkswagen::DealCarInfo):
                // giri, velocita' x0.01, tensione x0.01, temp. esterna x0.1 (con segno), chilometri totali 24 bit (verificato: 173952 = contachilometri), litri.
                fun u16(i: Int) = ((d[i].toInt() and 0xFF) shl 8) or (d[i + 1].toInt() and 0xFF)
                out[RPM] = u16(1).toString()
                out[SPEED] = "%.0f".format(java.util.Locale.US, u16(3) / 100.0)
                out[VOLTAGE] = "%.1f".format(java.util.Locale.US, u16(5) / 100.0)
                out[OUT_TEMP] = "%.1f".format(java.util.Locale.US, u16(7).toShort() / 10.0)
                out[ODOMETER_KM] = (((d[9].toInt() and 0xFF) shl 16) or ((d[10].toInt() and 0xFF) shl 8) or (d[11].toInt() and 0xFF)).toString()
                out[FUEL_L] = (d[12].toInt() and 0xFF).toString()
                return out // cambiano di continuo: niente riga grezza
            }
            id == 0x7D && sub == 0x06 && v >= 0 -> {
                out[BELT] = if ((v and 0x20) != 0) "1" else "0"
                // passeggero: bit 0x10 acceso = cintura allacciata (corretto dopo la prova in auto del 1/11)
                out[BELT_PASSENGER] = if ((v and 0x10) != 0) "1" else "0"
            }
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

    /**
     * Livelli 0..3 dei 4 sensori: 0 = bianco, 1 = giallo, 2 = arancio, 3 = rosso. Scala misurata in auto:
     * il valore SCENDE avvicinandosi all'ostacolo (1-3 = vicinissimo); da 10 in su, oppure 0, il sensore non vede nulla.
     */
    fun resetSensorScale() {}

    fun sensorLevels(signals: Map<String, String>, key: String): List<Int>? {
        val raw = sensorRaw(signals, key) ?: return null
        return raw.map { v ->
            when {
                v <= 0 || v >= 10 -> 0
                v <= 3 -> 3
                v <= 6 -> 2
                else -> 1
            }
        }
    }

    fun sensorRaw(signals: Map<String, String>, key: String): List<Int>? {
        val raw = signals[key]?.split(" ")?.mapNotNull { it.toIntOrNull() } ?: return null
        return raw.takeIf { it.size == 4 }
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
 * Se la porta non si apre (permessi, altro firmware) riprova ogni 2 s senza fare danni; se tace per 20 s la riapre.
 */
class CanSerialReader(private val path: String = "/dev/ttyS1") {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sinks = CopyOnWriteArrayList<(Map<String, String>) -> Unit>()
    @Volatile private var job: Job? = null
    @Volatile private var stream: FileInputStream? = null
    @Volatile var framesSeen: Long = 0
        private set
    /** Ultimo errore della richiesta dati motore (null = inviata). */
    @Volatile var queryError: String? = null
        private set
    private var queryJob: Job? = null
    @Volatile private var lastByteAt = 0L

    fun addSink(s: (Map<String, String>) -> Unit) { if (!sinks.contains(s)) sinks.add(s) }
    fun removeSink(s: (Map<String, String>) -> Unit) { sinks.remove(s) }

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            // Mai durante l'avvio o il risveglio della radio: in quei secondi la radio Junsun "saluta" l'MCU
            // su questa stessa linea (vedi pause()). Si aspetta [notBefore] e che lo schermo sia acceso.
            while (isActive && (System.currentTimeMillis() < notBefore || !screenOn() || !it.driveos.launcher.system.PowerGate.ready())) delay(1000)
            startQueries()
            val last = HashMap<String, String>()
            while (isActive) {
                try {
                    FileInputStream(path).use { ins ->
                        stream = ins
                        last.clear() // dopo una riapertura si ripubblica lo stato corrente
                        lastByteAt = System.currentTimeMillis()
                        // Guardiano: se la porta tace per 20 s (lettura bloccata dopo un ciclo del quadro, ecc.)
                        // la si chiude: la lettura fallisce e riparte da capo.
                        val dog = launch {
                            while (isActive) {
                                delay(5_000)
                                if (System.currentTimeMillis() - lastByteAt > 20_000) { runCatching { ins.close() }; break }
                            }
                        }
                        val parser = CanStreamParser()
                        val buf = ByteArray(512)
                        while (isActive) {
                            // Lettura SENZA attesa bloccata: si legge solo cio' che e' gia' arrivato.
                            // Cosi' allo spegnimento si smette all'istante e non resta una lettura "in sospeso"
                            // che ruberebbe alla radio la risposta dell'MCU (saluto di spegnimento/riaccensione).
                            if (paused || !it.driveos.launcher.system.PowerGate.ready()) break
                            val avail = runCatching { ins.available() }.getOrDefault(-1)
                            if (avail < 0) break
                            if (avail == 0) { delay(5); continue }
                            val n = ins.read(buf, 0, minOf(avail, buf.size))
                            if (n < 0) break
                            lastByteAt = System.currentTimeMillis()
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
                        dog.cancel()
                    }
                } catch (_: Throwable) { }
                stream = null
                if (isActive) delay(2_000)
            }
        }
    }

    /**
     * Chiede alla centralina i dati motore ogni [periodMs], come fa la schermata dati auto della radio
     * quando e' aperta. Senza la richiesta la centralina non li manda.
     */
    /** Quadro spento / schermo spento: smette di interrogare la centralina (la lascia dormire). */
    val running: Boolean get() = job?.isActive == true
    /** Prima di questo istante la seriale non si apre (impostato all'avvio e a ogni riaccensione). */
    @Volatile var notBefore: Long = System.currentTimeMillis() + 20_000
    /** Stato dello schermo (impostato da DriveOSApp). */
    @Volatile var screenOn: () -> Boolean = { true }
    @Volatile private var paused = false

    /**
     * Quadro spento: la seriale e' la STESSA linea con cui la radio Junsun parla con l'MCU.
     * Durante lo spegnimento/riaccensione (QuickBoot) la radio fa un "saluto" con l'MCU su questa linea:
     * se DriveOS legge o scrive in quel momento, ruba i byte e la radio mostra ErrorCode:001 e resta bloccata.
     * Quindi a schermo spento si chiude tutto, e si riapre solo qualche secondo dopo la riaccensione.
     */
    fun pause() {
        paused = paused || running || job != null
        if (!paused) return
        stopQueries()
        job?.cancel(); job = null
        runCatching { stream?.close() }
    }

    /** Riprende solo se era stata messa in pausa (non riaccende una seriale spenta nelle impostazioni). */
    fun resumeIfPaused() { if (paused) { paused = false; start() } }
    fun stopQueries() { queryJob?.cancel(); queryJob = null }

    fun startQueries(periodMs: Long = 1000) {
        if (queryJob?.isActive == true) return
        queryJob = scope.launch {
            var n = 0
            while (isActive) {
                try {
                    java.io.FileOutputStream(path).use { out ->
                        while (isActive) {
                            out.write(CanQuery.ENGINE); out.flush()
                            if (n++ % 5 == 0) { delay(50); out.write(CanQuery.STATUS); out.flush() }
                            queryError = null
                            delay(periodMs)
                        }
                    }
                } catch (t: Throwable) { queryError = t.message ?: t.javaClass.simpleName }
                if (isActive) delay(5_000)
            }
        }
    }

    fun stop() {
        paused = false
        queryJob?.cancel(); queryJob = null
        job?.cancel(); job = null
        runCatching { stream?.close() }
    }
}

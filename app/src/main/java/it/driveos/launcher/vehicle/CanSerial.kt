package it.driveos.launcher.vehicle

/** Un messaggio della seriale auto: '#', tipo ('C', 'S' o 'K'), lunghezza, contenuto, somma di controllo. */
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
    /** Richiesta angolo volante (EPS): la radio la manda ogni ~60 ms in retromarcia (libcan59, CCanVolkswagen::QueryEPS). */
    val EPS: ByteArray = frame(raise(0x90, 0x26, 0x00))
    /** Richieste generiche Raise (0x90 [comando, sotto]): stato porte/fari (0x24) e luci (0x7D/01). */
    val DOORS_FLAGS: ByteArray = frame(raise(0x90, 0x24, 0x00))
    val LIGHTS: ByteArray = frame(raise(0x90, 0x7D, 0x01))
}

/**
 * Taglia in messaggi il flusso di byte di /dev/ttyS1 (MCU/CAN → radio, registrato il 30/10).
 * Struttura: 0x23 ('#'), 'C' / 'S' / 'K', n, n byte di contenuto, 1 byte di controllo
 * (XOR di tipo, n e contenuto). Dentro i messaggi 'C' c'e' il protocollo Raise: 0x2E, comando,
 * lunghezza, dati, NOT(somma di comando + lunghezza + dati).
 *
 * La linea e' condivisa con la radio Junsun: un messaggio puo' arrivare a pezzi, meta' a DriveOS e
 * meta' alla radio. Per questo si accettano SOLO i messaggi con le somme di controllo giuste (sulle
 * catture reali: 2356 messaggi, tutti corretti): un messaggio spezzato non diventa piu' una porta
 * aperta o una retromarcia che non esistono. Se la somma non torna si scarta un byte e si riparte
 * dal '#' successivo, cosi' i messaggi buoni che seguono non si perdono.
 */
class CanStreamParser {
    private var buf = ByteArray(0)

    /** Messaggi scartati perche' la somma di controllo non torna (diagnostica). */
    var rejected: Long = 0
        private set

    fun feed(data: ByteArray, len: Int = data.size): List<CanFrame> {
        buf += data.copyOfRange(0, len)
        val out = ArrayList<CanFrame>()
        val junk = java.io.ByteArrayOutputStream()
        fun flushJunk() {
            // Byte che non fanno parte di nessun messaggio noto: li teniamo, "X".
            val b = junk.toByteArray(); junk.reset()
            if (b.isNotEmpty() && b.any { it.toInt() != 0 }) out.add(CanFrame('X', b.copyOfRange(0, minOf(b.size, 24))))
        }
        var i = 0
        while (i + 3 <= buf.size) {
            val t = buf[i + 1].toInt()
            // 'K' = tasti dell'autoradio (registro 07/11: "# K 03 10 <tasto> <1=premuto|2=rilasciato> xor")
            if (buf[i].toInt() == 0x23 && (t == 'C'.code || t == 'S'.code || t == 'K'.code) && (buf[i + 2].toInt() and 0xFF) in 1..40) {
                val n = buf[i + 2].toInt() and 0xFF
                if (i + 4 + n > buf.size) break // messaggio incompleto: aspetta altri byte
                if (checksumOk(buf, i, n)) {
                    flushJunk()
                    out.add(CanFrame(t.toChar(), buf.copyOfRange(i + 3, i + 3 + n)))
                    i += 4 + n
                    continue
                }
                rejected++
            }
            junk.write(buf[i].toInt()); i++
        }
        // Gli ultimi 1-2 byte che non possono iniziare un messaggio ('#') non vanno tenuti in attesa.
        if (i + 3 > buf.size) while (i < buf.size && buf[i].toInt() != 0x23) { junk.write(buf[i].toInt()); i++ }
        flushJunk()
        buf = buf.copyOfRange(i, buf.size)
        if (buf.size > 4096) buf = ByteArray(0)
        return out
    }

    companion object {
        /** Somma esterna (XOR) e, per i messaggi Raise completi, anche quella interna. */
        fun checksumOk(b: ByteArray, at: Int, n: Int): Boolean {
            var x = 0
            for (j in at + 1 until at + 3 + n) x = x xor (b[j].toInt() and 0xFF)
            if (x != (b[at + 3 + n].toInt() and 0xFF)) return false
            if (b[at + 1].toInt() != 'C'.code || n < 4 || (b[at + 3].toInt() and 0xFF) != 0x2E) return true
            val len = b[at + 5].toInt() and 0xFF
            if (n != len + 4) return true // forma non standard: la decide il decodificatore
            var sum = 0
            for (j in at + 4 until at + 6 + len) sum += b[j].toInt() and 0xFF
            return (sum.inv() and 0xFF) == (b[at + 6 + len].toInt() and 0xFF)
        }
    }
}

/**
 * Traduce i messaggi in segnali (id → valore). Decodifica verificata sul giro del 30/10 (Tiguan 2016):
 *  - C 0x24: 1° byte bit di stato (0x04 = fari), 2° byte maschera porte (1 ant.sx, 2 ant.dx, 4 post.sx, 8 post.dx, 0x10 baule);
 *  - C 0x41 sotto-tipo 01: bit 0x20 a 0 = freno a mano tirato (verificato dall'utente);
 *  - C 0x22 / 0x23: 4 sensori di parcheggio posteriori / anteriori (valore basso = ostacolo vicino; 0 o >= 10 = nulla);
 *  - C 0x25: stato del Park Pilot, mandato una volta all'accensione e allo spegnimento dei sensori
 *    (cattura del 30/10: "02 00" entrando in retromarcia, "00 00" allo spegnimento): bit 0x02 = attivo;
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
    /** Park Pilot (sensori di parcheggio) acceso "1" / spento "0", da C 0x25. */
    const val PARK = "can:park"
    const val SENSORS_OFF = "0 0 0 0"
    /** Angolo volante (C 0x26, protocollo Raise VW PQ): little-endian con segno, -500..+500 (libcan59 HandleCanCmd 0x26). */
    const val STEER = "can:steer"
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
    /** Tasto dell'autoradio premuto (messaggio 'K'): codice esadecimale. 05 = Indietro, 01 = Accensione. */
    const val KEY = "can:key"

    fun decode(f: CanFrame): Map<String, String> {
        val p = f.payload
        val out = LinkedHashMap<String, String>()
        if (f.type == 'K') {
            // solo la pressione (3° byte 1); il rilascio (2) si ignora
            if (p.size >= 3 && (p[0].toInt() and 0xFF) == 0x10 && p[2].toInt() == 1) out[KEY] = "%02x".format(p[1].toInt() and 0xFF)
            return out
        }
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
            // 0x24: il 1° byte NON e' un sotto-tipo ma bit di stato (0x04 = anabbaglianti accesi, come nel
            // protocollo Raise VW PQ). Prima si leggevano le porte solo con 0x04, cioe' solo a fari accesi
            // (07/11: "a fari spenti le porte non si vedono"). Le porte sono sempre nel 2° byte.
            id == 0x24 && v >= 0 -> { doors(v, out); out["can:C24/flags"] = sub.toString(16) }
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
        if (id == 0x26 && len >= 2) {
            // little-endian con segno (come gli emulatori Raise PQ open source); cambia di continuo
            out[STEER] = (((d[1].toInt() and 0xFF) shl 8) or (d[0].toInt() and 0xFF)).toShort().toInt().toString()
            return out
        }
        if (id == 0x25) out[PARK] = if ((sub and 0x02) != 0) "1" else "0"
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
 * Stato tra un messaggio e l'altro della seriale (logica pura, testabile):
 *  - passa solo le VARIAZIONI (i tasti invece sono eventi: passano sempre);
 *  - sensori di parcheggio: quando il Park Pilot si spegne (C 0x25 = 0) si azzerano subito e per
 *    [MUTE_MS] si ignorano i loro messaggi. Nella cattura del 30/10, dopo lo spegnimento, la centralina
 *    manda ancora "00 00 00 00" e poi "01 01 01 01" (che in scala vale "vicinissimo") e tace: senza
 *    questo filtro i sensori tornavano rossi per 2 s e la schermata Veicolo si riapriva da sola;
 *  - se lo spegnimento non e' arrivato (messaggio preso dalla radio) resta la scadenza: senza
 *    aggiornamenti per [EXPIRE_MS] i sensori si azzerano (a Park Pilot acceso arrivano ogni ~0,3 s).
 */
class CanSignalFilter {
    private val last = HashMap<String, String>()
    private val sensorSeen = HashMap<String, Long>()
    private var muteUntil = 0L

    /** Applica i segnali di un messaggio; in [changes] finisce solo cio' che e' cambiato. */
    fun apply(decoded: Map<String, String>, now: Long, changes: MutableMap<String, String>) {
        for ((k, v) in decoded) {
            when {
                k == CanDecoder.RAW_BYTES || k == CanDecoder.KEY -> { changes[k] = v; continue }
                k == CanDecoder.PARK -> {
                    if (v == "0") { muteUntil = now + MUTE_MS; clearSensors(changes) } else muteUntil = 0L
                }
                k in SENSOR_KEYS -> {
                    if (now < muteUntil) continue
                    sensorSeen[k] = now
                }
            }
            if (last[k] != v) { last[k] = v; changes[k] = v }
        }
    }

    /** Sensori senza aggiornamenti da troppo tempo: si azzerano. */
    fun expire(now: Long): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (k in SENSOR_KEYS) {
            val seen = sensorSeen[k] ?: continue
            if (now - seen > EXPIRE_MS && last[k] != null && last[k] != CanDecoder.SENSORS_OFF) {
                last[k] = CanDecoder.SENSORS_OFF; out[k] = CanDecoder.SENSORS_OFF
            }
        }
        return out
    }

    /**
     * La lettura si ferma o riparte (quadro spento, porta riaperta): cio' che si sapeva non vale piu'.
     * Restituisce i sensori da azzerare (non devono restare "rossi" con la seriale ferma); al prossimo
     * messaggio tutto viene ripubblicato.
     */
    fun reset(): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        clearSensors(out)
        last.clear(); sensorSeen.clear(); muteUntil = 0L
        return out
    }

    private fun clearSensors(changes: MutableMap<String, String>) {
        for (k in SENSOR_KEYS) {
            val cur = last[k] ?: continue
            if (cur != CanDecoder.SENSORS_OFF) { last[k] = CanDecoder.SENSORS_OFF; changes[k] = CanDecoder.SENSORS_OFF }
        }
    }

    companion object {
        const val EXPIRE_MS = 2_000L
        const val MUTE_MS = 1_500L
        val SENSOR_KEYS = listOf(CanDecoder.REAR_SENSORS, CanDecoder.FRONT_SENSORS)
    }
}

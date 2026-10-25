package it.driveos.launcher.vehicle

/**
 * Logica pura (testabile senza Android) della sonda veicolo.
 *
 * Un "segnale" e' un valore leggibile dall'app, identificato da un id con prefisso:
 *  - `prop:<chiave>`             proprieta' di sistema (getprop)
 *  - `set:<tabella>/<chiave>`    impostazione Android (system/global/secure)
 *  - `file:<percorso>`           file di sistema leggibile (/sys, /proc)
 *  - `bc:<azione>|<extra>`       ultimo valore di un extra di un broadcast
 *  - `bc:<azione>|#`             numero di volte che il broadcast e' arrivato
 */
object SignalIds {
    fun prop(key: String) = "prop:$key"
    fun setting(table: String, key: String) = "set:$table/$key"
    fun file(path: String) = "file:$path"
    fun broadcast(action: String, extra: String) = "bc:$action|$extra"
    fun broadcastCount(action: String) = "bc:$action|#"

    fun source(id: String): String = id.substringBefore(':')

    /** Azione di un segnale broadcast, altrimenti null. */
    fun actionOf(id: String): String? = if (id.startsWith("bc:")) id.removePrefix("bc:").substringBefore('|') else null

    /** Descrizione leggibile per l'interfaccia. */
    fun describe(id: String): String = when (source(id)) {
        "prop" -> "Proprietà " + id.removePrefix("prop:")
        "set" -> "Impostazione " + id.removePrefix("set:")
        "file" -> "File " + id.removePrefix("file:")
        "bc" -> {
            val rest = id.removePrefix("bc:")
            val extra = rest.substringAfter('|')
            "Broadcast " + rest.substringBefore('|') + if (extra == "#") " (conteggio)" else " → $extra"
        }
        else -> id
    }
}

/** Output di `getprop`: righe "[chiave]: [valore]". */
object GetpropParser {
    private val line = Regex("^\\[([^\\]]+)]: \\[(.*)]$")

    fun parse(text: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        text.lineSequence().forEach { l ->
            line.find(l.trim())?.let { m -> out[m.groupValues[1]] = m.groupValues[2] }
        }
        return out
    }
}

/** Differenze tra due istantanee: cosa e' cambiato, cosa e' comparso, cosa e' sparito. */
data class Change(val id: String, val before: String?, val after: String?)

object SnapshotDiff {
    fun diff(before: Map<String, String>, after: Map<String, String>): List<Change> {
        val out = ArrayList<Change>()
        for ((k, v) in after) {
            val old = before[k]
            if (old != v) out.add(Change(k, old, v))
        }
        for ((k, v) in before) if (k !in after) out.add(Change(k, v, null))
        return out.sortedBy { it.id }
    }
}

/**
 * Candidato per un evento (es. fari): segnale con un valore quando l'evento e' "acceso"
 * e un valore diverso quando e' "spento".
 */
data class Candidate(val id: String, val onValue: String?, val offValue: String?, val changeCount: Int, val score: Double)

object CandidateFinder {
    /** Segnali che vengono ignorati perche' cambiano sempre (orologi, contatori di sistema). */
    private val noisy = listOf(
        "time", "uptime", "boottime", "_ms", "timestamp", "counter", "battery", "temp", "random",
        "dalvik.vm", "sys.sysctl", "init.svc_debug", "ro.boottime", "persist.sys.timezone", "next_alarm",
        "screen_off_timeout", "debug.", "logd", "heartbeat",
    )

    /**
     * @param on istantanee prese quando l'evento e' attivo (es. fari accesi), una o piu'
     * @param off istantanee prese quando l'evento non e' attivo
     * @param changeCounts quante volte ogni segnale e' cambiato durante la registrazione
     *        (un segnale che cambia di continuo e' poco affidabile)
     */
    fun find(
        on: List<Map<String, String>>,
        off: List<Map<String, String>>,
        changeCounts: Map<String, Int> = emptyMap(),
    ): List<Candidate> {
        if (on.isEmpty() || off.isEmpty()) return emptyList()
        val ids = (on.flatMap { it.keys } + off.flatMap { it.keys }).toSet()
        val out = ArrayList<Candidate>()
        for (id in ids) {
            if (id.endsWith("|#")) continue // i conteggi crescono sempre: non distinguono acceso/spento
            if (noisy.any { id.lowercase().contains(it) }) continue
            val onValues = on.map { it[id] }.toSet()
            val offValues = off.map { it[id] }.toSet()
            // coerente: stesso valore in tutte le istantanee "on", stesso in tutte le "off", e diversi
            if (onValues.size != 1 || offValues.size != 1) continue
            val onV = onValues.first()
            val offV = offValues.first()
            if (onV == offV) continue
            val changes = changeCounts[id] ?: 0
            val cycles = minOf(on.size, off.size)
            // piu' cicli confermati = piu' affidabile; troppi cambi extra = rumore
            val expected = (on.size + off.size).toDouble()
            val noise = if (changes > expected * 2) (changes / expected) else 1.0
            val sourceBonus = when (SignalIds.source(id)) { "bc" -> 1.3; "prop" -> 1.2; "file" -> 1.1; else -> 1.0 }
            out.add(Candidate(id, onV, offV, changes, cycles * sourceBonus / noise))
        }
        return out.sortedWith(compareByDescending<Candidate> { it.score }.thenBy { it.id })
    }
}

/** Ruolo di un segnale collegato dall'utente. */
enum class SignalRole { LIGHTS, GAUGE, ALERT }

/**
 * Collegamento salvato: "questo segnale indica i fari" oppure "mostra questo segnale
 * nella schermata Veicolo con questa etichetta".
 */
data class SignalBinding(
    val id: String,
    val role: SignalRole,
    val label: String,
    val onValue: String? = null,
    val offValue: String? = null,
    val unit: String = "",
)

object BindingCodec {
    private const val FIELD = '\u001F'
    private const val RECORD = '\u001E'
    private const val NULL = "\u0000"

    fun encode(list: List<SignalBinding>): String = list.joinToString(RECORD.toString()) { b ->
        listOf(b.id, b.role.name, b.label, b.onValue ?: NULL, b.offValue ?: NULL, b.unit).joinToString(FIELD.toString())
    }

    fun decode(raw: String?): List<SignalBinding> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(RECORD).mapNotNull { rec ->
            val f = rec.split(FIELD)
            if (f.size < 6) return@mapNotNull null
            val role = SignalRole.entries.firstOrNull { it.name == f[1] } ?: return@mapNotNull null
            SignalBinding(
                id = f[0], role = role, label = f[2],
                onValue = f[3].takeIf { it != NULL }, offValue = f[4].takeIf { it != NULL }, unit = f[5],
            )
        }
    }

    /** Un solo segnale per i fari; per il cruscotto un segnale compare una volta sola. */
    fun upsert(list: List<SignalBinding>, b: SignalBinding): List<SignalBinding> {
        val rest = list.filterNot { (b.role == SignalRole.LIGHTS && it.role == SignalRole.LIGHTS) || (it.role == b.role && it.id == b.id) }
        return rest + b
    }

    fun remove(list: List<SignalBinding>, id: String, role: SignalRole) = list.filterNot { it.id == id && it.role == role }
}

object LightsResolver {
    /** true = fari accesi, false = spenti, null = non si sa (segnale assente o valore mai visto). */
    fun resolve(binding: SignalBinding?, value: String?): Boolean? {
        if (binding == null || value == null) return null
        if (value == binding.onValue) return true
        if (value == binding.offValue) return false
        // valore nuovo: se conosciamo il valore "spento", tutto il resto e' "acceso" (es. livelli di luminosita')
        return if (binding.offValue != null) true else null
    }
}

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

package it.driveos.launcher.vehicle

/**
 * Segnali del veicolo riconosciuti automaticamente su questo firmware, senza calibrazione.
 * Ricavati dall'export reale della radio ForFan dell'utente.
 */
object KnownVehicleSignals {

    /** Proprietà che indicano lo stato dei fari/illuminazione (valore "acceso", valore "spento"). */
    val lights = listOf(
        LightProp("prop:forfan_light_state", on = "on", off = "off"), // confermato sulla ForFan dell'utente
        LightProp("prop:persist.sys.ill", on = "1", off = "0"),
        LightProp("prop:sys.ill.state", on = "1", off = "0"),
    )

    data class LightProp(val id: String, val on: String, val off: String)

    /** Proprietà che contiene il package del navigatore impostato nel sistema (ForFan). */
    const val SYSTEM_NAV_PROP = "prop:forfan.navi.name"

    /** Rileva i fari dai segnali letti, senza bisogno di calibrazione. null = non noto. */
    fun lightsOn(signals: Map<String, String>): Boolean? {
        for (p in lights) {
            val v = signals[p.id] ?: continue
            if (v.equals(p.on, ignoreCase = true)) return true
            if (v.equals(p.off, ignoreCase = true)) return false
        }
        // seconda fonte: seriale auto (bit luci), se la proprieta' di sistema non c'e'
        signals["can:lights"]?.let { return it == "1" }
        return null
    }

    /** Un SignalBinding "fari" sintetico dal segnale noto presente, per mostrarlo come collegato. */
    fun syntheticLightsBinding(signals: Map<String, String>): SignalBinding? {
        val p = lights.firstOrNull { signals.containsKey(it.id) } ?: return null
        return SignalBinding(p.id, SignalRole.LIGHTS, "Fari (automatico)", p.on, p.off)
    }

    /** Package del navigatore impostato nel sistema, se presente. */
    fun systemNavPackage(signals: Map<String, String>): String? =
        signals[SYSTEM_NAV_PROP]?.takeIf { it.isNotBlank() && it.contains('.') }

    /** Retromarcia reale su ForFan: broadcast com.forfan.operator_reversing, extra "operate". */
    const val REVERSE_ID = "bc:com.forfan.operator_reversing|operate"

    /** Retrocamera (suite TS): bc com.ts.rearview_state, extra show_state. */
    const val REARVIEW_ID = "bc:com.ts.rearview_state|show_state"

    /** true = in retromarcia, false = no, null = segnale mai visto. */
    fun reverseOn(signals: Map<String, String>): Boolean? = when (signals[REVERSE_ID]) {
        null -> null
        "1" -> true
        else -> false
    }

    fun rearviewOn(signals: Map<String, String>): Boolean? = when (signals[REARVIEW_ID]) {
        null -> null
        "1" -> true
        else -> false
    }

    /** Telefono collegato al vivavoce della radio (stack Bluetooth TS, non quello Android). */
    const val PHONE_HFP_ID = "bc:com.globalconstant.BROADCAST_RESPONSE_HFPSTATUS|hfpStatus"

    /** Proiezione wireless (CarPlay/Android Auto tramite SpeedPlay) attiva. */
    const val PROJECTION_ID = "prop:sys.speedplay.connection.state"

    /** true = telefono collegato (hfpStatus diverso da 0), null = mai visto. */
    fun phoneConnected(signals: Map<String, String>): Boolean? = when (signals[PHONE_HFP_ID]) {
        null -> null
        "0" -> false
        else -> true
    }

    fun projectionActive(signals: Map<String, String>): Boolean = signals[PROJECTION_ID] == "1"
}

/** Un valore sempre visibile sul cruscotto (es. carburante, temperatura). */
data class Gauge(val label: String, val value: String, val unit: String)

/** Una spia attiva in questo momento (es. "Fari guasti", "Pressione pneumatici"). */
data class ActiveAlert(val label: String)

/**
 * Calcola, dai segnali letti e dai collegamenti scelti dall'utente:
 *  - i valori sempre visibili (gauge);
 *  - le spie attive ADESSO (le altre non si mostrano).
 * Niente dati finti: se un valore non c'è, resta "—" e la spia non compare.
 */
object VehicleStatus {

    fun gauges(bindings: List<SignalBinding>, signals: Map<String, String>): List<Gauge> =
        bindings.filter { it.role == SignalRole.GAUGE }.map { b ->
            Gauge(b.label, signals[b.id] ?: "—", b.unit)
        }

    /**
     * Una spia è "attiva" quando il suo segnale vale onValue (stato di guasto/avviso);
     * se onValue non è definito, è attiva quando il valore è diverso da offValue (non-normale).
     */
    fun activeAlerts(bindings: List<SignalBinding>, signals: Map<String, String>): List<ActiveAlert> =
        bindings.filter { it.role == SignalRole.ALERT }.mapNotNull { b ->
            val v = signals[b.id] ?: return@mapNotNull null
            val active = when {
                b.onValue != null -> v == b.onValue
                b.offValue != null -> v != b.offValue
                else -> false
            }
            if (active) ActiveAlert(b.label) else null
        }

    /**
     * Spie/eventi riconosciuti automaticamente su questo firmware (oltre a quelle mappate a mano).
     * Solo stati reali e attivi ADESSO: niente falsi allarmi.
     */
    fun knownActiveAlerts(signals: Map<String, String>): List<ActiveAlert> =
        KnownCatalog.entries
            .filter { it.role == KnownRole.ALERT && it.onHome && signals[it.id] == it.onValue }
            .map { ActiveAlert(it.label) }

    /** Stati e valori identificati su questo firmware, mostrati di default sulla Home. */
    fun knownStatus(signals: Map<String, String>): List<Gauge> =
        KnownCatalog.entries
            .filter { it.onHome && it.role != KnownRole.ALERT && signals[it.id] != null }
            .map { k ->
                val v = signals[k.id]!!
                val text = when (k.role) {
                    KnownRole.STATUS -> if (v == k.onValue) k.yesText else k.noText
                    else -> v
                }
                Gauge(k.label, text, if (k.role == KnownRole.VALUE) k.unit else "")
            }

    /** Stato fari: dal collegamento esplicito, altrimenti dai segnali noti automatici. */
    fun lightsOn(lightsBinding: SignalBinding?, signals: Map<String, String>): Boolean? {
        if (lightsBinding != null) {
            val v = signals[lightsBinding.id]
            if (v != null) {
                if (v == lightsBinding.onValue) return true
                if (v == lightsBinding.offValue) return false
                if (lightsBinding.offValue != null) return true
            }
        }
        return KnownVehicleSignals.lightsOn(signals)
    }
}

/** Catalogo spie standard di un'auto, abilitate di default. Si collegano a un segnale quando c'è. */
object AlertCatalog {
    val standard = listOf(
        "Fari guasti", "Pressione pneumatici", "Pressione olio", "Temperatura motore",
        "Avaria motore", "Batteria", "ABS", "Airbag", "Cintura non allacciata",
        "Porta aperta", "Liquido lavavetri", "Freno a mano",
    )
}

/** Come va interpretato un segnale identificato. */
enum class KnownRole { STATUS, ALERT, VALUE }

data class Known(
    val label: String,
    val id: String,
    val role: KnownRole,
    /** per ALERT: valore che indica "attivo"; per STATUS booleano: valore che indica "sì". */
    val onValue: String? = null,
    val yesText: String = "sì",
    val noText: String = "no",
    val unit: String = "",
    /** se mostrarlo sulla striscia Home (alcuni sono solo diagnostici, utili nell'export). */
    val onHome: Boolean = true,
)

/**
 * Tutti i segnali del veicolo che DriveOS ha identificato su QUESTA radio (ForFan/TS),
 * dai tuoi export e dal registro di sistema. Attivi di default: compaiono da soli,
 * senza bisogno di flag. Un solo elenco alimenta Home, cruscotto ed export.
 */
object KnownCatalog {
    val entries = listOf(
        // Fari li mostra già la pill dedicata nella striscia: qui solo per l'export (onHome=false).
        Known("Fari", KnownVehicleSignals.lights.first().id, KnownRole.STATUS, onValue = "on", yesText = "accesi", noText = "spenti", onHome = false),
        Known("Retromarcia", KnownVehicleSignals.REVERSE_ID, KnownRole.ALERT, onValue = "1"),
        Known("Retrocamera", KnownVehicleSignals.REARVIEW_ID, KnownRole.STATUS, onValue = "1", yesText = "attiva", noText = "no"),
        Known("Portiera", LogcatParser.DOOR, KnownRole.ALERT, onValue = "aperta"),
        Known("Sensori parcheggio", LogcatParser.PARKING, KnownRole.ALERT, onValue = "1"),
        Known("Luci (ADAS)", LogcatParser.LIGHTS, KnownRole.STATUS, onValue = "on", yesText = "accese", noText = "spente", onHome = false),
        Known("Telefono", KnownVehicleSignals.PHONE_HFP_ID, KnownRole.STATUS, onValue = "1", yesText = "collegato", noText = "scollegato"),
        Known("CarPlay/AA", KnownVehicleSignals.PROJECTION_ID, KnownRole.STATUS, onValue = "1", yesText = "attivo", noText = "no"),
        Known("Radio FM", LogcatParser.RADIO_FREQ, KnownRole.VALUE, unit = "kHz"),
        Known("Navigatore di sistema", KnownVehicleSignals.SYSTEM_NAV_PROP, KnownRole.VALUE, onHome = false),
        Known("App in primo piano", "prop:forfan.want2start", KnownRole.VALUE, onHome = false),
    )

    /** Valore grezzo corrente del segnale, se presente. */
    fun rawValue(k: Known, signals: Map<String, String>): String? = signals[k.id]

    /** Elenco (etichetta, valore leggibile o "—") per TUTTI gli identificati: per l'export e la vista completa. */
    fun identified(signals: Map<String, String>): List<Pair<String, String>> = entries.map { k ->
        val v = signals[k.id]
        val text = when {
            v == null -> "—"
            k.role == KnownRole.STATUS -> if (v == k.onValue) k.yesText else k.noText
            k.role == KnownRole.VALUE -> v + (if (k.unit.isNotBlank()) " ${k.unit}" else "")
            else -> v
        }
        k.label to text
    }
}

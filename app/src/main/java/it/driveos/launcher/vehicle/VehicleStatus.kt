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
    fun knownActiveAlerts(signals: Map<String, String>): List<ActiveAlert> {
        val out = ArrayList<ActiveAlert>()
        if (KnownVehicleSignals.reverseOn(signals) == true) out.add(ActiveAlert("Retromarcia"))
        return out
    }

    /** Stati sempre utili riconosciuti su questo firmware (telefono, CarPlay/AA). */
    fun knownStatus(signals: Map<String, String>): List<Gauge> {
        val out = ArrayList<Gauge>()
        KnownVehicleSignals.phoneConnected(signals)?.let { out.add(Gauge("Telefono", if (it) "collegato" else "scollegato", "")) }
        if (KnownVehicleSignals.projectionActive(signals)) out.add(Gauge("CarPlay/AA", "attivo", ""))
        return out
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

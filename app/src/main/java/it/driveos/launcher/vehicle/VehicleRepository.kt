package it.driveos.launcher.vehicle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Legge lo stato del veicolo con sole API pubbliche e senza permessi speciali:
 *  - proprieta' di sistema (getprop, via reflection su android.os.SystemProperties, pubbliche);
 *  - impostazioni Android (Settings.System/Global/Secure);
 *  - alcuni file di sistema leggibili (/sys/class/..., se il firmware li espone);
 *  - broadcast: ascolta le azioni di sistema e quelle "candidate" trovate nelle app Junsun.
 *
 * Non scrive nulla, non tocca MCU/CAN: osserva soltanto cio' che il firmware gia' pubblica.
 * Se un firmware non espone nulla di utile, la schermata Veicolo resta vuota e il tema
 * automatico ricade su alba/tramonto: nessun dato viene inventato.
 */
class VehicleRepository(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _signals = MutableStateFlow<Map<String, String>>(emptyMap())
    /** Ultimo valore noto di ogni segnale (id → valore). */
    val signals: StateFlow<Map<String, String>> = _signals.asStateFlow()

    private val _changeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val changeCounts: StateFlow<Map<String, Int>> = _changeCounts.asStateFlow()

    /** Azioni broadcast attualmente ascoltate (sistema + candidate dell'utente). */
    @Volatile private var listenedActions: Set<String> = emptySet()
    private var receiver: BroadcastReceiver? = null

    private val receiverImpl = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            val action = intent.action ?: return
            bump(SignalIds.broadcastCount(action), "")
            intent.extras?.let { putExtras(action, it) }
        }
    }

    fun start() {
        if (receiver == null) {
            receiver = receiverImpl
            updateListenedActions(ActionFilter.systemActions.toSet())
        }
        scope.launch { pollOnce() }
    }

    fun stop() {
        receiver?.let { runCatching { app.unregisterReceiver(it) } }
        receiver = null
    }

    /** Aggiorna l'elenco di azioni da ascoltare (es. dopo che l'utente ha avviato la scansione). */
    fun updateListenedActions(actions: Set<String>) {
        val all = (ActionFilter.systemActions + actions).toSet()
        if (all == listenedActions && receiver != null) return
        listenedActions = all
        receiver?.let { runCatching { app.unregisterReceiver(it) } }
        val r = receiverImpl
        receiver = r
        val filter = IntentFilter().apply { all.forEach { addAction(it) } }
        runCatching {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            app.registerReceiver(r, filter)
        }
    }

    /** Legge una volta proprieta', impostazioni e file; aggiorna i segnali. */
    fun pollOnce() {
        val snap = LinkedHashMap<String, String>()
        SystemProps.all().forEach { (k, v) -> snap[SignalIds.prop(k)] = v }
        readSettings(snap)
        readFiles(snap)
        merge(snap)
    }

    /** Istantanea completa e sincrona (per la registrazione "accendi/spegni i fari"). */
    fun snapshot(): Map<String, String> {
        pollOnce()
        return _signals.value
    }

    private fun readSettings(out: MutableMap<String, String>) {
        val cr = app.contentResolver
        INTERESTING_SETTINGS.forEach { (table, key) ->
            val value = runCatching {
                when (table) {
                    "system" -> Settings.System.getString(cr, key)
                    "global" -> Settings.Global.getString(cr, key)
                    "secure" -> Settings.Secure.getString(cr, key)
                    else -> null
                }
            }.getOrNull()
            if (value != null) out[SignalIds.setting(table, key)] = value
        }
    }

    private fun readFiles(out: MutableMap<String, String>) {
        INTERESTING_FILES.forEach { path ->
            runCatching {
                val f = File(path)
                if (f.canRead() && f.length() in 1..200) out[SignalIds.file(path)] = f.readText().trim()
            }
        }
    }

    private fun putExtras(action: String, extras: Bundle) {
        val updates = LinkedHashMap<String, String>()
        for (key in extras.keySet()) {
            val value = runCatching { ExtraFormat.stringify(extras.get(key)) }.getOrNull() ?: continue
            updates[SignalIds.broadcast(action, key)] = value
        }
        if (updates.isNotEmpty()) merge(updates)
    }

    private fun bump(id: String, value: String) {
        val counts = _changeCounts.value.toMutableMap()
        counts[id] = (counts[id] ?: 0) + 1
        _changeCounts.value = counts
        merge(mapOf(id to ((_signals.value[id]?.toIntOrNull() ?: 0) + 1).toString()))
    }

    private fun merge(updates: Map<String, String>) {
        val current = _signals.value
        var changed = false
        val next = current.toMutableMap()
        val counts = _changeCounts.value.toMutableMap()
        for ((k, v) in updates) {
            if (current[k] != v) {
                if (current.containsKey(k)) counts[k] = (counts[k] ?: 0) + 1
                next[k] = v
                changed = true
            }
        }
        if (changed) {
            _signals.value = next
            _changeCounts.value = counts
        }
    }

    companion object {
        private val INTERESTING_SETTINGS = listOf(
            "system" to "screen_brightness",
            "system" to "screen_brightness_mode",
            "global" to Settings.Global.DEVICE_PROVISIONED,
            "secure" to "ui_night_mode",
            "system" to "ui_night_mode",
            "global" to "night_mode",
        )

        /** Percorsi comuni su radio MTK/SPM dove a volte compaiono stato fari/retro/accensione. */
        private val INTERESTING_FILES = listOf(
            "/sys/class/switch/lightsensor/state",
            "/sys/class/display/night_mode",
            "/sys/kernel/mcu/ill",
            "/sys/kernel/canbus/illume",
            "/sys/devices/platform/reverse/state",
            "/proc/mtk_mcu/illumination",
        )

        /**
         * Canali veicolo noti di QUESTA radio (ForFan/Hazens, suite TS/TXZ), ricavati
         * dall'export reale. Ascoltati sempre, anche senza una nuova scansione.
         */
        val knownVehicleActions = listOf(
            // Dati CAN (il più importante: porta carburante/temperature/porte…)
            "com.ts.can.BROADCAST_CAN_INFO",
            "com.ts.can.BROADCAST_CAN_UPT",
            "com.ts.can.ac",
            "com.ts.can.device",
            "com.ts.can.excd",
            "com.ts.can.BROADCAST_CAMERA_KEY",
            // Eventi veicolo
            "com.forfan.operator_reversing", // retromarcia reale (extra "operate": 1=inserita)
            "com.ts.rearview_state",         // retrocamera (extra show_state)
            "com.globalconstant.BROADCAST_RESPONSE_HFPSTATUS", // telefono vivavoce (hfpStatus, name, number)
            "com.ts.bt.CONNECT_STATE_CHANGE",
            "comm.power.ENTER_REVERSE",
            "comm.power.QUIT_REVERSE",
            "com.cusc.action.ACC_OFF",
            "forfan.adas.turnlight_state",
            "com.autoai.iot.tpms",
            "vendor.hazens.vehicle.info.vin",
            // Avvisi di sistema MTK
            "mediatek.intent.action.BATTERY_WARNING",
            "mediatek.intent.action.THERMAL_WARNING",
            // Radio (suite media TS)
            "com.ts.main.radio",
        )

        /**
         * Canali che arrivano di continuo ma non dicono nulla sull'auto (battito della voce TXZ,
         * modem cellulare). Nel primo registro erano 660 righe su 2050: esclusi per un file pulito.
         */
        val noisyActions = setOf(
            "com.txznet.txz.heartbeat",
            "com.android.ims.IMS_SERVICE_UP",
            "com.mediatek.ims.MTK_IMS_SERVICE_UP",
            "com.mediatek.intent.action.RADIO_STATE_CHANGED",
            "com.mediatek.intent.action.LOCATED_PLMN_CHANGED",
            "com.mediatek.intent.action.ACTION_NETWORK_REJECT_CAUSE",
        )

        /** Tutte le azioni broadcast sensate da ascoltare (sistema + note + scoperte), senza rumore. */
        fun allListenActions(discovered: Collection<String>): Set<String> =
            (ActionFilter.systemActions + knownVehicleActions + discovered + listOf(
                "com.ts.car.client.ACTION_CHANGED_LOG_STATE",
                "com.ts.car.client.ACTION_REMOTE_SLEEP",
                "com.ts.car.client.ACTION_REMOTE_WAKEUP",
                "com.ts.car.client.ACTION_RESPONSE_AVM_PICTURE",
            )).toSet() - noisyActions

        /**
         * Legge, in modo indipendente dall'istanza, proprietà + impostazioni + file di sistema.
         * Usata dal servizio di registrazione continua.
         */
        fun readStatic(context: Context): Map<String, String> {
            val out = LinkedHashMap<String, String>()
            SystemProps.all().forEach { (k, v) -> out[SignalIds.prop(k)] = v }
            val cr = context.applicationContext.contentResolver
            INTERESTING_SETTINGS.forEach { (table, key) ->
                val value = runCatching {
                    when (table) {
                        "system" -> Settings.System.getString(cr, key)
                        "global" -> Settings.Global.getString(cr, key)
                        "secure" -> Settings.Secure.getString(cr, key)
                        else -> null
                    }
                }.getOrNull()
                if (value != null) out[SignalIds.setting(table, key)] = value
            }
            INTERESTING_FILES.forEach { path ->
                runCatching {
                    val f = File(path)
                    if (f.canRead() && f.length() in 1..200) out[SignalIds.file(path)] = f.readText().trim()
                }
            }
            return out
        }
    }
}

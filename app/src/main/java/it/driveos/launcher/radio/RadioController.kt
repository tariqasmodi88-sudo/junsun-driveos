package it.driveos.launcher.radio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Radio integrata in DriveOS (non apre l'app Junsun). Fa due cose:
 *
 *  LETTURA  — si mette in ascolto dei broadcast che il firmware pubblica sullo stato della
 *             radio e ne ricava frequenza e stato play/pause. Se il firmware non pubblica
 *             nulla, lo stato resta sconosciuto: NON viene mostrata una frequenza finta.
 *
 *  COMANDO  — invia i comandi (sintonizza, cerca, on/off) provando un insieme di "protocolli"
 *             noti dei firmware MTK/FYT, più le azioni trovate dalla scansione veicolo. È
 *             sperimentale: se nessun protocollo è quello giusto per questa radio, i comandi
 *             non hanno effetto (e la frequenza letta non cambia), senza danni.
 *
 * Quando scopriamo (dall'export) l'azione e l'extra esatti di questa radio, bastano poche
 * righe in [controlProfiles] per renderla pienamente funzionante.
 */
class RadioController(context: Context) {
    private val app = context.applicationContext

    private val _state = MutableStateFlow<RadioState?>(null)
    val state: StateFlow<RadioState?> = _state.asStateFlow()

    /** true se abbiamo mai ricevuto uno stato reale dal firmware (frequenza o play). */
    private val _detected = MutableStateFlow(false)
    val detected: StateFlow<Boolean> = _detected.asStateFlow()

    private var receiver: BroadcastReceiver? = null
    @Volatile private var extraActions: Set<String> = emptySet()

    private val impl = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) = onBroadcast(intent)
    }

    fun start(discoveredRadioActions: Set<String> = emptySet()) {
        extraActions = discoveredRadioActions
        receiver?.let { runCatching { app.unregisterReceiver(it) } }
        val actions = (listenActions + discoveredRadioActions).toSet()
        val filter = IntentFilter().apply { actions.forEach { addAction(it) } }
        receiver = impl
        runCatching {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            app.registerReceiver(impl, filter)
        }
    }

    fun stop() {
        receiver?.let { runCatching { app.unregisterReceiver(it) } }
        receiver = null
    }

    private fun onBroadcast(intent: Intent) {
        val extras = intent.extras ?: return
        var khz: Int? = null
        var playing: Boolean? = null
        for (key in extras.keySet()) {
            val raw = runCatching { extras.get(key)?.toString() }.getOrNull() ?: continue
            if (khz == null && RadioSignals.looksLikeFrequencyExtra(key)) khz = RadioSignals.parseFrequencyKhz(raw)
            if (playing == null && RadioSignals.looksLikePlayingExtra(key)) playing = RadioSignals.parsePlaying(raw)
        }
        if (khz == null && playing == null) return
        val prev = _state.value
        _state.value = RadioState(
            khz = khz ?: prev?.khz,
            playing = playing ?: prev?.playing,
            source = intent.action.orEmpty(),
        )
        _detected.value = true
    }

    // --- Comandi (best-effort) ---

    fun tune(khz: Int) = broadcastControl("TUNE", khz)
    fun seekUp() = broadcastControl("SEEK_UP", null)
    fun seekDown() = broadcastControl("SEEK_DOWN", null)
    fun stepUp() = broadcastControl("STEP_UP", null)
    fun stepDown() = broadcastControl("STEP_DOWN", null)
    fun power(on: Boolean) = broadcastControl(if (on) "POWER_ON" else "POWER_OFF", null)

    /**
     * Invia il comando con ogni profilo noto. Ogni profilo definisce il nome dell'azione e,
     * per la sintonizzazione, il nome e il formato dell'extra della frequenza.
     */
    private fun broadcastControl(command: String, khz: Int?) {
        controlProfiles.forEach { profile ->
            val action = profile.actions[command] ?: return@forEach
            runCatching {
                val intent = Intent(action).setPackage(profile.targetPackage)
                if (khz != null) profile.putFrequency(intent, khz)
                app.sendBroadcast(intent)
                // alcuni firmware ascoltano senza package esplicito
                app.sendBroadcast(Intent(action).also { if (khz != null) profile.putFrequency(it, khz) })
            }
        }
    }

    private companion object {
        /** Azioni da ascoltare per leggere lo stato (unione con quelle scoperte dalla scansione). */
        val listenActions = listOf(
            "com.fyt.radio.STATE", "com.fyt.fmradio.STATE", "com.fyt.FM_STATE",
            "com.microntek.radio.STATE", "com.syu.radio.STATE",
            "android.intent.action.FM_STATE", "com.android.fmradio.STATE",
            "com.hzc.radio.STATE", "com.mtk.radio.STATE",
        )

        /** Profili di comando noti (best-effort). Da raffinare con l'export della radio reale. */
        val controlProfiles = listOf(
            ControlProfile(
                targetPackage = "com.fyt.fmradio",
                freqExtra = "freq", freqFormat = FreqFormat.KHZ,
                actions = mapOf(
                    "TUNE" to "com.fyt.fmradio.ACTION_TUNE",
                    "SEEK_UP" to "com.fyt.fmradio.ACTION_SEEK_UP",
                    "SEEK_DOWN" to "com.fyt.fmradio.ACTION_SEEK_DOWN",
                    "STEP_UP" to "com.fyt.fmradio.ACTION_STEP_UP",
                    "STEP_DOWN" to "com.fyt.fmradio.ACTION_STEP_DOWN",
                    "POWER_ON" to "com.fyt.fmradio.ACTION_OPEN",
                    "POWER_OFF" to "com.fyt.fmradio.ACTION_CLOSE",
                ),
            ),
            ControlProfile(
                targetPackage = "com.microntek.radio",
                freqExtra = "freq", freqFormat = FreqFormat.MHZ_X100,
                actions = mapOf(
                    "TUNE" to "com.microntek.radio.tune",
                    "SEEK_UP" to "com.microntek.radio.seekup",
                    "SEEK_DOWN" to "com.microntek.radio.seekdown",
                ),
            ),
        )
    }

    enum class FreqFormat { KHZ, MHZ_X100 }

    data class ControlProfile(
        val targetPackage: String,
        val freqExtra: String,
        val freqFormat: FreqFormat,
        val actions: Map<String, String>,
    ) {
        fun putFrequency(intent: Intent, khz: Int) {
            val value = when (freqFormat) {
                FreqFormat.KHZ -> khz
                FreqFormat.MHZ_X100 -> khz / 10 // 101500 kHz → 10150
            }
            intent.putExtra(freqExtra, value)
        }
    }
}

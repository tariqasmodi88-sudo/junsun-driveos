package it.driveos.launcher.junsun

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.ts.tsspeechlib.function.ITsSpeechFunction
import com.ts.tsspeechlib.radio.ITsSpeechRadio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ponte verso i servizi che l'app Junsun (com.ts.MainUI) offre al comando vocale: radio e funzioni di
 * sistema (volume, schermo…). Sono gli stessi servizi che usa l'assistente vocale installato sulla radio,
 * quindi non serve nessun permesso particolare. Se non si collega, DriveOS usa i ripieghi.
 */
class TsBridge(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var radio: ITsSpeechRadio? = null
    @Volatile private var function: ITsSpeechFunction? = null

    /** Ultimo valore grezzo di frequenza letto: serve a capire l'unità (10 kHz, kHz…). */
    @Volatile var rawSample: Int? = null

    val radioConnected get() = radio != null
    val functionConnected get() = function != null

    private val radioConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) { radio = ITsSpeechRadio.Stub.asInterface(service) }
        override fun onServiceDisconnected(name: ComponentName?) { radio = null }
    }
    private val functionConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) { function = ITsSpeechFunction.Stub.asInterface(service) }
        override fun onServiceDisconnected(name: ComponentName?) { function = null }
    }

    /** Si collega (o riprova). Sicuro da chiamare più volte. */
    fun connect() {
        if (radio == null) bind(RADIO_ACTION, radioConn)
        if (function == null) bind(FUNCTION_ACTION, functionConn)
    }

    private fun bind(action: String, conn: ServiceConnection) {
        runCatching { app.bindService(Intent(action).setPackage(PKG), conn, Context.BIND_AUTO_CREATE) }
    }

    /** Esegue un comando radio senza bloccare l'interfaccia. false se non collegato (e riprova a collegarsi). */
    fun radioCall(block: (ITsSpeechRadio) -> Unit): Boolean {
        val r = radio ?: run { connect(); return false }
        scope.launch { runCatching { block(r) } }
        return true
    }

    fun functionCall(block: (ITsSpeechFunction) -> Unit): Boolean {
        val f = function ?: run { connect(); return false }
        scope.launch { runCatching { block(f) } }
        return true
    }

    /** Sintonizza una FM in kHz. */
    fun tuneFm(khz: Int): Boolean { val raw = fromKhz(khz, rawSample); return radioCall { it.TurnBandAndFq(0, raw) } }

    /** Frequenza grezza e banda grezza della radio, o null se non collegata. */
    suspend fun radioInfo(): Pair<Int, Int>? {
        val r = radio ?: run { connect(); return null }
        return withContext(Dispatchers.IO) { runCatching { r.GetRaidoFreq() to r.getRadioBand() }.getOrNull() }
            ?.also { if (toKhz(it.first) != null) rawSample = it.first }
    }

    /** La radio riceve dati RDS dalla stazione (indicazione ufficiale del servizio radio). null = non collegata. */
    suspend fun rdsOn(): Boolean? {
        val r = radio ?: return null
        return withContext(Dispatchers.IO) { runCatching { r.bRdsOn() }.getOrNull() }
    }

    companion object {
        const val PKG = "com.ts.MainUI"
        const val RADIO_ACTION = "com.ts.tsspeechlib.radio.TsRadioService"
        const val FUNCTION_ACTION = "com.ts.tsspeechlib.function.TsFunctionService"

        /** Il valore grezzo può essere 9200 (10 kHz), 92000 (kHz) o 920 (100 kHz): lo riportiamo a kHz FM. */
        fun toKhz(raw: Int): Int? = when (raw) {
            in 87_500..108_000 -> raw
            in 8_750..10_800 -> raw * 10
            in 875..1_080 -> raw * 100
            else -> null
        }

        /** Inverso di [toKhz] per lo stesso formato del valore grezzo letto. */
        fun fromKhz(khz: Int, rawSample: Int?): Int = when (rawSample) {
            in 87_500..108_000 -> khz
            in 875..1_080 -> khz / 100
            else -> khz / 10
        }
    }
}

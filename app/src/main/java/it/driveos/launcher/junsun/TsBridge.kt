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
    // --- Servizio principale della radio Junsun (com.ts.main.common.MainUI, interfaccia ITsCommon) ---
    // Da qui si ottiene il binder interno della radio (ITsRadioCommon) che espone il nome RDS
    // della stazione corrente: Radio_GetPsName (transazione 30). Ricavato dall'APK MainUI.
    @Volatile private var common: IBinder? = null
    @Volatile private var radioCommon: IBinder? = null
    @Volatile var commonError: String? = null
        private set
    private val commonConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) { common = service; radioCommon = null }
        override fun onServiceDisconnected(name: ComponentName?) { common = null; radioCommon = null }
    }

    private fun bindCommon() {
        if (common != null) return
        val ok = runCatching { app.bindService(Intent("android.intent.action.MAIN_UI").setPackage(PKG), commonConn, Context.BIND_AUTO_CREATE) }.getOrDefault(false)
        if (!ok) runCatching {
            app.bindService(Intent().setClassName(PKG, "com.ts.main.common.MainUI"), commonConn, Context.BIND_AUTO_CREATE)
        }.onFailure { commonError = it.message }
    }

    private fun radioCommonBinder(): IBinder? {
        radioCommon?.let { if (it.isBinderAlive) return it }
        val c = common ?: run { bindCommon(); return null }
        val data = android.os.Parcel.obtain(); val reply = android.os.Parcel.obtain()
        return try {
            data.writeInterfaceToken("com.ts.main.common.ITsCommon")
            data.writeString("Radio")
            c.transact(55, data, reply, 0) // getSpecialBinder("Radio")
            reply.readException()
            reply.readStrongBinder().also { radioCommon = it; commonError = null }
        } catch (t: Throwable) { commonError = t.message ?: t.javaClass.simpleName; null }
        finally { data.recycle(); reply.recycle() }
    }

    /** Nome RDS (PS) della stazione sintonizzata, direttamente dalla radio Junsun. null se non disponibile. */
    fun psName(): String? {
        val r = radioCommonBinder() ?: return null
        val data = android.os.Parcel.obtain(); val reply = android.os.Parcel.obtain()
        return try {
            data.writeInterfaceToken("com.ts.main.common.ITsRadioCommon")
            r.transact(30, data, reply, 0) // Radio_GetPsName
            reply.readException()
            reply.readString()
        } catch (t: Throwable) { commonError = t.message ?: t.javaClass.simpleName; radioCommon = null; null }
        finally { data.recycle(); reply.recycle() }
    }

    fun connect() {
        bindCommon()
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
    /** Chiamate radio/funzioni che aspettano la fine (servono in sequenza). */
    suspend fun radioNow(block: (ITsSpeechRadio) -> Unit): Boolean {
        val r = radio ?: run { connect(); return false }
        return withContext(Dispatchers.IO) { runCatching { block(r) }.isSuccess }
    }
    suspend fun isMuted(): Boolean? {
        val f = function ?: return null
        return withContext(Dispatchers.IO) { runCatching { f.isVolumeMute() }.getOrNull() }
    }
    suspend fun setMuted(on: Boolean) {
        val f = function ?: return
        withContext(Dispatchers.IO) { runCatching { if (on) f.onVolumeMute() else f.onVolumeUnmute() } }
    }
    /** 0 nessuna, 1 radio, 4 musica, 5 musica bluetooth. */
    suspend fun workMode(): Int? {
        val f = function ?: return null
        return withContext(Dispatchers.IO) { runCatching { f.getWorkMode() }.getOrNull() }
    }

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

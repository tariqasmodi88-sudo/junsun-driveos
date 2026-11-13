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
        if (!it.driveos.launcher.system.PowerGate.ready()) return null
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

    // NOTA: il CarInfoService di MainUI (requestCarDoorInfo) NON va interrogato: chiama la libreria
    // nativa libcan59 da un altro thread e fa crollare MainUI ("JNI ERROR: accessed stale Local",
    // registro del 07/11, CanJni.GetCarInfoAidl). Provato nella 1.1.4 e tolto.

    /** Spegne/riaccende la retroilluminazione come il tasto accensione della radio (ITsCommon.BklTurn, transazione 22 -> Mcu.BklTurn). */
    fun backlightToggle(): Boolean {
        if (!it.driveos.launcher.system.PowerGate.ready()) return false
        val c = common ?: run { bindCommon(); return false }
        val data = android.os.Parcel.obtain(); val reply = android.os.Parcel.obtain()
        return try {
            data.writeInterfaceToken("com.ts.main.common.ITsCommon")
            c.transact(22, data, reply, 0)
            reply.readException(); true
        } catch (t: Throwable) { commonError = t.message; false }
        finally { data.recycle(); reply.recycle() }
    }

    // --- Servizio "auto" dell'assistente vocale Junsun (com.ts.tsspeechlib.car.TsCarService) ---
    // PROVA (13/11): MainUI tiene in memoria porte, cofano, sensori e frecce e li offre all'assistente
    // vocale con questo servizio. A differenza del CarInfoService (vedi NOTA sopra) NON chiama la libreria
    // CAN: legge solo variabili gia' aggiornate da MainUI (Can.mCarMsg, Can.mRadarForeInfo/RearInfo).
    // Se in auto i valori risultano giusti, DriveOS potra' lasciare la seriale alla radio nella guida normale.
    // Per ora si usa SOLO dalla pagina Debug veicolo, a richiesta.
    @Volatile private var car: IBinder? = null
    private val carConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) { car = service }
        override fun onServiceDisconnected(name: ComponentName?) { car = null }
    }
    @Volatile private var carBound = false

    fun carConnect() {
        if (!it.driveos.launcher.system.PowerGate.ready() || carBound) return
        carBound = runCatching { app.bindService(Intent(CAR_ACTION).setPackage(PKG), carConn, Context.BIND_AUTO_CREATE) }.getOrDefault(false)
        if (function == null) bind(FUNCTION_ACTION, functionConn)
    }

    fun carDisconnect() {
        if (carBound) runCatching { app.unbindService(carConn) }
        carBound = false; car = null
    }

    /** Valori letti dal servizio auto di MainUI. Ogni campo e' null se la chiamata non e' riuscita. */
    class CarProbe(
        /** [ant.sx, ant.dx, post.sx, post.dx, baule, cofano] (1 = aperta). */
        val doors: List<Int>?,
        /** [4 anteriori, 4 posteriori], da sinistra a destra. */
        val radar: List<Int>?,
        val steer: Int?, val left: Int?, val right: Int?, val emergency: Int?,
        val ill: Int?, val reverse: Int?, val brake: Int?,
        val error: String?,
    )

    private fun carCall(code: Int, array: Boolean): Any? {
        val c = car ?: return null
        val data = android.os.Parcel.obtain(); val reply = android.os.Parcel.obtain()
        return try {
            data.writeInterfaceToken("com.ts.tsspeechlib.car.ITsSpeechCar")
            c.transact(code, data, reply, 0)
            reply.readException()
            if (array) reply.createIntArray()?.toList() else reply.readInt()
        } finally { data.recycle(); reply.recycle() }
    }

    /** Legge tutto una volta (bloccante: da un thread di sfondo). Numeri di chiamata ricavati dall'APK MainUI. */
    fun carProbe(): CarProbe {
        if (!it.driveos.launcher.system.PowerGate.ready()) return CarProbe(null, null, null, null, null, null, null, null, null, "radio non pronta")
        if (car == null) { carConnect(); return CarProbe(null, null, null, null, null, null, null, null, null, "servizio non ancora collegato") }
        var err: String? = null
        fun <T> safe(block: () -> T?): T? = try { block() } catch (t: Throwable) { err = t.message ?: t.javaClass.simpleName; null }
        @Suppress("UNCHECKED_CAST")
        val doors = safe { carCall(24, true) as? List<Int> }
        @Suppress("UNCHECKED_CAST")
        val radar = safe { carCall(25, true) as? List<Int> }
        val f = function
        return CarProbe(
            doors = doors, radar = radar,
            steer = safe { carCall(23, false) as? Int },
            left = safe { carCall(19, false) as? Int },
            right = safe { carCall(20, false) as? Int },
            emergency = safe { carCall(21, false) as? Int },
            ill = safe { f?.isIllOn() }, reverse = safe { f?.isBackCar() }, brake = safe { f?.withBrakes() },
            error = err,
        )
    }

    fun connect() {
        if (!it.driveos.launcher.system.PowerGate.ready()) return
        bindCommon()
        if (radio == null) bind(RADIO_ACTION, radioConn)
        if (function == null) bind(FUNCTION_ACTION, functionConn)
    }

    /** Scollega tutto dai servizi Junsun (quadro spento): la radio puo' andare in standby libera. */
    fun disconnect() {
        listOf(radioConn, functionConn, commonConn).forEach { c -> runCatching { app.unbindService(c) } }
        carDisconnect()
        radio = null; function = null; common = null; radioCommon = null
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
        const val CAR_ACTION = "com.ts.tsspeechlib.car.TsCarService"

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

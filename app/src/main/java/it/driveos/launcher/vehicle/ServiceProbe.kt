package it.driveos.launcher.vehicle

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Prova a collegarsi (bindService) ai servizi che le app del firmware DICHIARANO pubblici
 * (exported=true). È una normale comunicazione tra app su Android: serve a scoprire il NOME
 * dell'interfaccia di ciascun servizio (es. CarInfoService, TsRadioService), che è il dato
 * mancante per leggere da lì i valori del veicolo o comandare la radio.
 *
 * Non forza nulla: se il servizio non accetta il collegamento, viene semplicemente segnato
 * come "non raggiungibile". Diagnostica, incluso nell'export.
 */
object ServiceProbe {

    /** Servizi esportati interessanti su questa radio (dall'export dei componenti). */
    val targets = listOf(
        "com.ts.MainUI" to "com.ts.can.carinfo.CarInfoService",
        "com.ts.MainUI" to "com.ts.can.CanDataLogService",
        "com.ts.MainUI" to "com.ts.tsspeechlib.radio.TsRadioService",
        "com.ts.MainUI" to "com.ts.tsspeechlib.car.TsCarService",
        "com.ts.MainUI" to "com.ts.tsspeechlib.function.TsFunctionService",
        "com.ts.MainUI" to "com.ts.backcar.BackcarService",
        "com.ts.MainUI" to "com.ts.main.common.MainUI",
        "com.ts.car.client" to "com.ts.car.client.CarClientService",
    )

    data class Result(val component: String, val connected: Boolean, val descriptor: String?, val note: String)

    suspend fun probeAll(context: Context): List<Result> =
        targets.map { (pkg, cls) -> probe(context, pkg, cls) }

    private suspend fun probe(context: Context, pkg: String, cls: String): Result {
        val app = context.applicationContext
        val component = "$pkg/$cls"
        val deferred = CompletableDeferred<IBinder?>()
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (!deferred.isCompleted) deferred.complete(service)
            }
            override fun onServiceDisconnected(name: ComponentName?) {}
            override fun onNullBinding(name: ComponentName?) {
                if (!deferred.isCompleted) deferred.complete(null)
            }
        }
        val intent = Intent().setComponent(ComponentName(pkg, cls))
        val requested = runCatching { app.bindService(intent, conn, Context.BIND_AUTO_CREATE) }.getOrDefault(false)
        if (!requested) {
            runCatching { app.unbindService(conn) }
            return Result(component, false, null, "bind rifiutato (servizio assente o non avviabile)")
        }
        val binder = withTimeoutOrNull(2500) { deferred.await() }
        val descriptor = binder?.let { runCatching { it.interfaceDescriptor }.getOrNull() }
        runCatching { app.unbindService(conn) }
        return when {
            binder == null && !deferred.isCompleted -> Result(component, false, null, "nessuna risposta (timeout)")
            binder == null -> Result(component, false, null, "collegato ma senza interfaccia (null binding)")
            else -> Result(component, true, descriptor, "collegato" + if (descriptor.isNullOrBlank()) "" else " · interfaccia: $descriptor")
        }
    }
}

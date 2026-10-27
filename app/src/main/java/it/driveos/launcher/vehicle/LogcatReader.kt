package it.driveos.launcher.vehicle

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Legge il registro di sistema (logcat) e ne ricava i segnali del veicolo che l'app Junsun
 * `com.ts.MainUI` tiene al suo interno (portiere, sensori, luci, radio). I valori vengono
 * iniettati in [VehicleRepository] e quindi compaiono sulla Home e nel registro continuo.
 *
 * Funziona SOLO se l'utente ha concesso a DriveOS il permesso Android `READ_LOGS`, una volta,
 * via ADB (nessun root). Senza, [available] è false e non legge nulla.
 *
 * Concessione (ADB, anche wireless):
 *   adb shell pm grant it.driveos.launcher android.permission.READ_LOGS
 */
class LogcatReader(context: Context, private val repo: VehicleRepository) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    @Volatile private var process: Process? = null

    /** true se DriveOS ha il permesso di leggere il log di sistema. */
    fun available(): Boolean =
        app.checkSelfPermission(android.Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED

    fun start() {
        if (!available() || job?.isActive == true) return
        job = scope.launch { readLoop() }
    }

    fun stop() {
        job?.cancel()
        runCatching { process?.destroy() }
        process = null
    }

    private suspend fun readLoop() {
        // filtra per tag: molto meno rumore e consumo. "*:S" silenzia tutto il resto.
        val cmd = mutableListOf("logcat", "-v", "brief")
        LogcatParser.tags.forEach { cmd.add("$it:V") }
        cmd.add("*:S")
        runCatching {
            val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
            process = p
            BufferedReader(InputStreamReader(p.inputStream)).use { reader ->
                while (scope.isActive) {
                    val line = reader.readLine() ?: break
                    LogcatParser.parse(line)?.let { (id, value) -> repo.ingest(mapOf(id to value)) }
                }
            }
        }
    }
}

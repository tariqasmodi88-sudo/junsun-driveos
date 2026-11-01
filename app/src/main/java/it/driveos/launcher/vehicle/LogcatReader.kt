package it.driveos.launcher.vehicle

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        if (ticker?.isActive != true) ticker = scope.launch { while (isActive) { tick(); delay(300) } }
    }

    fun stop() {
        job?.cancel()
        runCatching { process?.destroy() }
        process = null
    }

    private var nameSeq = 0L
    private val _names = MutableStateFlow<Map<Int, String>>(emptyMap())
    /** Nomi RDS letti, per frequenza (kHz): il nome e' legato alla frequenza dello stesso registro. */
    val names: StateFlow<Map<Int, String>> = _names
    /** true durante "Leggi nomi": i nomi letti sostituiscono quelli gia' salvati. */
    @Volatile var readingNames = false
    /** Stato RDS della frequenza sintonizzata (SEARCHING / OK / NO_RDS). */
    val tracker = RdsTracker()
    private val _rds = MutableStateFlow(tracker.state)
    val rds: StateFlow<RdsTracker.State> = _rds
    private var ticker: Job? = null
    private val setFreq = Regex("setfreq=(\\d{4,5})")
    /** Si aspetta il nome della stazione gia' sintonizzata (radio nativa appena portata davanti). */
    fun expectName(khz: Int) { tracker.onTune(khz, force = true); stalePs = null; _rds.value = tracker.state }
    /** Frequenza letta dalla radio (binder): reset immediato dello stato RDS se e' cambiata. */
    // La radio Junsun non azzera il nome quando cambi stazione: se la nuova non trasmette RDS
    // continua a restituire quello della precedente. Si accetta quindi solo un nome DIVERSO
    // da quello che la radio dava al momento del cambio.
    @Volatile private var lastLivePs: String? = null
    @Volatile private var stalePs: String? = null
    fun onTuned(khz: Int) { if (tracker.onTune(khz)) { stalePs = lastLivePs; _rds.value = tracker.state } }
    /** Nome PS letto direttamente dalla radio (binder): stesso percorso dei nomi dal registro. */
    fun feedPs(ps: String) {
        val p = ps.trim()
        lastLivePs = p
        if (p.isEmpty() || p == stalePs?.trim()) return
        stalePs = null // la radio ha cambiato nome: da qui in poi e' quello della nuova stazione
        tracker.onPs(p); _rds.value = tracker.state
    }
    fun clearNames() { _names.value = emptyMap() }
    private fun tick() {
        tracker.tick()?.let { (k, n) ->
            // PS che scorre: se per questa frequenza c'e' gia' un nome, si tiene quello
            if (!(tracker.isDynamic && _names.value.containsKey(k))) _names.value = _names.value + (k to n)
        }
        _rds.value = tracker.state
    }
    private var parkingClear: Job? = null

    /** Il log dice solo "sensori attivi": se tace per [LogcatParser.PARKING_TIMEOUT_MS], li segna spenti. */
    private fun armParkingClear() {
        parkingClear?.cancel()
        parkingClear = scope.launch {
            delay(LogcatParser.PARKING_TIMEOUT_MS)
            repo.ingest(mapOf(LogcatParser.PARKING to "0"))
        }
    }

    private suspend fun readLoop() {
        // filtra per tag: molto meno rumore e consumo. "*:S" silenzia tutto il resto.
        // Tutto il log da livello Debug in su: poi si tiene solo ciò che riguarda il veicolo
        // (LogcatParser per i segnali noti).
        val cmd = mutableListOf("logcat", "-v", "brief", "*:V")
        runCatching {
            val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
            process = p
            BufferedReader(InputStreamReader(p.inputStream)).use { reader ->
                while (scope.isActive) {
                    val line = reader.readLine() ?: break
                    setFreq.find(line)?.let { m -> onTuned(m.groupValues[1].toInt() * 10) }
                    LogcatParser.parse(line)?.let { (id, value) ->
                        if (id == LogcatParser.RADIO_NAME) { tracker.onPs(value); _rds.value = tracker.state }
                        if (id == LogcatParser.RADIO_NAME) repo.ingest(mapOf(id to value, LogcatParser.RADIO_NAME_SEQ to (++nameSeq).toString()))
                        else repo.ingest(mapOf(id to value))
                        if (id == LogcatParser.PARKING && value == "1") armParkingClear()
                    }
                }
            }
        }
    }
}

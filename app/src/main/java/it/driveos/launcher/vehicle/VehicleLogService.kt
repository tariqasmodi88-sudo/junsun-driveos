package it.driveos.launcher.vehicle

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import it.driveos.launcher.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Registrazione continua dei dati del veicolo, in background e a schermo spento, finché
 * l'utente non la ferma (anche per giorni). È un "servizio in primo piano" con una notifica
 * discreta (richiesto da Android per girare a lungo). Registra SOLO le variazioni su file.
 *
 * Avvio:  context.startForegroundService(Intent(context, VehicleLogService::class.java))
 * Stop:   context.startService(Intent(..).setAction(ACTION_STOP))  oppure stopService(..)
 */
class VehicleLogService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val lastValues = HashMap<String, String>()
    private var receiver: BroadcastReceiver? = null
    @Volatile private var extraActions: List<String> = emptyList()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            val action = intent.action ?: return
            // conteggio arrivo del broadcast
            record(mapOf(SignalIds.broadcastCount(action) to ((lastValues[SignalIds.broadcastCount(action)]?.toIntOrNull() ?: 0) + 1).toString()))
            val extras = intent.extras ?: return
            val updates = HashMap<String, String>()
            for (key in extras.keySet()) {
                val v = runCatching { ExtraFormat.stringify(extras.get(key)) }.getOrNull() ?: continue
                updates[SignalIds.broadcast(action, key)] = v
            }
            if (updates.isNotEmpty()) record(updates)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        intent?.getStringArrayExtra(EXTRA_ACTIONS)?.let { extraActions = it.toList() }
        startForeground(NOTIF_ID, buildNotification())
        if (job == null || job?.isActive != true) startLogging()
        VehicleLog.setRunning(true)
        return START_STICKY // se il sistema lo uccide, lo riavvia
    }

    private fun startLogging() {
        VehicleLog.ensureHeader(this)
        VehicleLog.recount(this)
        registerReceiverSafe()
        startLogcat()
        job = scope.launch {
            // baseline: registra lo stato iniziale una volta (valori di partenza)
            val first = runCatching { VehicleRepository.readStatic(this@VehicleLogService) }.getOrDefault(emptyMap())
            baseline(first)
            while (isActive) {
                delay(POLL_MS)
                val now = runCatching { VehicleRepository.readStatic(this@VehicleLogService) }.getOrDefault(emptyMap())
                record(now)
            }
        }
    }

    private var logcatProc: Process? = null
    private var logcatJob: kotlinx.coroutines.Job? = null

    /** Legge il log di Android (se READ_LOGS e' concesso) e registra le righe utili, anche in background. */
    private fun startLogcat() {
        if (logcatJob?.isActive == true) return
        if (checkSelfPermission(android.Manifest.permission.READ_LOGS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val capture = LogCapture()
        logcatJob = scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val p = ProcessBuilder("logcat", "-v", "brief", "*:V").redirectErrorStream(true).start()
                logcatProc = p
                p.inputStream.bufferedReader().use { r ->
                    while (isActive) {
                        val line = r.readLine() ?: break
                        val upd = LinkedHashMap<String, String>()
                        LogcatParser.parse(line)?.let { (id, v) -> upd[id] = v }
                        capture.accept(line)?.let { e -> upd[e.id] = e.message }
                        if (upd.isNotEmpty()) record(upd)
                    }
                }
            }
        }
    }

    private fun registerReceiverSafe() {
        val filter = IntentFilter().apply {
            VehicleRepository.allListenActions(extraActions).forEach { addAction(it) }
        }
        receiver = broadcastReceiver
        runCatching {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(broadcastReceiver, filter)
        }
    }

    /** Prima istantanea: scrive tutti i valori come "comparsi", così il file parte completo. */
    private fun baseline(values: Map<String, String>) {
        val iso = VehicleLog.nowIso()
        val ts = System.currentTimeMillis()
        val lines = values.map { (k, v) -> LogFormat.line(ts, iso, k, null, v) }
        lastValues.clear(); lastValues.putAll(values)
        VehicleLog.append(this, lines)
    }

    /** Registra solo le differenze rispetto all'ultimo stato noto. */
    @Synchronized
    private fun record(values: Map<String, String>) {
        val iso = VehicleLog.nowIso()
        val ts = System.currentTimeMillis()
        val lines = ArrayList<String>()
        for ((k, v) in values) {
            val old = lastValues[k]
            if (old != v) {
                lines.add(LogFormat.line(ts, iso, k, old, v))
                lastValues[k] = v
            }
        }
        if (lines.isNotEmpty()) VehicleLog.append(this, lines)
    }

    override fun onDestroy() {
        job?.cancel()
        logcatJob?.cancel()
        runCatching { logcatProc?.destroy() }
        receiver?.let { runCatching { unregisterReceiver(it) } }
        VehicleLog.setRunning(false)
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val mgr = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, "Registrazione veicolo", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Registra le variazioni dei dati del veicolo"
                setShowBadge(false)
            }
            mgr?.createNotificationChannel(ch)
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL) else @Suppress("DEPRECATION") Notification.Builder(this)
        return builder
            .setContentTitle("DriveOS — registrazione veicolo")
            .setContentText("Registro le variazioni dei dati. Tocca per fermare in DriveOS.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_STOP = "it.driveos.launcher.STOP_VEHICLE_LOG"
        const val EXTRA_ACTIONS = "actions"
        private const val CHANNEL = "vehicle_log"
        private const val NOTIF_ID = 4201
        private const val POLL_MS = 4000L

        fun start(context: Context, discoveredActions: List<String> = emptyList()) {
            val i = Intent(context, VehicleLogService::class.java)
                .putExtra(EXTRA_ACTIONS, discoveredActions.toTypedArray())
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
                else context.startService(i)
            }
        }

        fun stop(context: Context) {
            runCatching { context.startService(Intent(context, VehicleLogService::class.java).setAction(ACTION_STOP)) }
            runCatching { context.stopService(Intent(context, VehicleLogService::class.java)) }
        }
    }
}

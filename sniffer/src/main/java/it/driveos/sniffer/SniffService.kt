package it.driveos.sniffer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registra in background tutto quello che passa nel registro di Android (logcat, tutti i buffer,
 * tutti i tag) e ogni variazione di proprieta' di sistema e impostazioni. Scrive file a rotazione.
 */
class SniffService : Service() {

    companion object {
        const val MAX_FILE_BYTES = 20L * 1024 * 1024
        const val KEEP_FILES = 10
        @Volatile var running = false
        @Volatile var lines = 0L

        // Rumore ad altissima frequenza che non porta informazione (schermo, GPS, SELinux).
        private val noise = Regex("hwcomposer|avc: +denied|MPE_|/mpe|pseudo|nmea|MNLD|gps_contr|update_location|PowerManagerService|chatty")

        /**
         * Broadcast del firmware ForFan/TS di cui si registra il CONTENUTO (tutti gli extra).
         * Quelli marcati 29/10 compaiono nel log come "non-protected broadcast" di com.ts.MainUI,
         * ma il logcat non dice cosa contengono: serve riceverli.
         */
        val ACTIONS = listOf(
            "broadcast_send_carinfo",                 // 29/10: AmapAuto.SendCarInfordata
            "AUTONAVI_STANDARD_BROADCAST_RECV",       // 29/10: protocollo Amap (fari, retro, posizione)
            "android.intent.action.SHOW_MYBRIGHTNESS",// 29/10: cambio luminosità coi fari
            "com.forfan.operator_reversing",
            "com.ts.rearview_state",
            "com.ts.can.BROADCAST_CAN_INFO", "com.ts.can.BROADCAST_CAN_UPT", "com.ts.can.ac",
            "com.ts.can.device", "com.ts.can.excd", "com.ts.can.BROADCAST_CAMERA_KEY",
            "com.globalconstant.BROADCAST_RESPONSE_HFPSTATUS", "com.ts.bt.CONNECT_STATE_CHANGE",
            "comm.power.ENTER_REVERSE", "comm.power.QUIT_REVERSE", "com.cusc.action.ACC_OFF",
            "forfan.adas.turnlight_state", "com.autoai.iot.tpms", "vendor.hazens.vehicle.info.vin",
            "com.ts.main.radio", "com.suding.speedplay",
            "AUTONAVI_STANDARD_BROADCAST_SEND",
        )

        fun dir(ctx: android.content.Context): File =
            File(ctx.getExternalFilesDir(null), "sniffer").apply { mkdirs() }
    }

    private var writer: BufferedWriter? = null
    private var current: File? = null
    private var written = 0L
    private val stamp = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
    @Volatile private var proc: Process? = null
    private var threads = listOf<Thread>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            val extras = intent.extras
            val body = extras?.keySet()?.sorted()?.joinToString(", ") { k ->
                @Suppress("DEPRECATION") val v = extras.get(k)
                "$k=" + fmt(v)
            } ?: ""
            write("${stamp.format(Date())} BROADCAST ${intent.action} {$body}")
        }
    }

    private fun fmt(v: Any?): String = when (v) {
        null -> "null"
        is ByteArray -> "bytes[" + v.joinToString(",") { (it.toInt() and 0xFF).toString() } + "]"
        is IntArray -> "ints[" + v.joinToString(",") + "]"
        is LongArray -> v.joinToString(",", "longs[", "]")
        is FloatArray -> v.joinToString(",", "floats[", "]")
        is BooleanArray -> v.joinToString(",", "bools[", "]")
        is Array<*> -> v.joinToString(",", "[", "]")
        is android.os.Bundle -> "{" + v.keySet().joinToString(", ") { k ->
            @Suppress("DEPRECATION") val x = v.get(k); "$k=" + fmt(x) } + "}"
        else -> v.toString()
    }

    private fun registerBroadcasts() {
        val f = IntentFilter().apply { ACTIONS.forEach { addAction(it) } }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, RECEIVER_EXPORTED)
        else registerReceiver(receiver, f)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("sniff", "Registrazione", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "sniff")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("DriveOS Sniffer")
            .setContentText("Registrazione in corso")
            .setOngoing(true).build()
        startForeground(1, n)
        if (!running) {
            running = true
            lines = 0
            openNew()
            runCatching { registerBroadcasts() }
            threads = listOf(
                Thread { readLogcat() }.apply { isDaemon = true; start() },
                Thread { watchChanges() }.apply { isDaemon = true; start() },
            )
        }
        return START_STICKY
    }

    @Synchronized private fun openNew() {
        runCatching { writer?.close() }
        val name = "sniff_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".txt"
        current = File(dir(this), name)
        writer = BufferedWriter(FileWriter(current, true), 32 * 1024)
        written = 0
        // tiene solo gli ultimi file
        dir(this).listFiles()?.sortedByDescending { it.name }?.drop(KEEP_FILES)?.forEach { it.delete() }
    }

    @Synchronized private fun write(line: String) {
        val w = writer ?: return
        w.write(line); w.write("\n")
        written += line.length + 1
        lines++
        if (lines % 50L == 0L) w.flush()
        if (written > MAX_FILE_BYTES) openNew()
    }

    private fun readLogcat() {
        while (running) {
            runCatching {
                val p = ProcessBuilder("logcat", "-b", "all", "-v", "threadtime", "-T", "1", "*:V")
                    .redirectErrorStream(true).start()
                proc = p
                p.inputStream.bufferedReader().use { r ->
                    while (running) {
                        val l = r.readLine() ?: break
                        if (!noise.containsMatchIn(l)) write(l)
                    }
                }
            }
            if (running) Thread.sleep(2000)
        }
    }

    private fun readProps(): Map<String, String> {
        val out = HashMap<String, String>()
        runCatching {
            val p = ProcessBuilder("getprop").redirectErrorStream(true).start()
            p.inputStream.bufferedReader().useLines { seq ->
                seq.forEach { l ->
                    val m = Regex("^\\[(.*?)]: \\[(.*)]$").find(l) ?: return@forEach
                    out["prop:" + m.groupValues[1]] = m.groupValues[2]
                }
            }
        }
        return out
    }

    private fun readSettings(): Map<String, String> {
        val out = HashMap<String, String>()
        listOf("system" to Settings.System.CONTENT_URI, "global" to Settings.Global.CONTENT_URI,
            "secure" to Settings.Secure.CONTENT_URI).forEach { (t, uri: Uri) ->
            runCatching {
                contentResolver.query(uri, arrayOf("name", "value"), null, null, null)?.use { c ->
                    while (c.moveToNext()) out["set:$t/${c.getString(0)}"] = c.getString(1) ?: ""
                }
            }
        }
        return out
    }

    private fun watchChanges() {
        var last: Map<String, String> = readProps() + readSettings()
        while (running) {
            Thread.sleep(2000)
            val now = readProps() + readSettings()
            val t = stamp.format(Date())
            for ((k, v) in now) {
                val old = last[k]
                if (old != v) write("$t CHANGE $k: ${old ?: "-"} -> $v")
            }
            last = now
        }
    }

    override fun onDestroy() {
        running = false
        runCatching { unregisterReceiver(receiver) }
        runCatching { proc?.destroy() }
        synchronized(this) { runCatching { writer?.flush(); writer?.close() } }
        super.onDestroy()
    }
}

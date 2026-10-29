package it.driveos.sniffer

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { refresh(); handler.postDelayed(this, 1000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (resources.displayMetrics.density * 20).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
        status = TextView(this).apply { textSize = 20f }
        fun btn(text: String, onClick: () -> Unit) = Button(this).apply {
            this.text = text; textSize = 20f; setOnClickListener { onClick() }
        }
        root.addView(TextView(this).apply { text = "DriveOS Sniffer"; textSize = 28f })
        root.addView(status)
        root.addView(btn("Avvia registrazione") { start() })
        root.addView(btn("Ferma registrazione") { stop() })
        root.addView(btn("Copia file in Download") { copyToDownload() })
        root.addView(TextView(this).apply {
            textSize = 15f; gravity = Gravity.START
            text = "\nRegistra tutto quello che passa nel log di Android e ogni variazione di proprietà " +
                "e impostazioni, anche a schermo spento e dopo il riavvio (se era attivo).\n\n" +
                "Permesso necessario (una volta, da ADB):\npm grant it.driveos.sniffer android.permission.READ_LOGS"
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() { super.onResume(); handler.post(tick) }
    override fun onPause() { super.onPause(); handler.removeCallbacks(tick) }

    private fun granted() = checkSelfPermission("android.permission.READ_LOGS") == PackageManager.PERMISSION_GRANTED

    private fun start() {
        if (!granted()) { status.text = "Permesso READ_LOGS mancante: dallo da ADB (vedi sotto)."; return }
        getSharedPreferences("sniffer", Context.MODE_PRIVATE).edit().putBoolean("on", true).apply()
        startForegroundService(Intent(this, SniffService::class.java))
    }

    private fun stop() {
        getSharedPreferences("sniffer", Context.MODE_PRIVATE).edit().putBoolean("on", false).apply()
        stopService(Intent(this, SniffService::class.java))
    }

    private fun refresh() {
        val files = SniffService.dir(this).listFiles()?.sortedByDescending { it.name }.orEmpty()
        val size = files.sumOf { it.length() } / 1024 / 1024
        status.text = (if (SniffService.running) "● IN REGISTRAZIONE" else "○ fermo") +
            "\nRighe scritte: ${SniffService.lines}\nFile: ${files.size} (${size} MB)\n" +
            (if (granted()) "Permesso log: ok" else "Permesso log: MANCANTE")
    }

    private fun copyToDownload() {
        val files = SniffService.dir(this).listFiles().orEmpty()
        var n = 0
        files.forEach { f ->
            runCatching {
                val v = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, f.name)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/DriveOSSniffer")
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)!!
                contentResolver.openOutputStream(uri)!!.use { out -> f.inputStream().use { it.copyTo(out) } }
                n++
            }
        }
        status.text = "Copiati $n file in Download › DriveOSSniffer"
    }
}

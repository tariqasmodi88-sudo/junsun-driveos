package it.driveos.launcher.vehicle

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import it.driveos.launcher.DriveOSApp

/**
 * Servizio di accessibilità (attivato dall'utente). Vede solo QUALE finestra si apre (pacchetto
 * e classe), non il contenuto. Serve a: sapere quando il firmware mostra portiere/radar/retro,
 * registrare i nomi delle finestre nell'export, e tornare alla Home DriveOS.
 */
class DriveAccessibilityService : AccessibilityService() {

    private var lastKey = ""
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val clearDoor = Runnable { (application as? DriveOSApp)?.vehicleRepository?.ingest(mapOf(LogcatParser.DOOR to "chiuse")) }
    private val clearRadar = Runnable { (application as? DriveOSApp)?.vehicleRepository?.ingest(mapOf(LogcatParser.PARKING to "0")) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        WindowWatcher.service = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString().orEmpty()
        val key = "$pkg/$cls"
        if (key == lastKey) return
        lastKey = key
        handler.postDelayed({ if (lastKey == key) lastKey = "" }, 3000)
        val kind = WindowRules.classify(pkg, cls)
        val repo = (application as? DriveOSApp)?.vehicleRepository
        // Il nome dell'ultima finestra finisce nel registro/export (utile a capire il firmware).
        repo?.ingest(mapOf("win:last" to key))
        when (kind) {
            // La finestra non dice quando si chiude: la portiera resta "aperta" 15 s dall'ultimo avviso.
            WindowKind.DOOR -> { repo?.ingest(mapOf(LogcatParser.DOOR to "aperta")); handler.removeCallbacks(clearDoor); handler.postDelayed(clearDoor, 15_000) }
            WindowKind.RADAR -> { repo?.ingest(mapOf(LogcatParser.PARKING to "1")); handler.removeCallbacks(clearRadar); handler.postDelayed(clearRadar, 15_000) }
            else -> {}
        }
        if (kind == WindowKind.DOOR || kind == WindowKind.RADAR) {
            // Cosa c'e' dentro la finestra (nomi dei pezzi): serve a capire QUALE porta/sensore.
            handler.postDelayed({ dumpTree(key) }, 400)
        }
        WindowWatcher.emit(WindowEvent(pkg, cls, kind))
    }

    private fun dumpTree(key: String) {
        val root = runCatching { rootInActiveWindow }.getOrNull() ?: return
        val parts = ArrayList<String>()
        fun walk(n: android.view.accessibility.AccessibilityNodeInfo?, depth: Int) {
            if (n == null || parts.size >= 40 || depth > 12) return
            val id = n.viewIdResourceName?.substringAfter(":id/")
            val d = n.contentDescription?.toString()
            val t = n.text?.toString()
            if (id != null || d != null || t != null) {
                parts.add(listOfNotNull(id, d?.let { "d=$it" }, t?.let { "t=$it" }, if (n.isSelected) "sel" else null,
                    if (n.isChecked) "chk" else null).joinToString(":"))
            }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        walk(root, 0)
        if (parts.isNotEmpty()) {
            (application as? DriveOSApp)?.vehicleRepository?.ingest(mapOf("win:tree" to (key + " | " + parts.joinToString(" ; ")).take(600)))
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (WindowWatcher.service === this) WindowWatcher.service = null
        return super.onUnbind(intent)
    }

    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
}

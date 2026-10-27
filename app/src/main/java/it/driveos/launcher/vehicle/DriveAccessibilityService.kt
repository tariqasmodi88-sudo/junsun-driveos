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
        WindowWatcher.emit(WindowEvent(pkg, cls, kind))
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (WindowWatcher.service === this) WindowWatcher.service = null
        return super.onUnbind(intent)
    }

    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
}

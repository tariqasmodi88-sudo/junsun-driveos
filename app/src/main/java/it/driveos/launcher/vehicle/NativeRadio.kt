package it.driveos.launcher.vehicle

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo

/**
 * La radio Junsun non accetta comandi dall'esterno (tutto avviene dentro la sua app). Questo oggetto
 * apre per un istante la radio nativa, preme da solo il pulsante richiesto (tramite il servizio
 * Accessibilità) e torna alla Home DriveOS. Nomi dei pulsanti: quelli scritti dall'app nel registro.
 */
object NativeRadio {
    const val SEEK_UP = "btn_seek_inc"
    const val SEEK_DOWN = "btn_seek_dec"
    const val FM = "btn_fm"
    const val AM = "btn_am"
    const val AF = "btn_af"
    const val TA = "btn_ta"
    fun preset(slot: Int) = "btn_t$slot"

    private const val PKG = "com.ts.MainUI"
    private const val CLS = "com.ts.main.radio.RadioMainActivity"
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var busy = false

    /** false se manca il servizio Accessibilità o un altro comando è in corso. */
    fun press(context: Context, viewName: String, backAfterMs: Long = 1500, onResult: (Boolean) -> Unit = {}): Boolean {
        val svc = WindowWatcher.service ?: return false
        if (busy) return false
        busy = true
        runCatching {
            context.startActivity(Intent().setClassName(PKG, CLS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        var tries = 0
        fun attempt() {
            val root = runCatching { svc.rootInActiveWindow }.getOrNull()
            val clicked = root != null && root.packageName?.toString() == PKG && click(root, "$PKG:id/$viewName")
            when {
                clicked -> handler.postDelayed({ svc.goHome(); busy = false; onResult(true) }, backAfterMs)
                ++tries > 20 -> { svc.goHome(); busy = false; onResult(false) }
                else -> handler.postDelayed({ attempt() }, 200)
            }
        }
        handler.postDelayed({ attempt() }, 400)
        return true
    }

    private fun click(root: AccessibilityNodeInfo, id: String): Boolean {
        val node = root.findAccessibilityNodeInfosByViewId(id)?.firstOrNull() ?: return false
        var n: AccessibilityNodeInfo? = node
        while (n != null) {
            if (n.isClickable && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            n = n.parent
        }
        return false
    }
}

package it.driveos.launcher.system

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import java.io.File

/**
 * Immagine di avvio mostrata anche quando si toglie la chiave.
 *
 * Registro di spegnimento del 07/11: appena arriva il segnale di spegnimento la radio mette davanti a
 * tutto una sua schermata nera (com.ts.HideActivity), dopo ~3 s spegne lo schermo e chiude DriveOS.
 * Un'immagine disegnata dentro DriveOS resterebbe quindi sotto quella schermata: qui si usa una finestra
 * "sopra le altre app" (permesso gia' concesso per la retromarcia), che non riceve tocchi ne' tastiera e
 * non interferisce con lo spegnimento. Sparisce da sola quando la radio chiude DriveOS; per sicurezza
 * anche alla riaccensione e comunque dopo [MAX_MS].
 */
object ShutdownImage {
    private const val MAX_MS = 20_000L
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var cached: Bitmap? = null
    private var view: View? = null

    /** File scelto dall'utente: avvio.jpg / avvio.png nella cartella dell'app. */
    fun file(ctx: Context): File? {
        val dir = ctx.getExternalFilesDir(null) ?: return null
        return listOf("avvio.jpg", "avvio.png").map { File(dir, it) }.firstOrNull { it.isFile }
    }

    /** Carica l'immagine in anticipo (da un thread di sfondo): allo spegnimento non c'e' tempo. */
    fun preload(ctx: Context) {
        cached = file(ctx)?.let { f -> runCatching { BitmapFactory.decodeFile(f.path) }.getOrNull() }
    }

    /** Dal thread principale. Non fa nulla senza immagine o senza il permesso "sopra le altre app". */
    fun show(ctx: Context) {
        val bmp = cached ?: return
        if (view != null || !Settings.canDrawOverlays(ctx)) return
        val wm = ctx.getSystemService(WindowManager::class.java) ?: return
        val v = ImageView(ctx).apply {
            setImageBitmap(bmp); scaleType = ImageView.ScaleType.FIT_CENTER; setBackgroundColor(Color.BLACK)
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE,
        )
        if (runCatching { wm.addView(v, lp) }.isSuccess) {
            view = v
            handler.postDelayed({ hide(ctx) }, MAX_MS)
        }
    }

    fun hide(ctx: Context) {
        val v = view ?: return
        view = null
        runCatching { ctx.getSystemService(WindowManager::class.java)?.removeView(v) }
    }
}

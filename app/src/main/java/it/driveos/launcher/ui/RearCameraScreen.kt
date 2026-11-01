package it.driveos.launcher.ui

import it.driveos.launcher.i18n.tr
import it.driveos.launcher.i18n.trf

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val Red = Color(0xFFFF4D4D)
private val Orange = Color(0xFFFF9A3D)
private val Yellow = Color(0xFFFFD43D)
private val Green = Color(0xFF3DDC84)

/**
 * Retromarcia: immagine della telecamera posteriore (se presente e leggibile) con linee guida
 * a colori e i 4 sensori di parcheggio. Senza immagine la schermata resta neutra con i soli sensori.
 * [rearLevels]: 0 = niente, 1 lontano, 2 medio, 3 vicinissimo (da sinistra a destra).
 */
@Composable
fun RearCameraScreen(rearLevels: List<Int>?, rearRaw: List<Int>?, cameraEnabled: Boolean, onHome: () -> Unit) {
    var hasImage by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraEnabled && hasCameraPermission(ctx)) {
            CameraPreview(onFrames = { hasImage = true }, onFail = { hasImage = false }, modifier = Modifier.fillMaxSize())
        }
        if (!hasImage) {
            // nessuna immagine: fondo neutro e solo i sensori
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }
        Guides(rearLevels, showLanes = hasImage, modifier = Modifier.fillMaxSize())
        // barra in alto
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color.Black.copy(alpha = 0.55f)) {
                Text(tr("R  Retromarcia"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
            Spacer(Modifier.weight(1f))
            val worst = rearLevels?.maxOrNull() ?: 0
            if (worst > 0) Surface(shape = RoundedCornerShape(14.dp), color = levelColor(worst).copy(alpha = 0.85f)) {
                Text(when (worst) { 3 -> "STOP"; 2 -> tr("Vicino"); else -> tr("Ostacolo") }, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = Color.Black, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
            Spacer(Modifier.width(10.dp))
            Surface(onClick = onHome, shape = RoundedCornerShape(14.dp), color = Color.Black.copy(alpha = 0.55f)) {
                Text(tr("Home"), fontSize = 18.sp, color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
        if (!hasImage) Text(
            if (cameraEnabled) tr("Nessuna immagine dalla telecamera") else tr("Telecamera non impostata"),
            fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}

private fun levelColor(l: Int) = when (l) { 3 -> Red; 2 -> Orange; 1 -> Yellow; else -> Color.White }

private fun hasCameraPermission(ctx: Context) =
    ctx.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED

/** Linee guida (con immagine) e arco dei 4 sensori in basso. */
@Composable
private fun Guides(levels: List<Int>?, showLanes: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        if (showLanes) {
            // trapezio prospettico: rosso vicino (in basso), poi arancio, giallo, verde
            val bands = listOf(Red to 0.0f, Orange to 0.25f, Yellow to 0.5f, Green to 0.75f)
            fun xAt(t: Float, left: Boolean): Float { val near = if (left) 0.18f else 0.82f; val far = if (left) 0.38f else 0.62f; return w * (near + (far - near) * t) }
            fun yAt(t: Float) = h * (0.95f - 0.5f * t)
            bands.forEach { (c, t0) ->
                val t1 = t0 + 0.25f
                drawLine(c, Offset(xAt(t0, true), yAt(t0)), Offset(xAt(t1, true), yAt(t1)), strokeWidth = 8f, cap = StrokeCap.Round)
                drawLine(c, Offset(xAt(t0, false), yAt(t0)), Offset(xAt(t1, false), yAt(t1)), strokeWidth = 8f, cap = StrokeCap.Round)
                drawLine(c, Offset(xAt(t1, true), yAt(t1)), Offset(xAt(t1, true) + w * 0.04f, yAt(t1)), strokeWidth = 6f)
                drawLine(c, Offset(xAt(t1, false), yAt(t1)), Offset(xAt(t1, false) - w * 0.04f, yAt(t1)), strokeWidth = 6f)
            }
        }
        // sensori: 4 settori ad arco sopra il paraurti (in basso al centro)
        val cx = w / 2; val cy = h * (if (showLanes) 1.02f else 0.80f)
        val r = minOf(w, h) * (if (showLanes) 0.30f else 0.42f)
        val lv = levels ?: listOf(0, 0, 0, 0)
        val sweep = 160f / 4
        for (i in 0 until 4) {
            val l = lv.getOrElse(i) { 0 }
            val start = 190f + i * sweep
            for (ring in 1..3) {
                val on = l > 0 && ring <= l
                val col = if (on) levelColor(l) else Color.White.copy(alpha = if (showLanes) 0.25f else 0.18f)
                val rr = r * (1.25f - ring * 0.22f)
                drawArc(col, start + 2f, sweep - 4f, useCenter = false,
                    topLeft = Offset(cx - rr, cy - rr), size = Size(rr * 2, rr * 2), style = Stroke(width = r * 0.13f, cap = StrokeCap.Butt))
            }
        }
        if (!showLanes) {
            // sagoma posteriore dell'auto
            val bw = w * 0.30f; val bh = h * 0.12f
            val p = Path().apply {
                moveTo(cx - bw / 2, cy); lineTo(cx - bw / 2, cy + bh); lineTo(cx + bw / 2, cy + bh); lineTo(cx + bw / 2, cy); close()
            }
            drawPath(p, Color.White.copy(alpha = 0.35f), style = Stroke(width = 4f))
        }
    }
}

/** Anteprima Camera2 della prima telecamera disponibile (sul Junsun di solito l'ingresso retro). */
@SuppressLint("MissingPermission")
@Composable
private fun CameraPreview(onFrames: () -> Unit, onFail: () -> Unit, modifier: Modifier) {
    val ctx = LocalContext.current
    val main = remember { Handler(Looper.getMainLooper()) }
    var device by remember { mutableStateOf<CameraDevice?>(null) }
    DisposableEffect(Unit) { onDispose { runCatching { device?.close() } } }
    AndroidView(modifier = modifier, factory = { c ->
        TextureView(c).apply {
            var framed = false
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                    val cm = c.getSystemService(CameraManager::class.java)
                    val id = runCatching {
                        cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_FRONT }
                            ?: cm.cameraIdList.firstOrNull()
                    }.getOrNull() ?: run { onFail(); return }
                    runCatching {
                        cm.openCamera(id, object : CameraDevice.StateCallback() {
                            override fun onOpened(cd: CameraDevice) {
                                device = cd
                                st.setDefaultBufferSize(1280, 720)
                                val surface = Surface(st)
                                val req = cd.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(surface) }
                                cd.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                                    override fun onConfigured(s: CameraCaptureSession) { runCatching { s.setRepeatingRequest(req.build(), null, main) } }
                                    override fun onConfigureFailed(s: CameraCaptureSession) = onFail()
                                }, main)
                            }
                            override fun onDisconnected(cd: CameraDevice) { cd.close(); onFail() }
                            override fun onError(cd: CameraDevice, error: Int) { cd.close(); onFail() }
                        }, main)
                    }.onFailure { onFail() }
                }
                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { runCatching { device?.close() }; return true }
                override fun onSurfaceTextureUpdated(st: SurfaceTexture) { if (!framed) { framed = true; onFrames() } }
            }
        }
    })
}

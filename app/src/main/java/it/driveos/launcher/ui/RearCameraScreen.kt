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
fun RearCameraScreen(rearLevels: List<Int>?, rearRaw: List<Int>?, cameraEnabled: Boolean, onHome: () -> Unit, steer: Int? = null) {
    var hasImage by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    // il messaggio "nessuna immagine" compare solo se dopo 3 s non arriva nulla (niente sfarfallio all'apertura)
    val waited by androidx.compose.runtime.produceState(false) { kotlinx.coroutines.delay(3000); value = true }
    Box(Modifier.fillMaxSize().background(Color(0xFF0E1116))) {
        if (cameraEnabled && hasCameraPermission(ctx)) {
            CameraPreview(onFrames = { hasImage = true }, onFail = { hasImage = false }, modifier = Modifier.fillMaxSize())
        }
        // stessa grafica con e senza immagine: linee di traiettoria + sensori
        Guides(rearLevels, steer, modifier = Modifier.fillMaxSize())
        // barra in alto
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color.Black.copy(alpha = 0.55f)) {
                Text(tr("R  Retromarcia"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
            Spacer(Modifier.weight(1f))
            val worst = rearLevels?.maxOrNull() ?: 0
            if (worst > 0) Surface(shape = RoundedCornerShape(14.dp), color = levelColor(worst).copy(alpha = 0.9f)) {
                Text(when (worst) { 3 -> "STOP"; 2 -> tr("Vicino"); else -> tr("Ostacolo") }, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = Color.Black, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
            Spacer(Modifier.width(10.dp))
            Surface(onClick = onHome, shape = RoundedCornerShape(14.dp), color = Color.Black.copy(alpha = 0.55f)) {
                Text(tr("Home"), fontSize = 18.sp, color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
        if (!hasImage && (waited || !cameraEnabled)) Text(
            if (cameraEnabled) tr("Nessuna immagine dalla telecamera") else tr("Telecamera non impostata"),
            fontSize = 14.sp, color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 26.dp),
        )
    }
}

private fun levelColor(l: Int) = when (l) { 3 -> Red; 2 -> Orange; 1 -> Yellow; else -> Color.White }

private fun hasCameraPermission(ctx: Context) =
    ctx.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED

/**
 * Linee di traiettoria che curvano con il volante (angolo dalla centralina, circa -500..+500, clamp della radio;
 * senza dato restano dritte) e i 4 sensori come archi sottili sopra il paraurti.
 */
@Composable
private fun Guides(levels: List<Int>?, steer: Int?, modifier: Modifier) {
    val k by androidx.compose.animation.core.animateFloatAsState(
        ((steer ?: 0) / 500f).coerceIn(-1f, 1f), label = "steer",
    )
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val bottom = h * 0.97f; val far = h * 0.42f
        // punto della corsia alla distanza t (0 = paraurti, 1 = lontano): prospettiva + curva del volante
        fun pt(t: Float, side: Float): Offset {
            val y = bottom - (bottom - far) * (1f - (1f - t) * (1f - t) * 0.15f) * t
            val half = w * (0.30f - 0.19f * t)          // meta' larghezza auto, si stringe in lontananza
            val bend = k * w * 0.32f * t * t             // la traiettoria piega con lo sterzo
            return Offset(w / 2 + side * half + bend, y)
        }
        fun colorAt(t: Float) = when { t < 0.25f -> Red; t < 0.5f -> Orange; t < 0.75f -> Yellow; else -> Green }
        // linee statiche (larghezza auto) tenui e tratteggiate
        if (kotlin.math.abs(k) > 0.03f) for (side in listOf(-1f, 1f)) {
            val p0 = Offset(w / 2 + side * w * 0.30f, bottom); val p1 = Offset(w / 2 + side * w * 0.11f, far)
            drawLine(Color.White.copy(alpha = 0.22f), p0, p1, strokeWidth = 3f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(18f, 14f)))
        }
        // traiettoria: due linee sottili a colori + tacche di distanza
        val steps = 40
        for (side in listOf(-1f, 1f)) {
            for (i in 0 until steps) {
                val t0 = i / steps.toFloat(); val t1 = (i + 1) / steps.toFloat()
                drawLine(colorAt(t0).copy(alpha = 0.95f), pt(t0, side), pt(t1, side), strokeWidth = 5f, cap = StrokeCap.Round)
            }
        }
        for (t in listOf(0.25f, 0.5f, 0.75f)) {
            val l = pt(t, -1f); val r = pt(t, 1f)
            val len = (r.x - l.x) * 0.12f
            drawLine(colorAt(t - 0.01f), l, Offset(l.x + len, l.y), strokeWidth = 4f, cap = StrokeCap.Round)
            drawLine(colorAt(t - 0.01f), r, Offset(r.x - len, r.y), strokeWidth = 4f, cap = StrokeCap.Round)
        }
        // sensori: 4 settori, 3 anelli sottili ciascuno, appena sopra il paraurti
        val cx = w / 2; val cy = h * 1.05f
        val r0 = minOf(w, h) * 0.20f
        val lv = levels ?: listOf(0, 0, 0, 0)
        val sweep = 120f / 4
        for (i in 0 until 4) {
            val l = lv.getOrElse(i) { 0 }
            val start = 210f + i * sweep
            for (ring in 1..3) {
                // anello 1 = il piu' vicino all'auto (si accende per ultimo, insieme al rosso)
                val on = l > 0 && ring >= 4 - l
                val col = if (on) levelColor(l) else Color.White.copy(alpha = 0.14f)
                val rr = r0 + ring * r0 * 0.16f
                drawArc(col, start + 3f, sweep - 6f, useCenter = false,
                    topLeft = Offset(cx - rr, cy - rr), size = Size(rr * 2, rr * 2),
                    style = Stroke(width = if (on) 7f else 4f, cap = StrokeCap.Round))
            }
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
            // la schermata puo' chiudersi (retro tolta subito) prima che la telecamera finisca di aprirsi
            var alive = true
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                    val cm = c.getSystemService(CameraManager::class.java)
                    // Junsun FF_866X: l'ingresso retrocamera analogico (PAL 960x576 / NTSC) e' la camera "1",
                    // la stessa che apre l'app nativa SpmCarCam in retromarcia. Le altre sono AHD/vuote.
                    val id = runCatching {
                        val ids = cm.cameraIdList.toList()
                        fun h(id: String) = cm.getCameraCharacteristics(id).get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)?.height() ?: 0
                        ids.firstOrNull { h(it) == 576 || h(it) == 480 && it != "0" }
                            ?: ids.firstOrNull { it == "1" }
                            ?: ids.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_FRONT }
                            ?: ids.firstOrNull()
                    }.getOrNull() ?: run { onFail(); return }
                    runCatching {
                        cm.openCamera(id, object : CameraDevice.StateCallback() {
                            override fun onOpened(cd: CameraDevice) {
                                if (!alive) { runCatching { cd.close() }; return }
                                device = cd
                                // "Surface was abandoned" (07/11): mai far cadere l'app, al massimo niente immagine
                                runCatching {
                                    st.setDefaultBufferSize(1280, 720)
                                    val surface = Surface(st)
                                    val req = cd.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(surface) }
                                    cd.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                                        override fun onConfigured(s: CameraCaptureSession) { runCatching { s.setRepeatingRequest(req.build(), null, main) } }
                                        override fun onConfigureFailed(s: CameraCaptureSession) = onFail()
                                    }, main)
                                }.onFailure { runCatching { cd.close() }; device = null; onFail() }
                            }
                            override fun onDisconnected(cd: CameraDevice) { cd.close(); onFail() }
                            override fun onError(cd: CameraDevice, error: Int) { cd.close(); onFail() }
                        }, main)
                    }.onFailure { onFail() }
                }
                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { alive = false; runCatching { device?.close() }; device = null; return true }
                override fun onSurfaceTextureUpdated(st: SurfaceTexture) { if (!framed) { framed = true; onFrames() } }
            }
        }
    })
}

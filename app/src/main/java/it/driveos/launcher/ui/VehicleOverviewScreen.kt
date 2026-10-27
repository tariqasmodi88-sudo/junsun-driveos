package it.driveos.launcher.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.vehicle.ActiveAlert

/** Stato del veicolo da mostrare. null = dato non disponibile (mai inventato). */
data class VehicleOverviewState(
    val lightsOn: Boolean?,
    val reverse: Boolean?,
    val rearview: Boolean?,
    val doorOpen: Boolean?,
    val parking: Boolean?,
    val phone: Boolean?,
    val projection: Boolean,
    val radioKhz: Int?,
    val speedKmh: Double?,
    val alerts: List<ActiveAlert>,
    val logsGranted: Boolean,
    val gpsPermission: Boolean = true,
    /** Livello di vicinanza 0..3 dei 4 sensori posteriori (sinistra→destra); null = dato non disponibile. */
    val rearLevels: List<Int>? = null,
)

private val Amber = Color(0xFFFFB020)
private val Warn = Color(0xFFFF5A4E)
private val Ok = Color(0xFF3DDC97)
private val Beam = Color(0xFFFFE9A8)

/**
 * Schermata "auto ferma": vista dall'alto della Tiguan con portiere, fari, retromarcia e
 * sensori; stato a sinistra, spie a destra. Unico comando: Home.
 */
@Composable
fun VehicleOverviewScreen(state: VehicleOverviewState, onHome: () -> Unit, onDebug: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            // --- Sinistra: stato ---
            Column(Modifier.weight(0.36f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Stato")
                StatusRow("Fari", state.lightsOn?.let { if (it) "accesi" else "spenti" }, state.lightsOn == true, Amber)
                StatusRow("Portiere", state.doorOpen?.let { if (it) "aperta" else "chiuse" }, state.doorOpen == true, Warn,
                    missing = if (!state.logsGranted) "permesso" else null)
                StatusRow("Retromarcia", state.reverse?.let { if (it) "inserita" else "no" }, state.reverse == true, Color.White)
                StatusRow("Sensori post.", if (rearSensors(state)) "attivi" else (state.parking?.let { "no" }),
                    rearSensors(state), Amber, missing = if (!state.logsGranted) "con retro" else null)
                StatusRow("Telefono", state.phone?.let { if (it) "collegato" else "scollegato" }, state.phone == true, Ok)
                StatusRow("CarPlay/AA", if (state.projection) "attivo" else "no", state.projection, Ok)
                StatusRow("Radio", state.radioKhz?.let { "%.1f MHz".format(it / 1000.0) }, false, c.primary)
            }

            // --- Centro: l'auto ---
            Box(Modifier.weight(0.3f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                CarTopView(state, Modifier.fillMaxSize())
                Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.speedKmh?.let { "${it.toInt()} km/h" } ?: "— km/h",
                        fontSize = 30.sp, fontWeight = FontWeight.Bold, color = c.onBackground)
                    Text(when { state.speedKmh == null -> if (state.gpsPermission) "In attesa del GPS" else "Serve il permesso posizione"; state.speedKmh < 2.0 -> "Veicolo fermo"; else -> "In marcia" },
                        fontSize = 14.sp, color = c.onSurfaceVariant)
                }
            }

            // --- Destra: spie e motore ---
            Column(Modifier.weight(0.34f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Spie")
                if (state.alerts.isEmpty()) {
                    Surface(shape = RoundedCornerShape(16.dp), color = Ok.copy(alpha = 0.14f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).background(Ok, CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Text("Nessuna spia attiva", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = c.onSurface)
                        }
                    }
                } else {
                    state.alerts.forEach { a ->
                        Surface(shape = RoundedCornerShape(16.dp), color = Warn.copy(alpha = 0.18f), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("⚠", fontSize = 20.sp, color = Warn)
                                Spacer(Modifier.width(10.dp))
                                Text(a.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = c.onSurface)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                SectionTitle("Motore")
                Surface(shape = RoundedCornerShape(16.dp), color = c.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        EngineLine("Carburante", "—")
                        EngineLine("Temp. motore", "—")
                        EngineLine("Giri", "—")
                        Spacer(Modifier.height(8.dp))
                        Text("Questa radio non passa i dati del motore alle app. Arrivano con un adattatore OBD-II.",
                            fontSize = 13.sp, color = c.onSurfaceVariant)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = onHome, shape = RoundedCornerShape(20.dp), color = c.primary, contentColor = c.onPrimary,
                modifier = Modifier.weight(1f).height(68.dp)) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Home", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Surface(onClick = onDebug, shape = RoundedCornerShape(20.dp), color = c.surfaceVariant, contentColor = c.onSurface,
                modifier = Modifier.width(140.dp).height(68.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Debug", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** Sensori posteriori: dal log radar se disponibile, altrimenti attivi con la retromarcia inserita. */
private fun rearSensors(s: VehicleOverviewState) = s.reverse == true

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun StatusRow(label: String, value: String?, active: Boolean, activeColor: Color, missing: String? = null) {
    val c = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(14.dp), color = if (active) activeColor.copy(alpha = 0.16f) else c.surface,
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(if (active) activeColor else c.outline, CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(label, fontSize = 15.sp, color = c.onSurfaceVariant, maxLines = 1, modifier = Modifier.weight(1f))
            Text(value ?: (missing ?: "—"), fontSize = if (value != null) 17.sp else 13.sp,
                fontWeight = if (value != null) FontWeight.SemiBold else FontWeight.Normal,
                color = if (value != null) c.onSurface else c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EngineLine(label: String, value: String) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, fontSize = 15.sp, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.onSurface)
    }
}

/** Disegno dell'auto vista dall'alto, con fari, portiere, luci retro e onde dei sensori. */
@Composable
private fun CarTopView(state: VehicleOverviewState, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p",
    )
    val body = c.surfaceVariant
    val outline = c.onSurfaceVariant.copy(alpha = 0.5f)
    val glass = c.background.copy(alpha = 0.85f)

    Canvas(modifier.padding(top = 70.dp, bottom = 10.dp)) {
        val h = size.height * 0.86f
        val w = h * 0.44f
        val left = (size.width - w) / 2f
        val top = (size.height - h) / 2f + 10f

        // Fari: fasci di luce davanti
        if (state.lightsOn == true) {
            val beamLen = h * 0.22f
            listOf(left + w * 0.22f, left + w * 0.78f).forEach { x ->
                drawPath(androidx.compose.ui.graphics.Path().apply {
                    moveTo(x - w * 0.06f, top + 6f)
                    lineTo(x - w * 0.22f, top - beamLen)
                    lineTo(x + w * 0.22f, top - beamLen)
                    lineTo(x + w * 0.06f, top + 6f)
                    close()
                }, Brush.verticalGradient(listOf(Beam.copy(alpha = 0f), Beam.copy(alpha = 0.55f)),
                    startY = top - beamLen, endY = top))
            }
        }

        // Sensori di parcheggio posteriori: 4 settori ad anelli (come l'originale). Più l'ostacolo è
        // vicino, più anelli si accendono e il colore passa da giallo a rosso. Senza dato di
        // distanza i 4 settori "respirano" in giallo: sensori attivi.
        if (rearSensors(state)) {
            val levels = state.rearLevels
            val cx = left + w / 2
            val cy = top + h - 6f
            val span = 150f
            val seg = span / 4f
            for (i in 0 until 4) {
                val level = levels?.getOrNull(i)?.coerceIn(0, 3)
                for (ring in 1..3) {
                    val lit = if (level == null) ring == 1 else ring <= level
                    val color = when {
                        level == null -> Amber
                        level >= 3 -> Warn
                        level == 2 -> Color(0xFFFF9A3D)
                        else -> Amber
                    }
                    val alpha = if (lit) (if (level == null) 0.35f + 0.4f * pulse else if (level >= 3) 0.6f + 0.4f * pulse else 0.95f) else 0.12f
                    val r = w * (0.32f + ring * 0.17f)
                    drawArc(color.copy(alpha = alpha), 15f + seg * i + 2f, seg - 4f, false,
                        topLeft = Offset(cx - r, cy - r * 0.55f), size = Size(r * 2, r * 1.1f),
                        style = Stroke(width = 11f, cap = StrokeCap.Round))
                }
            }
        }

        // Carrozzeria
        drawRoundRect(body, Offset(left, top), Size(w, h), CornerRadius(w * 0.32f))
        drawRoundRect(outline, Offset(left, top), Size(w, h), CornerRadius(w * 0.32f), style = Stroke(3f))
        // Parabrezza, tetto, lunotto
        drawRoundRect(glass, Offset(left + w * 0.13f, top + h * 0.20f), Size(w * 0.74f, h * 0.14f), CornerRadius(w * 0.12f))
        drawRoundRect(body.copy(alpha = 0.9f), Offset(left + w * 0.15f, top + h * 0.36f), Size(w * 0.70f, h * 0.30f), CornerRadius(w * 0.08f))
        drawRoundRect(outline, Offset(left + w * 0.15f, top + h * 0.36f), Size(w * 0.70f, h * 0.30f), CornerRadius(w * 0.08f), style = Stroke(2f))
        drawRoundRect(glass, Offset(left + w * 0.16f, top + h * 0.69f), Size(w * 0.68f, h * 0.10f), CornerRadius(w * 0.10f))

        // Fari (gruppi ottici) e luci posteriori
        val headColor = if (state.lightsOn == true) Beam else outline
        drawRoundRect(headColor, Offset(left + w * 0.08f, top + h * 0.025f), Size(w * 0.26f, h * 0.035f), CornerRadius(8f))
        drawRoundRect(headColor, Offset(left + w * 0.66f, top + h * 0.025f), Size(w * 0.26f, h * 0.035f), CornerRadius(8f))
        val tailColor = if (state.reverse == true) Color.White else Warn.copy(alpha = if (state.lightsOn == true) 0.95f else 0.45f)
        drawRoundRect(tailColor, Offset(left + w * 0.08f, top + h * 0.94f), Size(w * 0.24f, h * 0.03f), CornerRadius(8f))
        drawRoundRect(tailColor, Offset(left + w * 0.68f, top + h * 0.94f), Size(w * 0.24f, h * 0.03f), CornerRadius(8f))

        // Portiere: linee di separazione; se aperta, portiere evidenziate e "spalancate"
        drawDoors(left, top, w, h, open = state.doorOpen == true, closedColor = outline, openColor = Warn.copy(alpha = pulse))
    }
}

private fun DrawScope.drawDoors(left: Float, top: Float, w: Float, h: Float, open: Boolean, closedColor: Color, openColor: Color) {
    val frontY = top + h * 0.36f
    val midY = top + h * 0.52f
    val rearY = top + h * 0.68f
    val doorLen = h * 0.15f
    if (!open) {
        // tagli porte chiuse sui fianchi
        listOf(frontY, midY, rearY).forEach { y ->
            drawLine(closedColor, Offset(left, y), Offset(left + w * 0.12f, y), strokeWidth = 3f)
            drawLine(closedColor, Offset(left + w * 0.88f, y), Offset(left + w, y), strokeWidth = 3f)
        }
        return
    }
    // La radio dice "una portiera è aperta" ma non quale: le mostriamo tutte socchiuse in evidenza.
    listOf(frontY, midY).forEach { y ->
        // lato sinistro (guidatore)
        drawLine(openColor, Offset(left, y), Offset(left - doorLen * 0.75f, y + doorLen * 0.65f), strokeWidth = 9f, cap = StrokeCap.Round)
        // lato destro
        drawLine(openColor, Offset(left + w, y), Offset(left + w + doorLen * 0.75f, y + doorLen * 0.65f), strokeWidth = 9f, cap = StrokeCap.Round)
    }
}

package it.driveos.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.radio.RadioBand
import it.driveos.launcher.radio.RadioPreset
import it.driveos.launcher.radio.RadioPresetLogic
import it.driveos.launcher.radio.RadioState
import it.driveos.launcher.ui.icons.PauseIcon

/**
 * Radio INTEGRATA in DriveOS. Mostra solo ciò che si legge davvero (frequenza dal sistema, nome dai
 * preferiti salvati); i comandi sono best-effort finché non conosciamo i parametri esatti di questa radio.
 * Sei posti fissi (P1…P6): un posto vuoto salva la stazione in ascolto.
 */
@Composable
fun RadioScreen(
    state: RadioState?,
    detected: Boolean,
    presets: List<RadioPreset>,
    currentKhz: Int?,
    onTune: (RadioPreset) -> Unit,
    onSeekDown: () -> Unit,
    onSeekUp: () -> Unit,
    onStepDown: () -> Unit,
    onStepUp: () -> Unit,
    onPlayPause: () -> Unit,
    onAdd: () -> Unit,
    onSaveCurrent: (() -> Unit)? = null,
    onRemove: (RadioPreset) -> Unit,
    onOpenExternal: () -> Unit,
    onBack: () -> Unit,
    /** Nome RDS, se il firmware lo pubblica. */
    stationName: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val khz = state?.khz ?: currentKhz
    val freqText = khz?.let { RadioPreset(it, "").displayFrequency() } ?: "—"
    val playing = state?.playing == true
    val current = presets.firstOrNull { it.khz == khz }
    val name = stationName ?: current?.name

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Radio", onBack) {
            Surface(onClick = onOpenExternal, shape = RoundedCornerShape(14.dp), color = colors.surfaceVariant, contentColor = colors.onSurface) {
                Text("Radio Junsun", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
            }
        }
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // --- Sintonizzatore ---
            Column(Modifier.weight(0.52f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(24.dp), color = colors.surface, modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(10.dp), color = colors.primary.copy(alpha = 0.18f)) {
                                Text("FM", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            Text(if (detected) (if (playing) "In ascolto" else "In pausa") else "Frequenza dal sistema",
                                fontSize = 13.sp, color = colors.onSurfaceVariant)
                        }
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(freqText, fontSize = 64.sp, fontWeight = FontWeight.Light, color = colors.onSurface)
                                Spacer(Modifier.width(8.dp))
                                Text("MHz", fontSize = 18.sp, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 14.dp))
                            }
                            Text(name ?: "—", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                                color = if (name != null) colors.primary else colors.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Dial(khz)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CtrlButton(null, "Cerca giù", Modifier.weight(1f), onSeekDown, textIcon = "«")
                    CtrlButton(null, "Passo giù", Modifier.weight(0.8f), onStepDown, textIcon = "‹")
                    CtrlButton(if (playing) PauseIcon else Icons.Filled.PlayArrow, "Play/Pausa", Modifier.weight(1f), onPlayPause, highlighted = true)
                    CtrlButton(null, "Passo su", Modifier.weight(0.8f), onStepUp, textIcon = "›")
                    CtrlButton(null, "Cerca su", Modifier.weight(1f), onSeekUp, textIcon = "»")
                }
                Spacer(Modifier.height(4.dp))
            }

            // --- Preferiti: 6 posti fissi ---
            Column(Modifier.weight(0.48f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PREFERITI", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, color = colors.primary)
                for (row in 0 until 3) {
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (col in 0 until 2) {
                            val i = row * 2 + col
                            val p = presets.getOrNull(i)
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                if (p != null) PresetTile(i + 1, p, selected = khz == p.khz, onTune, onRemove)
                                else EmptyTile(i + 1, canSave = onSaveCurrent != null && khz != null,
                                    onClick = { if (onSaveCurrent != null && khz != null) onSaveCurrent() else onAdd() })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/** Scala di sintonia 87,5–108 MHz con l'indicatore sulla frequenza attuale. */
@Composable
private fun Dial(khz: Int?) {
    val colors = MaterialTheme.colorScheme
    val tick = colors.onSurfaceVariant.copy(alpha = 0.55f)
    val accent = colors.primary
    Canvas(Modifier.fillMaxWidth().height(34.dp)) {
        val lo = RadioPresetLogic.FM_MIN_KHZ; val hi = RadioPresetLogic.FM_MAX_KHZ
        val n = (hi - lo) / 100
        for (i in 0..n step 2) {
            val x = size.width * i / n
            val major = i % 10 == 0
            val h = if (major) size.height * 0.55f else size.height * 0.3f
            drawLine(tick, Offset(x, size.height - h), Offset(x, size.height), strokeWidth = if (major) 3f else 2f, cap = StrokeCap.Round)
        }
        if (khz != null && khz in lo..hi) {
            val x = size.width * (khz - lo).toFloat() / (hi - lo)
            drawRoundRect(accent, Offset(x - 3f, 0f), Size(6f, size.height), CornerRadius(3f))
        }
    }
}

@Composable
private fun CtrlButton(
    icon: ImageVector?, desc: String, modifier: Modifier, onClick: () -> Unit,
    highlighted: Boolean = false, textIcon: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp),
        color = if (highlighted) colors.primary else colors.surfaceVariant,
        contentColor = if (highlighted) colors.onPrimary else colors.onSurface,
        modifier = modifier.height(64.dp)) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, desc, modifier = Modifier.size(30.dp))
            else Text(textIcon ?: "", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PresetTile(slot: Int, p: RadioPreset, selected: Boolean, onTune: (RadioPreset) -> Unit, onRemove: (RadioPreset) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = { onTune(p) }, shape = RoundedCornerShape(20.dp),
        color = if (selected) colors.primary.copy(alpha = 0.22f) else colors.surface,
        modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Column(Modifier.align(Alignment.CenterStart)) {
                Text("P$slot", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) colors.primary else colors.onSurfaceVariant)
                Text(p.displayFrequency(), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                Text(p.name, fontSize = 14.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.align(Alignment.TopEnd).size(30.dp).clickable { onRemove(p) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, "Rimuovi", tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun EmptyTile(slot: Int, canSave: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = colors.surface.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("P$slot", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
            Icon(Icons.Filled.Add, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(26.dp))
            Text(if (canSave) "Salva questa" else "Aggiungi", fontSize = 12.sp, color = colors.onSurfaceVariant)
        }
    }
}

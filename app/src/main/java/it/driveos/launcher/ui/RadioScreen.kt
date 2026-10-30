package it.driveos.launcher.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.radio.RadioPreset
import it.driveos.launcher.radio.RadioState
import it.driveos.launcher.ui.icons.PauseIcon

/**
 * Radio INTEGRATA in DriveOS: resta qui, non apre l'app Junsun. Mostra la frequenza solo
 * se il firmware la pubblica (niente valori finti); i comandi sono best-effort finché non
 * abbiamo i parametri esatti di questa radio (si ricavano dall'export).
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
) {
    val colors = MaterialTheme.colorScheme
    val freqText = state?.khz?.let { RadioPreset(it, "").displayFrequency() } ?: "—"
    val playing = state?.playing == true

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Radio", onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Display frequenza
            Surface(shape = RoundedCornerShape(22.dp), color = colors.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(freqText, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        Spacer(Modifier.width(8.dp))
                        Text("MHz", fontSize = 20.sp, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
                    }
                    Text(
                        if (detected) (if (playing) "In riproduzione" else "In pausa")
                        else "Sintonizzatore non ancora rilevato su questo firmware",
                        fontSize = 14.sp, color = if (detected) colors.primary else colors.onSurfaceVariant,
                    )
                }
            }

            // Comandi
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CtrlButton(null, "Cerca giù", Modifier.weight(1f), onSeekDown, textIcon = "«")
                CtrlButton(null, "Passo giù", Modifier.weight(0.7f), onStepDown, textIcon = "‹")
                CtrlButton(if (playing) PauseIcon else Icons.Filled.PlayArrow, "Play/Pausa", Modifier.weight(1f), onPlayPause, highlighted = true)
                CtrlButton(null, "Passo su", Modifier.weight(0.7f), onStepUp, textIcon = "›")
                CtrlButton(null, "Cerca su", Modifier.weight(1f), onSeekUp, textIcon = "»")
            }

            if (!detected) {
                Surface(shape = RoundedCornerShape(14.dp), color = colors.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("DriveOS non riceve ancora lo stato della radio da questo firmware. " +
                            "I comandi qui sopra provano i protocolli più comuni; se non funzionano, " +
                            "esporta i dati (Veicolo → Esporta) così li adattiamo a questa radio esatta.",
                            fontSize = 14.sp, color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Surface(onClick = onOpenExternal, shape = RoundedCornerShape(12.dp),
                            color = colors.surface, contentColor = colors.onSurface, modifier = Modifier.fillMaxWidth().height(46.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("Apri l'app radio Junsun (ripiego)", fontSize = 15.sp) }
                        }
                    }
                }
            }

            // Preset
            Text("CANALI MEMORIZZATI", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp,
                color = colors.primary, modifier = Modifier.padding(top = 4.dp))
            if (presets.isEmpty()) {
                Text("Nessun canale salvato. Tocca «Aggiungi canale».", fontSize = 15.sp, color = colors.onSurfaceVariant)
            } else {
                LazyVerticalGrid(columns = GridCells.Adaptive(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    items(presets, key = { "${it.band}${it.khz}" }) { p ->
                        PresetTile(p, selected = currentKhz == p.khz, onTune, onRemove)
                    }
                }
            }

            if (onSaveCurrent != null && state?.khz != null) {
                Surface(onClick = onSaveCurrent, shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant, contentColor = colors.onSurface,
                    modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Salva questa stazione come preferita ($freqText MHz) · max 6", fontSize = 16.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Surface(onClick = onAdd, shape = RoundedCornerShape(16.dp), color = colors.primary, contentColor = colors.onPrimary,
                modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Aggiungi canale (frequenza attuale: $freqText)", fontSize = 16.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CtrlButton(
    icon: ImageVector?, desc: String, modifier: Modifier, onClick: () -> Unit,
    highlighted: Boolean = false, textIcon: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp),
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
private fun PresetTile(p: RadioPreset, selected: Boolean, onTune: (RadioPreset) -> Unit, onRemove: (RadioPreset) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = { onTune(p) }, shape = RoundedCornerShape(18.dp),
        color = if (selected) colors.primary.copy(alpha = 0.20f) else colors.surface,
        modifier = Modifier.height(92.dp)) {
        Box(Modifier.fillMaxSize().padding(14.dp)) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(p.displayFrequency(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    if (p.displayUnit().isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        Text(p.displayUnit(), fontSize = 13.sp, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                Text(p.name, fontSize = 15.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.align(Alignment.TopEnd).size(28.dp).clickable { onRemove(p) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, "Rimuovi", tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

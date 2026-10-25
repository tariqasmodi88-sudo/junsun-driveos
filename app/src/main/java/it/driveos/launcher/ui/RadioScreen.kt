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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.radio.RadioPreset

/**
 * Radio: memorizza i canali (indipendente dall'app Junsun). Toccare un preset apre l'app
 * radio; se la scansione ha trovato un comando di sintonizzazione lo usa, altrimenti la
 * frequenza resta un promemoria (DriveOS non può pilotare il tuner senza quel comando).
 */
@Composable
fun RadioScreen(
    presets: List<RadioPreset>,
    radioConfigured: Boolean,
    canTune: Boolean,
    onPlay: (RadioPreset) -> Unit,
    onAdd: () -> Unit,
    onRemove: (RadioPreset) -> Unit,
    onOpenRadioApp: () -> Unit,
    onChooseRadioApp: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Radio", onBack) {
            ActionButton("Apri radio", Icons.Filled.PlayArrow, onClick = onOpenRadioApp,
                modifier = Modifier.width(200.dp), highlighted = true)
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Text(
                if (!radioConfigured) "Scegli l'app radio Junsun qui sotto, poi salva i tuoi canali."
                else if (canTune) "Tocca un canale per sintonizzarlo."
                else "Tocca un canale per aprire la radio sulla frequenza salvata.",
                fontSize = 15.sp, color = colors.onSurfaceVariant,
            )

            if (presets.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("Nessun canale salvato", fontSize = 18.sp, color = colors.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Adaptive(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    items(presets, key = { "${it.band}${it.khz}" }) { p -> PresetTile(p, onPlay, onRemove) }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(onClick = onAdd, shape = RoundedCornerShape(16.dp),
                    color = colors.primary, contentColor = colors.onPrimary,
                    modifier = Modifier.weight(1f).height(56.dp)) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Aggiungi canale", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Surface(onClick = onChooseRadioApp, shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceVariant, contentColor = colors.onSurface,
                    modifier = Modifier.weight(1f).height(56.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("App radio…", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PresetTile(p: RadioPreset, onPlay: (RadioPreset) -> Unit, onRemove: (RadioPreset) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = { onPlay(p) }, shape = RoundedCornerShape(18.dp), color = colors.surface,
        modifier = Modifier.height(96.dp)) {
        Box(Modifier.fillMaxSize().padding(14.dp)) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(p.displayFrequency(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    if (p.displayUnit().isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        Text(p.displayUnit(), fontSize = 14.sp, color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                Text(p.name, fontSize = 15.sp, color = colors.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.align(Alignment.TopEnd).size(28.dp).clickable { onRemove(p) },
                contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, "Rimuovi", tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

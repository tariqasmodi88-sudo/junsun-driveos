package it.driveos.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.controls.SystemControls

/**
 * Pannello controlli: volume e luminosità con grafica DriveOS, al posto delle finestrelle
 * di sistema. Compare dall'alto; agisce sul volume reale e sulla luminosità dello schermo.
 */
@Composable
fun ControlsPanel(
    volume: SystemControls.VolumeState,
    brightness: Float,
    canSystemBrightness: Boolean,
    onVolume: (Int) -> Unit,
    onVolumeStep: (Int) -> Unit,
    onBrightness: (Float) -> Unit,
    onOpenSoundSettings: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface, shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Controlli rapidi", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface, modifier = Modifier.weight(1f))
                Surface(onClick = onClose, shape = RoundedCornerShape(12.dp), color = colors.surfaceVariant,
                    modifier = Modifier.height(44.dp).width(80.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Chiudi", tint = colors.onSurface)
                    }
                }
            }

            // Volume
            Text("Volume   ${volume.percent}%", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton("−") { onVolumeStep(-1) }
                Slider(
                    value = volume.percent.toFloat(),
                    onValueChange = { onVolume(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary),
                )
                StepButton("+") { onVolumeStep(1) }
            }

            // Luminosità
            Text("Luminosità" + if (!canSystemBrightness) " (solo DriveOS)" else "",
                fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.onSurface)
            Slider(
                value = if (brightness < 0f) 0.5f else brightness,
                onValueChange = onBrightness,
                valueRange = 0.05f..1f,
                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary),
            )

            Surface(onClick = onOpenSoundSettings, shape = RoundedCornerShape(12.dp),
                color = colors.surfaceVariant, contentColor = colors.onSurface, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Impostazioni audio di sistema", fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = colors.surfaceVariant,
        contentColor = colors.onSurface, modifier = Modifier.size(52.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Piccola barra in alto sulla Home con accessi rapidi. */
@Composable
fun QuickBar(
    volumePercent: Int,
    autoThemeLabel: String?,
    onControls: () -> Unit,
    onVehicle: () -> Unit,
    onRadio: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        QuickChip("Veicolo", Modifier.weight(1f), onVehicle)
        QuickChip("Radio", Modifier.weight(1f), onRadio)
        Surface(onClick = onControls, shape = RoundedCornerShape(14.dp), color = colors.surfaceVariant,
            contentColor = colors.onSurface, modifier = Modifier.weight(1f).height(44.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Volume $volumePercent%", fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun QuickChip(label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = colors.surfaceVariant,
        contentColor = colors.onSurface, modifier = modifier.height(44.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

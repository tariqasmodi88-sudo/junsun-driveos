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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.vehicle.SignalIds

/**
 * Schermata Debug del veicolo: stato della seriale auto e della lettura dei log Junsun,
 * registrazione continua con export, elenco (sola lettura) dei segnali letti ora.
 */
@Composable
fun VehicleScreen(
    signals: Map<String, String>,
    logsGranted: Boolean,
    loggingOn: Boolean,
    loggedChanges: Int,
    onToggleLogging: (Boolean) -> Unit,
    canSerialOn: Boolean,
    onToggleCanSerial: (Boolean) -> Unit,
    onOpenKeys: () -> Unit,
    onExportLog: () -> Unit,
    onClearLog: () -> Unit,
    exportStatus: String?,
    onBack: () -> Unit,
) {
    var showAll by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val canSignals = signals.filterKeys { it.startsWith("can:") }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Debug veicolo", onBack) {}
        exportStatus?.let {
            Text(it, fontSize = 14.sp, color = colors.primary, modifier = Modifier.padding(horizontal = 20.dp))
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Secondary("Tasti e volante (impara i tasti)", Modifier.fillMaxWidth()) { onOpenKeys() }

            Surface(shape = RoundedCornerShape(16.dp), color = if (canSerialOn) colors.primary.copy(alpha = 0.14f) else colors.surface,
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Seriale auto", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Text(
                            if (canSerialOn) "Attiva · ${canSignals.size} valori letti. Porte, baule, freno a mano, retromarcia, batteria, sensori."
                            else "Spenta. Accendila per leggere porte, baule, freno a mano e batteria.",
                            fontSize = 14.sp, color = colors.onSurfaceVariant,
                        )
                        Text("Sola lettura. Se la radio nativa si comporta male, spegni qui.",
                            fontSize = 13.sp, color = colors.onSurfaceVariant)
                    }
                    androidx.compose.material3.Switch(checked = canSerialOn, onCheckedChange = onToggleCanSerial)
                }
            }

            Surface(shape = RoundedCornerShape(16.dp), color = if (loggingOn) colors.primary.copy(alpha = 0.14f) else colors.surface,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Registrazione continua", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                            Text(
                                if (loggingOn) "Attiva · $loggedChanges variazioni registrate (anche a schermo spento)"
                                else "Registra le variazioni per giorni, poi esporti un file solo",
                                fontSize = 14.sp, color = colors.onSurfaceVariant,
                            )
                        }
                        androidx.compose.material3.Switch(checked = loggingOn, onCheckedChange = onToggleLogging)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Secondary("Esporta registro", Modifier.weight(1f)) { onExportLog() }
                        Secondary("Azzera registro", Modifier.weight(1f)) { onClearLog() }
                    }
                }
            }

            Surface(shape = RoundedCornerShape(16.dp), color = if (logsGranted) colors.primary.copy(alpha = 0.14f) else colors.surface,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (logsGranted) "Lettura log Junsun: attiva ✓" else "Lettura log Junsun: non attiva",
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    if (!logsGranted) {
                        Spacer(Modifier.height(6.dp))
                        Text("Serve per radio e finestre Junsun. Si concede una volta via ADB, poi riapri DriveOS:",
                            fontSize = 14.sp, color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Surface(shape = RoundedCornerShape(10.dp), color = colors.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                                Text("adb shell pm grant it.driveos.launcher android.permission.READ_LOGS",
                                    fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                                    color = colors.onSurface, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }

            Text("SEGNALI LETTI ORA (${signals.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp, color = colors.primary, modifier = Modifier.padding(top = 12.dp))
            val ordered = (canSignals.entries + signals.entries.filterNot { it.key.startsWith("can:") }).toList()
            val shown = if (showAll) ordered else ordered.take(20)
            Column {
                shown.forEach { (id, value) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (id.startsWith("can:")) id else SignalIds.describe(id), fontSize = 14.sp, color = colors.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(10.dp))
                        Text(value, fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = colors.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
                    }
                }
            }
            if (!showAll && ordered.size > 20) {
                Text("Mostra tutti (${ordered.size})", fontSize = 15.sp, color = colors.primary,
                    modifier = Modifier.clickable { showAll = true }.padding(vertical = 6.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Secondary(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.height(52.dp)) {
        Box(Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

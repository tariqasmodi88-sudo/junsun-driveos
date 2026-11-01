package it.driveos.launcher.ui

import it.driveos.launcher.i18n.tr
import it.driveos.launcher.i18n.trf

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
    frontSensorsOn: Boolean = false,
    onToggleFrontSensors: (Boolean) -> Unit = {},
    touchLimitOn: Boolean = true,
    rearCameraOn: Boolean = false,
    onToggleRearCamera: (Boolean) -> Unit = {},
    onToggleTouchLimit: (Boolean) -> Unit = {},
) {
    var showAll by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val canSignals = signals.filterKeys { it.startsWith("can:") }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(tr("Veicolo"), onBack) {}
        exportStatus?.let {
            Text(it, fontSize = 14.sp, color = colors.primary, modifier = Modifier.padding(horizontal = 20.dp))
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Secondary(tr("Tasti e volante (impara i tasti)"), Modifier.fillMaxWidth()) { onOpenKeys() }

            Surface(shape = RoundedCornerShape(16.dp), color = if (canSerialOn) colors.primary.copy(alpha = 0.14f) else colors.surface,
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Seriale auto"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Text(
                            if (canSerialOn) trf("Attiva · {0} valori letti. Porte, baule, freno a mano, retromarcia, batteria, sensori.", canSignals.size)
                            else tr("Spenta. Accendila per leggere porte, baule, freno a mano e batteria."),
                            fontSize = 14.sp, color = colors.onSurfaceVariant,
                        )
                        Text(tr("Sola lettura. Se la radio nativa si comporta male, spegni qui."),
                            fontSize = 13.sp, color = colors.onSurfaceVariant)
                    }
                    androidx.compose.material3.Switch(checked = canSerialOn, onCheckedChange = onToggleCanSerial)
                }
            }

            Surface(shape = RoundedCornerShape(16.dp), color = if (rearCameraOn) colors.primary.copy(alpha = 0.14f) else colors.surface,
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Telecamera posteriore"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Text(if (rearCameraOn) tr("Montata: in retromarcia DriveOS mostra immagine, linee guida e sensori.") else tr("Non montata: in retromarcia solo i sensori."),
                            fontSize = 14.sp, color = colors.onSurfaceVariant)
                    }
                    androidx.compose.material3.Switch(checked = rearCameraOn, onCheckedChange = onToggleRearCamera)
                }
            }

            Surface(shape = RoundedCornerShape(16.dp), color = if (touchLimitOn) colors.primary.copy(alpha = 0.14f) else colors.surface,
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Limite tocchi in marcia"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Text(if (touchLimitOn) tr("Attivo: oltre i 6 tocchi ravvicinati, schermo bloccato 10 s (come Android Auto).") else tr("Spento: nessun limite durante la guida."),
                            fontSize = 14.sp, color = colors.onSurfaceVariant)
                    }
                    androidx.compose.material3.Switch(checked = touchLimitOn, onCheckedChange = onToggleTouchLimit)
                }
            }

            Surface(shape = RoundedCornerShape(16.dp), color = if (frontSensorsOn) colors.primary.copy(alpha = 0.14f) else colors.surface,
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Sensori di parcheggio anteriori"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Text(if (frontSensorsOn) tr("Montati: si disegnano a bassa velocità.") else tr("Non montati: non si disegnano."),
                            fontSize = 14.sp, color = colors.onSurfaceVariant)
                    }
                    androidx.compose.material3.Switch(checked = frontSensorsOn, onCheckedChange = onToggleFrontSensors)
                }
            }

            Surface(shape = RoundedCornerShape(16.dp), color = if (logsGranted) colors.primary.copy(alpha = 0.14f) else colors.surface,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (logsGranted) tr("Lettura log Junsun: attiva ✓") else tr("Lettura log Junsun: non attiva"),
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    if (!logsGranted) {
                        Spacer(Modifier.height(6.dp))
                        Text(tr("Serve per radio e finestre Junsun. Si concede una volta via ADB, poi riapri DriveOS:"),
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

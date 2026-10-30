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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import it.driveos.launcher.settings.DriveSettings
import it.driveos.launcher.vehicle.Candidate
import it.driveos.launcher.vehicle.SignalBinding
import it.driveos.launcher.vehicle.SignalIds
import it.driveos.launcher.vehicle.SignalRole

/**
 * Schermata Veicolo: mostra i segnali che il firmware pubblica, scopre le azioni delle app
 * Junsun, fa indicare quale segnale sono i fari (per il tema automatico) e quali valori
 * mostrare sul cruscotto (carburante, pneumatici, warning). Nessun dato finto.
 */
@Composable
fun VehicleScreen(
    settings: DriveSettings,
    signals: Map<String, String>,
    scanning: Boolean,
    scanResult: String?,
    lightsResolved: Boolean?,
    candidates: List<Candidate>,
    recordedOn: Int,
    recordedOff: Int,
    onScanApps: () -> Unit,
    onRecordOn: () -> Unit,
    onRecordOff: () -> Unit,
    onAssignLights: (Candidate) -> Unit,
    onAddGauge: (String) -> Unit,
    onAddAlert: (String, String) -> Unit,
    onRemoveBinding: (SignalBinding) -> Unit,
    logsGranted: Boolean,
    loggingOn: Boolean,
    loggedChanges: Int,
    onToggleLogging: (Boolean) -> Unit,
    canSerialOn: Boolean = true,
    onToggleCanSerial: (Boolean) -> Unit = {},
    onExportLog: () -> Unit,
    onClearLog: () -> Unit,
    onClearFlags: () -> Unit,
    exportStatus: String?,
    onExport: () -> Unit,
    onBack: () -> Unit,
) {
    var showAll by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val gaugeIds = settings.gaugeBindings.map { it.id }.toSet()
    val alertBindings = settings.signalBindings.filter { it.role == SignalRole.ALERT }
    val alertIds = alertBindings.map { it.id }.toSet()
    val lights = settings.lightsBinding

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Veicolo (CAN)", onBack) {
            ActionButton("Esporta", Icons.Filled.Share, onClick = onExport,
                modifier = Modifier.width(150.dp), highlighted = true)
        }
        exportStatus?.let {
            Text(it, fontSize = 14.sp, color = colors.primary, modifier = Modifier.padding(horizontal = 20.dp))
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val lightsText = when (lightsResolved) {
                true -> "Fari: accesi"
                false -> "Fari: spenti"
                null -> if (lights == null) "Fari: non collegati" else "Fari: in attesa del segnale"
            }
            Banner("$lightsText · ${signals.size} segnali letti" +
                (if (settings.gaugeBindings.isNotEmpty()) " · ${settings.gaugeBindings.size} sul cruscotto" else ""))

            Surface(shape = RoundedCornerShape(16.dp), color = if (canSerialOn) colors.primary.copy(alpha = 0.14f) else colors.surface,
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Seriale auto", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Text("Porte, baule, freno a mano, batteria dal canale dell'auto (sola lettura). Se qualcosa della radio nativa si comporta male, spegni qui.",
                            fontSize = 14.sp, color = colors.onSurfaceVariant)
                    }
                    androidx.compose.material3.Switch(checked = canSerialOn, onCheckedChange = onToggleCanSerial)
                }
            }

            // --- Registrazione continua (la via consigliata per trovare tutto) ---
            Surface(shape = RoundedCornerShape(16.dp),
                color = if (loggingOn) colors.primary.copy(alpha = 0.14f) else colors.surface,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Registrazione continua", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                            Text(
                                if (loggingOn) "Attiva · $loggedChanges variazioni registrate (anche a schermo spento)"
                                else "Registra TUTTE le variazioni per giorni, poi esporti un file solo",
                                fontSize = 14.sp, color = colors.onSurfaceVariant,
                            )
                        }
                        androidx.compose.material3.Switch(checked = loggingOn, onCheckedChange = onToggleLogging)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Non serve flaggare niente: la registrazione prende da sola tutti i 1264 segnali. " +
                        "Lasciala attiva, guida come al solito (e apri la radio Junsun qualche volta), poi esporta.",
                        fontSize = 13.sp, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Secondary("Esporta registro", Modifier.weight(1f)) { onExportLog() }
                        Secondary("Azzera registro", Modifier.weight(1f)) { onClearLog() }
                    }
                }
            }

            // --- Lettura del registro di sistema (portiere, sensori, radio) ---
            Surface(shape = RoundedCornerShape(16.dp),
                color = if (logsGranted) colors.primary.copy(alpha = 0.14f) else colors.surface,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (logsGranted) "Lettura dati Junsun: ATTIVA ✓" else "Lettura dati Junsun: non attiva",
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    Spacer(Modifier.height(6.dp))
                    if (logsGranted) {
                        Text("DriveOS legge portiere, sensori di parcheggio, luci e radio dal registro " +
                            "dell'app Junsun. Compaiono sulla Home quando accadono.",
                            fontSize = 14.sp, color = colors.onSurfaceVariant)
                    } else {
                        Text("Portiere, sensori e radio su questa radio stanno dentro l'app Junsun e non " +
                            "escono come segnali normali. DriveOS può leggerli dal registro di sistema se gli " +
                            "concedi UNA volta questo permesso via ADB (niente root):",
                            fontSize = 14.sp, color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Surface(shape = RoundedCornerShape(10.dp), color = colors.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                                Text("adb shell pm grant it.driveos.launcher android.permission.READ_LOGS",
                                    fontSize = 13.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = colors.onSurface, modifier = Modifier.padding(12.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Poi riapri DriveOS. Lo puoi dare anche via ADB wireless (es. Bugjaeger).",
                            fontSize = 13.sp, color = colors.onSurfaceVariant)
                    }
                }
            }

            Label("1 · Scopri i segnali delle app Junsun")
            Hint("Legge gli APK delle app CAN/radio/MCU per trovare i canali dati. Fallo a motore acceso, " +
                "muovendo il veicolo per qualche minuto: così i valori cambiano e diventano riconoscibili.")
            Primary(if (scanning) "Scansione in corso…" else "Scansiona app del veicolo",
                if (scanning) null else Icons.Filled.Add, scanning) { if (!scanning) onScanApps() }
            scanResult?.let { Text(it, fontSize = 14.sp, color = colors.primary) }

            Label("2 · Collega i fari (tema automatico)")
            Hint("Con i fari SPENTI tocca «Registra spenti», poi accendili e tocca «Registra accesi». " +
                "Ripeti una o due volte: DriveOS trova il segnale che cambia e lo propone qui sotto.")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Secondary("Registra spenti ($recordedOff)", Modifier.weight(1f)) { onRecordOff() }
                Secondary("Registra accesi ($recordedOn)", Modifier.weight(1f)) { onRecordOn() }
            }
            if (candidates.isNotEmpty()) {
                Hint("Segnali che distinguono acceso/spento (il primo è il più probabile):")
                candidates.take(6).forEach { c -> CandidateRow(c, lights?.id == c.id) { onAssignLights(c) } }
            }
            lights?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Collegati: ${SignalIds.describe(it.id)}", fontSize = 14.sp, color = colors.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    Text("Rimuovi", fontSize = 14.sp, color = colors.error,
                        modifier = Modifier.clickable { onRemoveBinding(it) })
                }
            }
            if (lights == null && lightsResolved != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Fari rilevati automaticamente su questo firmware (forfan_light_state). Il tema automatico li usa già.",
                        fontSize = 14.sp, color = colors.onSurface)
                }
            }

            Label("3 · Valori sempre visibili (cruscotto)")
            settings.gaugeBindings.forEach { g ->
                SignalRow(g.label, (signals[g.id] ?: "—") + (if (g.unit.isNotBlank()) " ${g.unit}" else ""),
                    "Rimuovi", colors.error) { onRemoveBinding(g) }
            }
            Hint("Tocca «+» su un segnale per mostrarlo sempre (carburante, temperatura, olio…).")

            Label("4 · Spie (attive solo quando scattano)")
            if (alertBindings.isEmpty()) Hint("Tocca «!» su un segnale per usarlo come spia (es. pressione pneumatici, avaria).")
            alertBindings.forEach { a ->
                val active = signals[a.id]?.let { v -> if (a.onValue != null) v == a.onValue else v != a.offValue } ?: false
                SignalRow(a.label + if (active) "  ●" else "", (signals[a.id] ?: "—"),
                    "Rimuovi", colors.error) { onRemoveBinding(a) }
            }

            Label("Segnali letti ora (${signals.size})")
            Hint("I flag sono facoltativi: «+» fissa un valore sulla Home, «!» lo usa come spia. " +
                "Per l'analisi non serve: basta la registrazione continua qui sopra.")
            if (settings.signalBindings.isNotEmpty()) {
                Text("Azzera tutti i flag (fari/valori/spie)", fontSize = 15.sp, color = colors.error,
                    modifier = Modifier.clickable { onClearFlags() }.padding(vertical = 4.dp))
            }
            if (signals.isEmpty()) {
                Hint("Nessun segnale ancora. Avvia la scansione e muovi il veicolo.")
            } else {
                val shown = if (showAll) signals.entries.toList() else signals.entries.take(14)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    items(shown, key = { it.key }) { (id, value) ->
                        LiveSignalRow(
                            name = SignalIds.describe(id), value = value,
                            isGauge = id in gaugeIds, isAlert = id in alertIds,
                            onGauge = { onAddGauge(id) }, onAlert = { onAddAlert(id, value) },
                        )
                    }
                }
                if (!showAll && signals.size > 14) {
                    Text("Mostra tutti (${signals.size})", fontSize = 15.sp, color = colors.primary,
                        modifier = Modifier.clickable { showAll = true }.padding(vertical = 6.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Banner(text: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun Hint(text: String) {
    Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Primary(label: String, icon: ImageVector?, loading: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Row(Modifier.padding(horizontal = 16.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary)
            else if (icon != null) Icon(icon, null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.Medium)
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

@Composable
private fun CandidateRow(c: Candidate, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.primary.copy(alpha = 0.18f) else colors.surface,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(SignalIds.describe(c.id), fontSize = 15.sp, fontWeight = FontWeight.Medium,
                color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("acceso = ${c.onValue}   spento = ${c.offValue}", fontSize = 13.sp,
                color = colors.onSurfaceVariant, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun SignalRow(name: String, value: String, trailing: String, trailingColor: Color, onTrailing: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, fontSize = 15.sp, color = colors.onSurface, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Text(value, fontSize = 15.sp, fontFamily = FontFamily.Monospace, color = colors.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(110.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.clickable { onTrailing() }.padding(horizontal = 4.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
            Text(trailing, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = trailingColor)
        }
    }
}

@Composable
private fun LiveSignalRow(name: String, value: String, isGauge: Boolean, isAlert: Boolean, onGauge: () -> Unit, onAlert: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, fontSize = 15.sp, color = colors.onSurface, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(value, fontSize = 15.sp, fontFamily = FontFamily.Monospace, color = colors.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(96.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(34.dp).clickable(enabled = !isGauge) { onGauge() }, contentAlignment = Alignment.Center) {
            Text(if (isGauge) "✓" else "+", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = if (isGauge) colors.primary else colors.onSurface)
        }
        Box(Modifier.size(34.dp).clickable(enabled = !isAlert) { onAlert() }, contentAlignment = Alignment.Center) {
            Text("!", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = if (isAlert) colors.error else colors.onSurfaceVariant)
        }
    }
}

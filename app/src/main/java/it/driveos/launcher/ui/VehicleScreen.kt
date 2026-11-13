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
import it.driveos.launcher.vehicle.CanDecoder
import it.driveos.launcher.vehicle.KnownVehicleSignals
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
    onAutoSetup: (() -> Unit)? = null,
    bridge: it.driveos.launcher.junsun.TsBridge? = null,
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

            if (bridge != null) CarServiceProbe(bridge, signals)

            Surface(shape = RoundedCornerShape(16.dp), color = if (logsGranted) colors.primary.copy(alpha = 0.14f) else colors.surface,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (logsGranted) tr("Lettura log Junsun: attiva ✓") else tr("Lettura log Junsun: non attiva"),
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    if (!logsGranted) {
                        Spacer(Modifier.height(6.dp))
                        Text(tr("Serve per radio e finestre Junsun. Si concede una volta via ADB, poi riapri DriveOS:"),
                            fontSize = 14.sp, color = colors.onSurfaceVariant)
                        if (onAutoSetup != null) {
                            Spacer(Modifier.height(10.dp))
                            Surface(onClick = onAutoSetup, shape = RoundedCornerShape(12.dp), color = colors.primary, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(tr("Concedi da solo"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.onPrimary)
                                    Text(tr("Se la radio chiede \"Consentire il debug?\" tocca Consenti sempre. DriveOS poi si riapre."),
                                        fontSize = 13.sp, color = colors.onPrimary.copy(alpha = 0.85f))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(tr("Oppure dal telefono:"), fontSize = 13.sp, color = colors.onSurfaceVariant)
                        }
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

/**
 * PROVA: confronto tra i dati che la radio Junsun offre al suo assistente vocale e quelli che DriveOS
 * legge dalla seriale. Interroga la radio solo finche' l'interruttore e' acceso e la pagina e' aperta.
 * Ogni variazione finisce anche nel registro (etichetta "DriveOSCar") per poterla mandare.
 */
@Composable
private fun CarServiceProbe(bridge: it.driveos.launcher.junsun.TsBridge, signals: Map<String, String>) {
    val colors = MaterialTheme.colorScheme
    var on by remember { mutableStateOf(false) }
    var probe by remember { mutableStateOf<it.driveos.launcher.junsun.TsBridge.CarProbe?>(null) }
    androidx.compose.runtime.LaunchedEffect(on) {
        if (!on) { probe = null; bridge.carDisconnect(); return@LaunchedEffect }
        bridge.carConnect()
        var lastLine = ""
        while (true) {
            val p = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { bridge.carProbe() }.getOrNull() }
            probe = p
            if (p != null) {
                val line = "porte=${p.doors} sensori=${p.radar} volante=${p.steer} frecce=${p.left}/${p.right}/${p.emergency} " +
                    "luci=${p.ill} retro=${p.reverse} freno=${p.brake} err=${p.error}"
                if (line != lastLine) { lastLine = line; android.util.Log.i("DriveOSCar", line) }
            }
            kotlinx.coroutines.delay(500)
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { bridge.carDisconnect() } }

    Surface(shape = RoundedCornerShape(16.dp), color = if (on) colors.primary.copy(alpha = 0.14f) else colors.surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Prova: dati dalla radio Junsun"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    Text(tr("Confronta quello che dice la radio con quello che DriveOS legge dalla seriale. Prova con «Seriale auto» accesa e poi spenta."),
                        fontSize = 14.sp, color = colors.onSurfaceVariant)
                }
                androidx.compose.material3.Switch(checked = on, onCheckedChange = { on = it })
            }
            val p = probe
            if (on && p != null) {
                Spacer(Modifier.height(10.dp))
                fun yes(v: Int?) = when (v) { null -> "—"; 0 -> "no"; else -> "sì" }
                fun sig(k: String) = signals[k] ?: "—"
                val names = listOf("ant.sx", "ant.dx", "post.sx", "post.dx", "baule", "cofano")
                val doorsSvc = p.doors?.let { d -> names.filterIndexed { i, _ -> (d.getOrNull(i) ?: 0) != 0 }.joinToString(", ").ifEmpty { "chiuse" } } ?: "—"
                val doorsCan = CanDecoder.openDoors(signals)?.joinToString(", ")?.ifEmpty { "chiuse" } ?: "—"
                val rows = listOf(
                    Triple("Porte", doorsSvc, doorsCan),
                    Triple("Sensori post.", p.radar?.drop(4)?.joinToString(" ") ?: "—", sig(CanDecoder.REAR_SENSORS)),
                    Triple("Sensori ant.", p.radar?.take(4)?.joinToString(" ") ?: "—", sig(CanDecoder.FRONT_SENSORS)),
                    Triple("Retromarcia", yes(p.reverse), when (KnownVehicleSignals.reverseOn(signals)) { null -> "—"; true -> "sì"; else -> "no" }),
                    Triple("Luci (posizione)", yes(p.ill), signals["prop:forfan_light_state"] ?: "—"),
                    Triple("Freno a mano", yes(p.brake), when (signals[CanDecoder.HANDBRAKE]) { null -> "—"; "1" -> "sì"; else -> "no" }),
                    Triple("Volante", p.steer?.toString() ?: "—", sig(CanDecoder.STEER)),
                    Triple("Frecce sx / dx / 4", "${yes(p.left)} / ${yes(p.right)} / ${yes(p.emergency)}", "non lette"),
                )
                Row(Modifier.fillMaxWidth()) {
                    Text("", Modifier.weight(1f))
                    Text(tr("Radio Junsun"), Modifier.weight(1.3f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                    Text(tr("DriveOS (seriale)"), Modifier.weight(1.3f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
                }
                rows.forEach { (label, a, b) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(tr(label), Modifier.weight(1f), fontSize = 15.sp, color = colors.onSurfaceVariant)
                        Text(a, Modifier.weight(1.3f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, color = colors.onSurface)
                        Text(b, Modifier.weight(1.3f), fontSize = 15.sp, fontFamily = FontFamily.Monospace, color = colors.onSurface)
                    }
                }
                p.error?.let { Text(it, fontSize = 13.sp, color = colors.error) }
            }
        }
    }
}

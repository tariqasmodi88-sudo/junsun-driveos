package it.driveos.launcher.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.vehicle.CanDecoder

/**
 * Stato del climatizzatore letto dalla centralina CAN (solo lettura: con questa centralina la radio
 * non puo' comandare il clima, si regola con i tasti dell'auto e qui si vede subito).
 */
@Composable
fun ClimateScreen(signals: Map<String, String>, onBack: () -> Unit) {
    val c = MaterialTheme.colorScheme
    fun on(k: String) = signals[k] == "1"
    val known = signals.containsKey(CanDecoder.AC_FAN)
    val power = signals[CanDecoder.AC_POWER] != "0"
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Clima", onBack)
        if (!known) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text("In attesa dei dati del climatizzatore.", fontSize = 24.sp, color = c.onSurface)
                Text("Arrivano dalla centralina dell'auto appena tocchi un comando del clima (quadro acceso).",
                    fontSize = 17.sp, color = c.onSurfaceVariant)
            }
            return@Column
        }
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TempPanel("Guidatore", CanDecoder.acTemp(signals[CanDecoder.AC_TEMP_L]), signals[CanDecoder.SEAT_HEAT_L], power, Modifier.weight(1f))
            Column(Modifier.weight(1.4f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(22.dp), color = c.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("VENTOLA", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, color = c.primary)
                        Spacer(Modifier.height(10.dp))
                        val fan = signals[CanDecoder.AC_FAN]?.toIntOrNull() ?: 0
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                            for (i in 1..7) Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (power && i <= fan) c.primary else c.surfaceVariant,
                                modifier = Modifier.weight(1f).height((14 + i * 6).dp),
                            ) {}
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("FLUSSO ARIA", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, color = c.primary)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip("Parabrezza", on(CanDecoder.AC_AIR_UP), Modifier.weight(1f))
                            Chip("Viso", on(CanDecoder.AC_AIR_MID), Modifier.weight(1f))
                            Chip("Piedi", on(CanDecoder.AC_AIR_DOWN), Modifier.weight(1f))
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(22.dp), color = c.surface, modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(if (power) "ACCESO" else "OFF", power, Modifier.weight(1f))
                            Chip("AUTO", signals[CanDecoder.AC_AUTO] != "0", Modifier.weight(1f))
                            Chip("A/C", on(CanDecoder.AC_COMP), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(if (on(CanDecoder.AC_DUAL)) "DUAL" else "SYNC", true, Modifier.weight(1f))
                            Chip("Ricircolo", on(CanDecoder.AC_RECIRC), Modifier.weight(1f))
                            Chip("MAX sbrin.", on(CanDecoder.AC_MAXDEF), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip("Lunotto", on(CanDecoder.AC_REARDEF), Modifier.weight(1f))
                            Spacer(Modifier.weight(2.08f))
                        }
                    }
                }
            }
            TempPanel("Passeggero", CanDecoder.acTemp(signals[CanDecoder.AC_TEMP_R]), signals[CanDecoder.SEAT_HEAT_R], power, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TempPanel(label: String, temp: String?, seat: String?, power: Boolean, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(22.dp), color = c.surface, modifier = modifier.fillMaxHeight()) {
        Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Text(label.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, color = c.primary)
            Row(verticalAlignment = Alignment.Top) {
                Text(if (power) temp ?: "—" else "OFF", fontSize = 64.sp, fontWeight = FontWeight.Light, color = c.onSurface)
                if (power && temp != null && temp != "LO" && temp != "HI") Text("°C", fontSize = 20.sp, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
            }
            val lv = seat?.toIntOrNull() ?: 0
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sedile riscaldato", fontSize = 14.sp, color = c.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1..3) Surface(shape = RoundedCornerShape(5.dp), color = if (i <= lv) Color3 else c.surfaceVariant,
                        modifier = Modifier.width(26.dp).height(10.dp)) {}
                }
            }
        }
    }
}

private val Color3 = androidx.compose.ui.graphics.Color(0xFFFF9A3D)

@Composable
private fun Chip(text: String, active: Boolean, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(14.dp), color = if (active) c.primary.copy(alpha = 0.22f) else c.surfaceVariant.copy(alpha = 0.6f), modifier = modifier.height(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (active) c.primary else c.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

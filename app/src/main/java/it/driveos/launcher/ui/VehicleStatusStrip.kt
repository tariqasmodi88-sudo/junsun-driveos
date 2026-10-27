package it.driveos.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.vehicle.ActiveAlert
import it.driveos.launcher.vehicle.Gauge

/**
 * Striscia di stato veicolo sulla Home: valori sempre visibili (fari, carburante, temperature…)
 * e spie attive in questo momento. Mostra solo ciò che è realmente disponibile.
 */
@Composable
fun VehicleStatusStrip(
    alerts: List<ActiveAlert>,
    onOpenVehicle: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            onClick = onOpenVehicle,
            shape = RoundedCornerShape(14.dp),
            color = colors.surface,
            modifier = Modifier.weight(1f).height(46.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                alerts.forEach { a -> Pill("⚠", a.label, colors.error, warning = true) }
            }
        }
    }
}

@Composable
private fun Pill(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color, warning: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (warning) colors.error.copy(alpha = 0.16f) else colors.surfaceVariant,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!warning) {
                Text(label, fontSize = 12.sp, color = colors.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
            }
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = valueColor, maxLines = 1)
        }
    }
}

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.vehicle.KeyAction
import it.driveos.launcher.vehicle.KeyBinding

/**
 * Tasti dell'autoradio e del volante: per ogni azione tocca «Impara» e premi il tasto.
 * DriveOS legge la seriale dell'auto e ricorda quale segnale scatta.
 */
@Composable
fun KeysScreen(
    bindings: List<KeyBinding>,
    learning: KeyAction?,
    message: String?,
    onLearn: (KeyAction) -> Unit,
    onClear: (KeyAction) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Tasti e volante", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Tocca «Impara», poi premi il tasto (dell'autoradio o del volante) entro 8 secondi. " +
                "Funziona solo con i tasti che passano dalla seriale dell'auto.",
                fontSize = 14.sp, color = colors.onSurfaceVariant)
            message?.let {
                Surface(shape = RoundedCornerShape(14.dp), color = colors.primary.copy(alpha = 0.14f), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(it, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.onSurface, modifier = Modifier.weight(1f))
                        if (learning != null) Text("Annulla", fontSize = 15.sp, color = colors.error,
                            modifier = Modifier.clickableNoRipple(onCancel).padding(start = 12.dp))
                    }
                }
            }
            KeyAction.entries.forEach { a ->
                val b = bindings.firstOrNull { it.action == a }
                Surface(shape = RoundedCornerShape(14.dp),
                    color = if (learning == a) colors.primary.copy(alpha = 0.18f) else colors.surface,
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(a.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                            Text(b?.let { "${it.signal} = ${it.value}" } ?: "non assegnato", fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace, color = colors.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (b != null) {
                            Text("Cancella", fontSize = 14.sp, color = colors.error,
                                modifier = Modifier.clickableNoRipple { onClear(a) }.padding(end = 14.dp))
                        }
                        Surface(onClick = { onLearn(a) }, shape = RoundedCornerShape(12.dp),
                            color = colors.primary, contentColor = colors.onPrimary) {
                            Text(if (learning == a) "In ascolto…" else "Impara", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable { onClick() })

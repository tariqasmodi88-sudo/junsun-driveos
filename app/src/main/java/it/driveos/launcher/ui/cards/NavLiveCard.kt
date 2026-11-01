package it.driveos.launcher.ui.cards

import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.apps.AppEntry
import it.driveos.launcher.nav.KnownNavApps
import it.driveos.launcher.nav.NavGuidance
import it.driveos.launcher.nav.NavProgress
import it.driveos.launcher.nav.NavRoute
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Cosa mostrare nel pannello manovra, da qualunque fonte arrivi. */
private data class Maneuver(
    val turn: NavProgress.Turn?,
    val bitmap: ImageBitmap?,
    val big: String,
    val street: String,
    val hint: String,
    val then: NavProgress.Turn? = null,
)

/** Le notifiche "di stato" (Waze: "In funzione. Clicca per aprire.") non sono indicazioni. */
private fun isStatusText(s: String): Boolean {
    val t = s.lowercase(Locale.ITALY)
    return t.isBlank() || listOf("in funzione", "clicca", "tocca", "running", "tap to").any { it in t }
}

/**
 * Scheda Navigazione durante la guida: mappa a tutta scheda, prossima manovra in alto,
 * arrivo/tempo/km in basso. Tocco ovunque = torna al navigatore; la X chiede conferma.
 * Le indicazioni continuano anche con il navigatore in secondo piano (calcolate da DriveOS se
 * l'app non le pubblica, come Waze).
 */
@Composable
internal fun LiveNavCard(
    navEntry: AppEntry,
    guidance: NavGuidance?,
    destination: String?,
    route: NavRoute.Route?,
    location: Location?,
    onResume: () -> Unit,
    onStop: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.red + colors.background.green + colors.background.blue < 1.5f
    val here = location?.let { it.latitude to it.longitude }
    val progress = remember(route, here) { if (route != null && here != null) NavProgress.compute(route, here) else null }
    val external = guidance?.takeIf { it.packageName == KnownNavApps.GOOGLE_MAPS || !isStatusText(it.title + " " + it.text) }
        ?.takeIf { it.title.isNotBlank() && !isStatusText(it.title) }

    val maneuver = when {
        progress != null -> Maneuver(progress.turn, null, NavProgress.formatDistance(progress.distanceM),
            progress.street.ifBlank { progress.instruction }, if (progress.street.isBlank()) "" else progress.instruction, progress.then)
        external != null -> Maneuver(null, external.icon, external.title, external.text, "", null)
        destination != null -> Maneuver(NavProgress.Turn.STRAIGHT, null, "In viaggio", destination,
            if (here == null) "In attesa del GPS…" else "Calcolo il percorso…")
        else -> Maneuver(null, null, navEntry.label, "Navigazione in corso", "Tocca per aprire il navigatore")
    }
    val eta = progress?.let {
        val at = SimpleDateFormat("HH:mm", Locale.ITALY).format(Date(System.currentTimeMillis() + (it.remainingS * 1000).toLong()))
        Triple(at, NavProgress.formatDuration(it.remainingS), NavProgress.formatDistance(it.remainingM))
    }
    val etaFallback = external?.subText?.takeIf { it.isNotBlank() }

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.6f)),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 170.dp || maxWidth < 220.dp
            if (!compact) {
                RouteMap(route, location, dark, Modifier.fillMaxSize(), progress?.traveledIndex ?: 0, colors.primary.toArgb())
                // sfumature: testo leggibile sopra la mappa, bordi morbidi
                Box(Modifier.fillMaxWidth().height(maxHeight * 0.45f).background(
                    Brush.verticalGradient(listOf(colors.surface.copy(alpha = 0.85f), Color.Transparent))))
                Box(Modifier.fillMaxWidth().height(maxHeight * 0.35f).align(Alignment.BottomCenter).background(
                    Brush.verticalGradient(listOf(Color.Transparent, colors.surface.copy(alpha = 0.9f)))))
            }
            // tocco su tutta la scheda: torna al navigatore
            Box(Modifier.matchParentSize().clickable(onClick = onResume))

            if (compact) {
                CompactNav(maneuver, Modifier.align(Alignment.CenterStart).padding(16.dp))
            } else {
                ManeuverPanel(maneuver, Modifier.align(Alignment.TopStart).padding(12.dp).widthIn(max = 520.dp).fillMaxWidth())
                BottomBar(navEntry, eta, etaFallback, onStop, Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ManeuverIconBox(m: Maneuver, size: androidx.compose.ui.unit.Dp) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        when {
            m.turn != null -> ManeuverIcon(m.turn, colors.onPrimary, Modifier.size(size * 0.78f))
            m.bitmap != null -> Image(m.bitmap, null, Modifier.size(size * 0.72f))
        }
    }
}

@Composable
private fun ManeuverPanel(m: Maneuver, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.surfaceVariant.copy(alpha = 0.94f),
            shadowElevation = 6.dp,
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ManeuverIconBox(m, 76.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.big, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = colors.onSurface,
                        maxLines = 1, lineHeight = 36.sp, overflow = TextOverflow.Ellipsis)
                    Text(m.street, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (m.hint.isNotBlank()) {
                        Text(m.hint, fontSize = 15.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (m.then != null) {
            Spacer(Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = colors.surfaceVariant.copy(alpha = 0.94f)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Poi", fontSize = 15.sp, color = colors.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    ManeuverIcon(m.then, colors.onSurface, Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun BottomBar(
    navEntry: AppEntry,
    eta: Triple<String, String, String>?,
    fallback: String?,
    onStop: (() -> Unit)?,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.surfaceVariant.copy(alpha = 0.94f),
            shadowElevation = 6.dp,
            modifier = Modifier.weight(1f).height(64.dp),
        ) {
            Row(Modifier.fillMaxHeight().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(navEntry.icon, navEntry.label, Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(14.dp))
                if (eta != null) {
                    Text(eta.first, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.primary, maxLines = 1)
                    Spacer(Modifier.width(14.dp))
                    Text("${eta.second} · ${eta.third}", fontSize = 18.sp, color = colors.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                } else {
                    Text(fallback ?: "Tocca per aprire ${navEntry.label}", fontSize = 18.sp, color = colors.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
        }
        if (onStop != null) {
            Spacer(Modifier.width(10.dp))
            StopButton(onStop)
        }
    }
}

/** Primo tocco: si allarga in "Termina?"; secondo tocco entro 4 s: termina davvero. */
@Composable
private fun StopButton(onStop: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { delay(4000); armed = false } }
    val bg by animateColorAsState(if (armed) colors.error else colors.surfaceVariant.copy(alpha = 0.94f), label = "stop")
    Surface(
        onClick = { if (armed) { armed = false; onStop() } else armed = true },
        shape = CircleShape,
        color = bg,
        contentColor = if (armed) Color.White else colors.onSurface,
        shadowElevation = 6.dp,
        modifier = Modifier.height(64.dp),
    ) {
        Row(Modifier.padding(horizontal = 20.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.Close, "Termina navigazione", Modifier.size(26.dp))
            AnimatedVisibility(armed, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                Text("  Termina?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CompactNav(m: Maneuver, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        ManeuverIconBox(m, 52.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(m.big, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 1)
            Text(m.street, fontSize = 15.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

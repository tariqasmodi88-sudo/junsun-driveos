package it.driveos.launcher.ui.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import it.driveos.launcher.apps.AppEntry
import it.driveos.launcher.nav.NavGuidance

@Composable
fun NavCard(
    navEntry: AppEntry?,
    configured: Boolean,
    guidance: NavGuidance?,
    recent: List<String>,
    onOpenDetail: () -> Unit,
    onChooseApp: () -> Unit,
    onResume: () -> Unit,
    onHome: () -> Unit,
    onWork: () -> Unit,
    onDestination: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    DashCard("Navigazione", Icons.Filled.Place, onOpenDetail, modifier, trailing = navEntry?.label) { size, wide ->
        when {
            navEntry == null -> NotConfigured(configured, size, onChooseApp)
            guidance != null -> Guidance(guidance, navEntry, size, onResume)
            else -> Idle(size, wide, recent, onOpenDetail, onHome, onWork, onDestination)
        }
    }
}

@Composable
private fun ColumnScope.NotConfigured(configured: Boolean, size: CardSize, onChooseApp: () -> Unit) {
    if (size != CardSize.COMPACT) {
        Text(
            if (configured) "L'app scelta non è più installata." else "Scegli l'app con cui navigare.",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
    }
    CardButton(
        "Scegli app", Icons.Filled.Place, onChooseApp, Modifier.fillMaxWidth(),
        height = if (size == CardSize.COMPACT) 56.dp else 72.dp, primary = true,
    )
}

@Composable
private fun ColumnScope.Guidance(g: NavGuidance, navEntry: AppEntry, size: CardSize, onResume: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val compact = size == CardSize.COMPACT
    val iconSize = when (size) {
        CardSize.COMPACT -> 44.dp
        CardSize.REGULAR -> 64.dp
        CardSize.LARGE -> 96.dp
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(16.dp), color = colors.primary.copy(alpha = 0.18f), modifier = Modifier.size(iconSize)) {
            Box(contentAlignment = Alignment.Center) {
                Image(g.icon ?: navEntry.icon, contentDescription = null, modifier = Modifier.size(iconSize * 0.8f))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                g.title.ifBlank { "In navigazione" },
                fontSize = if (compact) 20.sp else if (size == CardSize.LARGE) 34.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.onSurface,
            )
            if (g.text.isNotBlank()) {
                Text(
                    g.text,
                    fontSize = if (compact) 14.sp else if (size == CardSize.LARGE) 22.sp else 18.sp,
                    maxLines = if (compact) 1 else 3,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onSurface,
                )
            }
        }
    }
    if (!compact) {
        if (g.subText.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(g.subText, fontSize = 18.sp, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.weight(1f))
        CardButton("Torna alla guida", Icons.Filled.PlayArrow, onResume, Modifier.fillMaxWidth(), height = 72.dp, primary = true)
    }
}

@Composable
private fun ColumnScope.Idle(
    size: CardSize,
    wide: Boolean,
    recent: List<String>,
    onSearch: () -> Unit,
    onHome: () -> Unit,
    onWork: () -> Unit,
    onDestination: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    when (size) {
        CardSize.COMPACT -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CardButton("Casa", Icons.Filled.Home, onHome, Modifier.weight(1f), height = 56.dp, primary = true)
            CardButton(if (wide) "Cerca" else null, Icons.Filled.Search, onSearch, Modifier.weight(1f), height = 56.dp)
        }

        CardSize.REGULAR -> {
            Text("Dove andiamo?", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface, maxLines = 1)
            Spacer(Modifier.weight(1f))
            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CardButton("Casa", Icons.Filled.Home, onHome, Modifier.weight(1f), height = 68.dp, primary = true)
                    CardButton("Lavoro", Icons.Filled.Star, onWork, Modifier.weight(1f), height = 68.dp)
                    CardButton("Cerca", Icons.Filled.Search, onSearch, Modifier.weight(1f), height = 68.dp)
                }
            } else {
                CardButton("Casa", Icons.Filled.Home, onHome, Modifier.fillMaxWidth(), height = 64.dp, primary = true)
                Spacer(Modifier.height(10.dp))
                CardButton("Lavoro", Icons.Filled.Star, onWork, Modifier.fillMaxWidth(), height = 64.dp)
                Spacer(Modifier.height(10.dp))
                CardButton("Cerca", Icons.Filled.Search, onSearch, Modifier.fillMaxWidth(), height = 64.dp)
            }
        }

        CardSize.LARGE -> {
            Text("Dove andiamo?", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface, maxLines = 1)
            Spacer(Modifier.height(14.dp))
            Surface(
                onClick = onSearch,
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(72.dp),
            ) {
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("Cerca indirizzo o luogo", fontSize = 20.sp, color = colors.onSurfaceVariant, maxLines = 1)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardButton("Casa", Icons.Filled.Home, onHome, Modifier.weight(1f), height = 76.dp, primary = true)
                CardButton("Lavoro", Icons.Filled.Star, onWork, Modifier.weight(1f), height = 76.dp)
            }
            if (recent.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                // Mostra solo le destinazioni recenti che entrano davvero nello spazio rimasto.
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val rows = ((maxHeight - 20.dp) / 56.dp).toInt().coerceIn(0, 3)
                    Column {
                        if (rows > 0) {
                            Text("RECENTI", fontSize = 12.sp, letterSpacing = 1.2.sp, color = colors.onSurfaceVariant)
                        }
                        recent.take(rows).forEach { dest ->
                            Surface(
                                onClick = { onDestination(dest) },
                                shape = RoundedCornerShape(14.dp),
                                color = colors.surface,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Refresh, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(dest, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

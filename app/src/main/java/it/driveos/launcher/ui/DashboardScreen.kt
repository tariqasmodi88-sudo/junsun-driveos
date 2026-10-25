package it.driveos.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.apps.AppEntry
import it.driveos.launcher.apps.AppKey
import it.driveos.launcher.settings.DriveSettings
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    settings: DriveSettings,
    apps: List<AppEntry>,
    onOpenNav: () -> Unit,
    onChooseNav: () -> Unit,
    onGoHome: () -> Unit,
    onSearchDestination: () -> Unit,
    onLaunch: (AppKey) -> Unit,
    onOpenApps: () -> Unit,
) {
    val navEntry = remember(apps, settings.navApp) { apps.firstOrNull { it.key == settings.navApp } }
    val favorites = remember(apps, settings.favorites) {
        settings.favorites.mapNotNull { raw ->
            AppKey.decode(raw)?.let { key -> apps.firstOrNull { it.key == key } }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        val wide = maxWidth > maxHeight
        val nav: @Composable (Modifier) -> Unit = { m ->
            NavPanel(navEntry, settings.navApp != null, settings.homeAddress, onOpenNav, onChooseNav, onGoHome, onSearchDestination, m)
        }
        if (wide) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ClockPanel(Modifier.weight(1f).fillMaxHeight())
                nav(Modifier.weight(1.1f).fillMaxHeight())
                FavoritesPanel(favorites, onLaunch, onOpenApps, Modifier.weight(1.2f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ClockPanel(Modifier.fillMaxWidth().weight(0.7f))
                nav(Modifier.fillMaxWidth().weight(1f))
                FavoritesPanel(favorites, onLaunch, onOpenApps, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun ClockPanel(modifier: Modifier) {
    // Si aggiorna allo scoccare di ogni minuto: nessun polling continuo.
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(((60 - value.second) * 1000L).coerceAtLeast(500L))
        }
    }
    val locale = Locale.getDefault()
    val time = remember(now) { now.format(DateTimeFormatter.ofPattern("HH:mm", locale)) }
    val date = remember(now) {
        now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    Panel(modifier) {
        Spacer(Modifier.weight(1f))
        Text(
            time,
            fontSize = 84.sp,
            lineHeight = 88.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(date, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun NavPanel(
    navEntry: AppEntry?,
    configured: Boolean,
    homeAddress: String,
    onOpenNav: () -> Unit,
    onChooseNav: () -> Unit,
    onGoHome: () -> Unit,
    onSearchDestination: () -> Unit,
    modifier: Modifier,
) {
    Panel(modifier, title = "Navigazione") {
        if (navEntry == null) {
            Text(
                if (configured) "L'app di navigazione scelta non è più installata."
                else "Nessuna app di navigazione scelta.",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            ActionButton("Scegli app di navigazione", Icons.Filled.Place, onChooseNav, highlighted = true)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(navEntry.icon, contentDescription = null, modifier = Modifier.size(44.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    navEntry.label,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.weight(1f))
            ActionButton("Apri ${navEntry.label}", Icons.Filled.PlayArrow, onOpenNav, highlighted = true)
            Spacer(Modifier.height(10.dp))
            ActionButton(
                if (homeAddress.isBlank()) "Imposta indirizzo di casa" else "Portami a casa",
                Icons.Filled.Home,
                onGoHome,
            )
            Spacer(Modifier.height(10.dp))
            ActionButton("Cerca destinazione", Icons.Filled.Search, onSearchDestination)
        }
    }
}

@Composable
private fun FavoritesPanel(
    favorites: List<AppEntry>,
    onLaunch: (AppKey) -> Unit,
    onOpenApps: () -> Unit,
    modifier: Modifier,
) {
    Panel(modifier, title = "Preferiti") {
        if (favorites.isEmpty()) {
            Text(
                "Nell'elenco App tieni premuta un'icona per aggiungerla qui.",
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            ActionButton("Apri elenco app", Icons.Filled.Menu, onOpenApps)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(96.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                items(favorites, key = { it.key.encode() }) { entry ->
                    AppTile(entry, isFavorite = false, onClick = { onLaunch(entry.key) }, onLongClick = null, iconSize = 56.dp)
                }
            }
        }
    }
}

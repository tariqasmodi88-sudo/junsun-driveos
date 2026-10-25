package it.driveos.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.apps.AppEntry
import it.driveos.launcher.apps.AppKey
import it.driveos.launcher.apps.AppSearch

@Composable
fun AppDrawerScreen(
    apps: List<AppEntry>,
    loaded: Boolean,
    favorites: List<String>,
    onLaunch: (AppKey) -> Unit,
    onToggleFavorite: (AppKey) -> Unit,
    onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(apps, query) { apps.filter { AppSearch.matches(it.label, query) } }
    val favoriteSet = remember(favorites) { favorites.toSet() }
    // Con la ricerca vuota i preferiti compaiono in testa, nell'ordine in cui li hai aggiunti.
    val favoriteEntries = remember(apps, favorites, query) {
        if (query.isNotBlank()) emptyList()
        else favorites.mapNotNull { k -> apps.firstOrNull { it.key.encode() == k } }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Applicazioni", onBack) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Cerca app") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Cancella") }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            )
        }
        Text(
            "Tocca per aprire · tieni premuto per aggiungere o togliere dai preferiti",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (!loaded) "Caricamento…" else "Nessuna app trovata.",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(112.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
            ) {
                if (favoriteEntries.isNotEmpty()) {
                    item(key = "h_fav", span = { GridItemSpan(maxLineSpan) }) { SectionLabel("Preferiti") }
                    items(favoriteEntries, key = { "f_" + it.key.encode() }) { entry ->
                        AppTile(
                            entry = entry,
                            isFavorite = true,
                            onClick = { onLaunch(entry.key) },
                            onLongClick = { onToggleFavorite(entry.key) },
                        )
                    }
                    item(key = "h_all", span = { GridItemSpan(maxLineSpan) }) { SectionLabel("Tutte le app") }
                }
                items(filtered, key = { it.key.encode() }) { entry ->
                    AppTile(
                        entry = entry,
                        isFavorite = entry.key.encode() in favoriteSet,
                        onClick = { onLaunch(entry.key) },
                        onLongClick = { onToggleFavorite(entry.key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
    )
}

package it.driveos.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.apps.AppEntry
import it.driveos.launcher.apps.AppKey
import it.driveos.launcher.apps.AppSearch

@Composable
fun AppDrawerScreen(
    apps: List<AppEntry>,
    loaded: Boolean,
    favorites: Set<String>,
    onLaunch: (AppKey) -> Unit,
    onToggleFavorite: (AppKey) -> Unit,
    onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(apps, query) { apps.filter { AppSearch.matches(it.label, query) } }

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
                items(filtered, key = { it.key.encode() }) { entry ->
                    AppTile(
                        entry = entry,
                        isFavorite = entry.key.encode() in favorites,
                        onClick = { onLaunch(entry.key) },
                        onLongClick = { onToggleFavorite(entry.key) },
                    )
                }
            }
        }
    }
}

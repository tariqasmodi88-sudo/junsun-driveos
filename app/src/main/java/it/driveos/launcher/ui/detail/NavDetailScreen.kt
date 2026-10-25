package it.driveos.launcher.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.apps.AppEntry
import it.driveos.launcher.nav.NavGuidance
import it.driveos.launcher.ui.ScreenHeader
import it.driveos.launcher.ui.cards.CardButton
import it.driveos.launcher.ui.icons.MicIcon

@Composable
fun NavDetailScreen(
    navEntry: AppEntry?,
    guidance: NavGuidance?,
    homeAddress: String,
    workAddress: String,
    recent: List<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    voiceAvailable: Boolean,
    onVoice: () -> Unit,
    onNavigate: (String) -> Unit,
    onHome: () -> Unit,
    onWork: () -> Unit,
    onEditHome: () -> Unit,
    onEditWork: () -> Unit,
    onRemoveRecent: (String) -> Unit,
    onOpenApp: () -> Unit,
    onChooseApp: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Navigazione", onBack) {
            Surface(onClick = onChooseApp, shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (navEntry != null) {
                        Image(navEntry.icon, null, Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        navEntry?.let { "${it.label} · Cambia" } ?: "Scegli app di navigazione",
                        fontSize = 17.sp,
                        color = colors.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1.15f).fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 22.sp, color = colors.onSurface),
                        placeholder = { Text("Indirizzo o luogo", fontSize = 20.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(28.dp)) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Filled.Clear, "Cancella") }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { if (query.isNotBlank()) onNavigate(query) }),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f),
                    )
                    if (voiceAvailable) CardButton(null, MicIcon, onVoice, Modifier.width(76.dp), height = 64.dp)
                    CardButton("Vai", Icons.Filled.PlayArrow, { if (query.isNotBlank()) onNavigate(query) }, height = 64.dp, primary = true)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlaceTile("Casa", homeAddress, Icons.Filled.Home, onHome, onEditHome, Modifier.weight(1f))
                    PlaceTile("Lavoro", workAddress, Icons.Filled.Star, onWork, onEditWork, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                if (guidance != null) {
                    Surface(shape = RoundedCornerShape(20.dp), color = colors.primary.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("GUIDA IN CORSO", fontSize = 12.sp, letterSpacing = 1.2.sp, color = colors.primary)
                            Text(guidance.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(guidance.text, fontSize = 19.sp, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (guidance.subText.isNotBlank()) Text(guidance.subText, fontSize = 17.sp, color = colors.primary, maxLines = 1)
                            Spacer(Modifier.weight(1f))
                            CardButton("Torna alla guida", Icons.Filled.PlayArrow, onOpenApp, Modifier.fillMaxWidth(), height = 64.dp, primary = true)
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                    if (navEntry != null) {
                        CardButton("Apri ${navEntry.label}", Icons.Filled.PlayArrow, onOpenApp, Modifier.fillMaxWidth(), height = 64.dp)
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxSize()) {
                Text("RECENTI", fontSize = 13.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                if (recent.isEmpty()) {
                    Text("Le destinazioni cercate compariranno qui.", fontSize = 18.sp, color = colors.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recent, key = { it }) { dest ->
                            Surface(
                                onClick = { onNavigate(dest) },
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surface,
                                modifier = Modifier.fillMaxWidth().height(68.dp),
                            ) {
                                Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Refresh, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(14.dp))
                                    Text(dest, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurface, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onRemoveRecent(dest) }, modifier = Modifier.size(64.dp)) {
                                        Icon(Icons.Filled.Clear, "Rimuovi", tint = colors.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceTile(
    title: String,
    address: String,
    icon: ImageVector,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = colors.surfaceVariant, modifier = modifier.height(88.dp)) {
        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = colors.primary, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                Text(
                    address.ifBlank { "Tocca per impostare" },
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onSurfaceVariant,
                )
            }
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = onEdit, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Filled.Edit, "Modifica $title", tint = colors.onSurfaceVariant)
                }
            }
        }
    }
}

package it.driveos.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.driveos.launcher.Screen
import it.driveos.launcher.apps.AppEntry
import it.driveos.launcher.apps.AppKey
import it.driveos.launcher.apps.AppSearch

/** Pulsante grande della barra inferiore: area di tocco ampia, icona + etichetta. */
@Composable
fun BigButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) colors.primary else colors.surfaceVariant,
        contentColor = if (selected) colors.onPrimary else colors.onSurface,
    ) {
        Column(
            Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun BottomBar(
    current: Screen,
    onHome: () -> Unit,
    onVehicle: () -> Unit,
    onNav: () -> Unit,
    onRadio: () -> Unit,
    onMusic: () -> Unit,
    onPhone: () -> Unit,
    onApps: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(92.dp).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Home e Veicolo restano fermi; gli altri scorrono se non c'è spazio.
        val fixed = Modifier.width(116.dp).fillMaxHeight()
        BigButton("Home", Icons.Filled.Home, onHome, fixed, selected = current == Screen.DASHBOARD)
        BigButton("Veicolo", it.driveos.launcher.ui.icons.CarIcon, onVehicle, fixed, selected = current == Screen.OVERVIEW)
        Row(
            Modifier.weight(1f).fillMaxHeight().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val m = Modifier.width(128.dp).fillMaxHeight()
            BigButton("Naviga", Icons.Filled.Place, onNav, m)
            BigButton("Radio", it.driveos.launcher.ui.icons.RadioIcon, onRadio, m, selected = current == Screen.RADIO)
            BigButton("Musica", Icons.Filled.PlayArrow, onMusic, m, selected = current == Screen.MUSIC)
            BigButton("Telefono", Icons.Filled.Phone, onPhone, m)
            BigButton("App", Icons.Filled.Menu, onApps, m, selected = current == Screen.APPS)
        }
    }
}

/** Riquadro della dashboard. */
@Composable
fun Panel(modifier: Modifier = Modifier, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            if (title != null) {
                Text(
                    title.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
fun ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (highlighted) colors.primary else colors.surfaceVariant,
        contentColor = if (highlighted) colors.onPrimary else colors.onSurface,
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.width(24.dp))
        trailing()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    entry: AppEntry,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    iconSize: Dp = 64.dp,
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Image(bitmap = entry.icon, contentDescription = null, modifier = Modifier.size(iconSize))
            if (isFavorite) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Preferito",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp).align(Alignment.TopEnd),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            entry.label,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Finestra di dialogo di base (evita le restrizioni di misura di AlertDialog con le liste). */
@Composable
fun DriveDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(0.9f),
        ) {
            Column(Modifier.padding(24.dp), content = content)
        }
    }
}

@Composable
fun AppPickerDialog(
    title: String,
    apps: List<AppEntry>,
    selected: AppKey?,
    onPick: (AppEntry?) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) { apps.filter { AppSearch.matches(it.label, query) } }
    DriveDialog(onDismiss) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Cerca") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp).weight(1f, fill = false)) {
            item { PickerRow(null, "Nessuna (non configurata)", selected == null) { onPick(null) } }
            items(filtered, key = { it.key.encode() }) { entry ->
                PickerRow(entry.icon, entry.label, entry.key == selected) { onPick(entry) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Chiudi", fontSize = 17.sp) }
        }
    }
}

@Composable
private fun PickerRow(icon: ImageBitmap?, label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(40.dp))
        else Spacer(Modifier.size(40.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun RadioPresetDialog(
    onConfirm: (frequency: String, name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var freq by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    DriveDialog(onDismiss) {
        Text("Aggiungi canale", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = freq,
            onValueChange = { freq = it },
            singleLine = true,
            label = { Text("Frequenza FM (es. 101.5)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            label = { Text("Nome (es. Radio Deejay)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (freq.isNotBlank()) onConfirm(freq, name) }),
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Annulla", fontSize = 17.sp) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onConfirm(freq, name) }, enabled = freq.isNotBlank()) {
                Text("Salva", fontSize = 17.sp)
            }
        }
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    placeholder: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    DriveDialog(onDismiss) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (value.isNotBlank()) onConfirm(value.trim()) }),
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Annulla", fontSize = 17.sp) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onConfirm(value.trim()) }, enabled = value.isNotBlank()) {
                Text(confirmLabel, fontSize = 17.sp)
            }
        }
    }
}

package it.driveos.launcher.ui.detail

import it.driveos.launcher.i18n.tr
import it.driveos.launcher.i18n.trf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.media.AudioFolder
import it.driveos.launcher.media.AudioFolders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Elenco delle cartelle con musica: si tocca per riprodurla (la voce in alto riproduce tutto). */
@Composable
fun FolderPickerDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val folders by produceState<List<AudioFolder>?>(null) { value = withContext(Dispatchers.IO) { AudioFolders.folders(ctx) } }
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Scegli la cartella"), fontSize = 24.sp) },
        text = {
            val list = folders
            when {
                list == null -> Text(tr("Cerco la musica…"), fontSize = 18.sp)
                list.isEmpty() -> Text(tr("Nessun brano trovato sul dispositivo."), fontSize = 18.sp)
                else -> LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    item {
                        Text(tr("Tutti i brani"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colors.primary,
                            modifier = Modifier.fillMaxWidth().clickable { onPick("") }.padding(vertical = 14.dp))
                    }
                    items(list, key = { it.path }) { f ->
                        Column(Modifier.fillMaxWidth().clickable { onPick(f.path) }.padding(vertical = 10.dp)) {
                            Text("${f.name}  (${f.count})", fontSize = 21.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(f.path, fontSize = 14.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Chiudi"), fontSize = 18.sp) } },
    )
}

package it.driveos.launcher.ui.detail

import it.driveos.launcher.i18n.tr
import it.driveos.launcher.i18n.trf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val AUDIO = listOf(".mp3", ".flac", ".m4a", ".aac", ".ogg", ".opus", ".wav", ".wma", ".mka")
fun isAudioFile(f: File) = f.isFile && AUDIO.any { f.name.lowercase().endsWith(it) }

private class Listing(val dirs: List<File>, val files: List<File>, val error: String?)

private fun roots(): List<Pair<String, File>> {
    val out = mutableListOf(tr("Memoria interna") to File("/storage/emulated/0"))
    File("/storage").listFiles()?.filter { it.isDirectory && it.name != "emulated" && it.name != "self" }
        ?.forEach { out.add("USB / SD (${it.name})" to it) }
    return out
}

/** Navigatore di cartelle dentro DriveOS: tocchi un brano e parte da li' con il resto della cartella. */
@Composable
fun FileBrowserDialog(
    start: String?,
    onPlay: (files: List<File>, index: Int, folder: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var dir by remember { mutableStateOf(start?.let { File(it) }?.takeIf { it.isDirectory }) }
    val listing by produceState<Listing?>(null, dir) {
        value = null
        val d = dir
        value = withContext(Dispatchers.IO) {
            if (d == null) Listing(emptyList(), emptyList(), null)
            else {
                val kids = d.listFiles()
                if (kids == null) Listing(emptyList(), emptyList(), tr("Non riesco a leggere questa cartella (permesso file mancante?)."))
                else Listing(
                    kids.filter { it.isDirectory && !it.name.startsWith(".") }.sortedBy { it.name.lowercase() },
                    kids.filter { isAudioFile(it) }.sortedBy { it.name.lowercase() },
                    null,
                )
            }
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().padding(24.dp), shape = RoundedCornerShape(24.dp), color = colors.surface) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dir != null) {
                        Surface(onClick = {
                            val d = dir!!
                            dir = if (roots().any { it.second.path == d.path }) null else d.parentFile
                        }, shape = RoundedCornerShape(14.dp), color = colors.surfaceVariant) {
                            Icon(Icons.Filled.ArrowBack, tr("Su"), Modifier.padding(12.dp).size(30.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(dir?.name ?: tr("Scegli la musica"), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (dir != null) Text(dir!!.path, fontSize = 14.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    val l = listing
                    if (dir != null && l != null && l.files.isNotEmpty()) {
                        Surface(onClick = { onPlay(l.files, 0, dir!!.path) }, shape = RoundedCornerShape(14.dp), color = colors.primary, contentColor = colors.onPrimary) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PlayArrow, null, Modifier.size(26.dp)); Spacer(Modifier.width(6.dp))
                                Text(tr("Riproduci tutto"), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Surface(onClick = onDismiss, shape = RoundedCornerShape(14.dp), color = colors.surfaceVariant) {
                        Icon(Icons.Filled.Close, tr("Chiudi"), Modifier.padding(12.dp).size(30.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                val l = listing
                when {
                    dir == null -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(roots(), key = { it.second.path }) { (label, f) ->
                            Entry(if (f.path.contains("emulated")) Icons.Filled.Folder else Icons.Filled.Usb, label, f.path) { dir = f }
                        }
                    }
                    l == null -> Text(tr("Apro la cartella…"), fontSize = 20.sp)
                    l.error != null -> Text(l.error, fontSize = 20.sp, color = colors.error)
                    l.dirs.isEmpty() && l.files.isEmpty() -> Text(tr("Cartella vuota (nessun brano)."), fontSize = 20.sp)
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(l.dirs, key = { "d" + it.path }) { d -> Entry(Icons.Filled.Folder, d.name, null) { dir = d } }
                        items(l.files.size, key = { "f" + l.files[it].path }) { i ->
                            val f = l.files[i]
                            Entry(Icons.Filled.MusicNote, f.name.substringBeforeLast('.'), null) { onPlay(l.files, i, dir!!.path) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Entry(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(30.dp), tint = colors.primary)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (sub != null) Text(sub, fontSize = 13.sp, color = colors.onSurfaceVariant)
            }
        }
    }
}

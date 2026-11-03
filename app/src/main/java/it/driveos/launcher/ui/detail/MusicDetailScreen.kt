package it.driveos.launcher.ui.detail

import it.driveos.launcher.i18n.tr
import it.driveos.launcher.i18n.trf

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.apps.AppEntry
import it.driveos.launcher.apps.AppKey
import it.driveos.launcher.media.MediaInfo
import it.driveos.launcher.media.PlaybackMath
import it.driveos.launcher.ui.ScreenHeader
import it.driveos.launcher.ui.cards.AlbumArt
import it.driveos.launcher.ui.cards.CardButton
import it.driveos.launcher.ui.cards.TransportControls
import kotlinx.coroutines.delay

@Composable
fun MusicDetailScreen(
    access: Boolean,
    media: MediaInfo?,
    sourceEntry: AppEntry?,
    musicApp: AppEntry?,
    sources: List<AppEntry>,
    onGrantAccess: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenSource: () -> Unit,
    onLaunch: (AppKey) -> Unit,
    onBack: () -> Unit,
    onSeek: (Long) -> Unit = {},
    shuffle: Boolean = false,
    repeat: Int = 0,
    onShuffle: () -> Unit = {},
    onRepeat: () -> Unit = {},
    onPickFolder: (() -> Unit)? = null,
    onPlayLast: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(tr("Musica"), onBack) {
            val app = sourceEntry ?: musicApp
            if (onPickFolder != null) CardButton(tr("Scegli musica"), Icons.Filled.Folder, onPickFolder, Modifier.widthIn(max = 320.dp), height = 56.dp)
            if (app != null && onPickFolder == null) CardButton(trf("Apri {0}", app.label), Icons.Filled.PlayArrow, onOpenSource, Modifier.widthIn(max = 320.dp), height = 56.dp)
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) {
            when {
                !access && media == null -> Column(Modifier.weight(1f)) {
                    Text(tr("Per mostrare brano, copertina e comandi serve l'accesso alle notifiche."), fontSize = 22.sp, color = colors.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tr("DriveOS lo usa solo per leggere il lettore attivo e le indicazioni del navigatore. Le altre notifiche vengono ignorate."),
                        fontSize = 17.sp, color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    CardButton(tr("Attiva accesso"), Icons.Filled.Lock, onGrantAccess, Modifier.widthIn(max = 360.dp).fillMaxWidth(), height = 72.dp, primary = true)
                }

                media == null || media.title.isBlank() -> Column(Modifier.weight(1f)) {
                    Text(tr("Nessuna riproduzione in corso."), fontSize = 24.sp, color = colors.onSurface)
                    Text(tr("Scegli una cartella di brani o avvia una sorgente."), fontSize = 18.sp, color = colors.onSurfaceVariant)
                    if (onPlayLast != null) {
                        Spacer(Modifier.height(12.dp))
                        CardButton(tr("Riproduci l'ultima cartella"), Icons.Filled.PlayArrow, onPlayLast, Modifier.widthIn(max = 420.dp).fillMaxWidth(), height = 64.dp, primary = true)
                    }
                }

                else -> BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val art = minOf(maxHeight, maxWidth * 0.5f)
                    val local = media.packageName == LocalContext.current.packageName
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        AlbumArt(media, if (local) null else sourceEntry, Modifier.size(art), corner = 28.dp)
                        Spacer(Modifier.width(28.dp))
                        Column(Modifier.weight(1f)) {
                            Text(media.title, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = colors.onSurface)
                            if (media.artist.isNotBlank()) Text(media.artist, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
                            if (media.album.isNotBlank()) Text(media.album, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
                            Spacer(Modifier.height(14.dp))
                            if (media.durationMs > 0) {
                                val pos by produceState(media.positionNow(), media) {
                                    while (true) {
                                        value = media.positionNow()
                                        if (!media.isPlaying) break
                                        delay(1000)
                                    }
                                }
                                var drag by remember(media.title) { mutableStateOf<Float?>(null) }
                                val shown = drag ?: PlaybackMath.progress(pos, media.durationMs).coerceIn(0f, 1f)
                                Slider(
                                    value = shown,
                                    onValueChange = { drag = it },
                                    onValueChangeFinished = {
                                        drag?.let { onSeek((it * media.durationMs).toLong()) }
                                        drag = null
                                    },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                )
                                Row(Modifier.fillMaxWidth()) {
                                    Text(PlaybackMath.format(drag?.let { (it * media.durationMs).toLong() } ?: pos), fontSize = 15.sp, color = colors.onSurfaceVariant)
                                    Spacer(Modifier.weight(1f))
                                    Text(PlaybackMath.format(media.durationMs), fontSize = 15.sp, color = colors.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(14.dp))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                RoundButton(Icons.Filled.Shuffle, tr("Casuale"), 60.dp, active = shuffle, onClick = onShuffle)
                                RoundButton(Icons.Filled.SkipPrevious, tr("Precedente"), 72.dp, onClick = onPrevious)
                                RoundButton(if (media.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, tr("Play"), 92.dp, primary = true, onClick = onPlayPause)
                                RoundButton(Icons.Filled.SkipNext, tr("Successivo"), 72.dp, onClick = onNext)
                                RoundButton(if (repeat == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat, tr("Ripeti"), 60.dp, active = repeat != 0, onClick = onRepeat)
                            }
                        }
                    }
                }
            }
            if (sources.isNotEmpty() && (media == null || media.title.isBlank())) {
                Spacer(Modifier.height(12.dp))
                Text("SORGENTI", fontSize = 13.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    items(sources, key = { it.key.encode() }) { app ->
                        Surface(
                            onClick = { onLaunch(app.key) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (app.key.packageName == media?.packageName) colors.primary.copy(alpha = 0.22f) else colors.surface,
                            modifier = Modifier.height(76.dp),
                        ) {
                            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(app.icon, null, Modifier.size(40.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(app.label, fontSize = 18.sp, maxLines = 1, color = colors.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, size: androidx.compose.ui.unit.Dp, active: Boolean = false, primary: Boolean = false, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        onClick = onClick, shape = CircleShape,
        color = when { primary -> c.primary; active -> c.primary.copy(alpha = 0.25f); else -> c.surfaceVariant },
        contentColor = when { primary -> c.onPrimary; active -> c.primary; else -> c.onSurface },
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, Modifier.size(size * 0.5f)) }
    }
}

@Composable
private fun ModeChip(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick, modifier = modifier.height(48.dp), shape = RoundedCornerShape(24.dp),
        color = if (active) colors.primary else colors.surfaceVariant,
        contentColor = if (active) colors.onPrimary else colors.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) { Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
    }
}

package it.driveos.launcher.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Musica", onBack) {
            val app = sourceEntry ?: musicApp
            if (app != null) CardButton("Apri ${app.label}", Icons.Filled.PlayArrow, onOpenSource, Modifier.widthIn(max = 320.dp), height = 56.dp)
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) {
            when {
                !access -> Column(Modifier.weight(1f)) {
                    Text("Per mostrare brano, copertina e comandi serve l'accesso alle notifiche.", fontSize = 22.sp, color = colors.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "DriveOS lo usa solo per leggere il lettore attivo e le indicazioni del navigatore. Le altre notifiche vengono ignorate.",
                        fontSize = 17.sp, color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    CardButton("Attiva accesso", Icons.Filled.Lock, onGrantAccess, Modifier.widthIn(max = 360.dp).fillMaxWidth(), height = 72.dp, primary = true)
                }

                media == null || media.title.isBlank() -> Column(Modifier.weight(1f)) {
                    Text("Nessuna riproduzione in corso.", fontSize = 24.sp, color = colors.onSurface)
                    Text("Avvia una sorgente qui sotto.", fontSize = 18.sp, color = colors.onSurfaceVariant)
                }

                else -> BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val art = minOf(maxHeight, maxWidth * 0.4f)
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        AlbumArt(media, sourceEntry, Modifier.size(art), corner = 24.dp)
                        Spacer(Modifier.width(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(media.title, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = colors.onSurface)
                            Text(media.artist, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
                            if (media.album.isNotBlank()) Text(media.album, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            if (media.durationMs > 0) {
                                val pos by produceState(media.positionNow(), media) {
                                    while (true) {
                                        value = media.positionNow()
                                        if (!media.isPlaying) break
                                        delay(1000)
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { PlaybackMath.progress(pos, media.durationMs) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                )
                                Row(Modifier.fillMaxWidth()) {
                                    Text(PlaybackMath.format(pos), fontSize = 15.sp, color = colors.onSurfaceVariant)
                                    Spacer(Modifier.weight(1f))
                                    Text(PlaybackMath.format(media.durationMs), fontSize = 15.sp, color = colors.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                            TransportControls(media.isPlaying, onPlayPause, onNext, onPrevious, height = 80.dp, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            if (sources.isNotEmpty()) {
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

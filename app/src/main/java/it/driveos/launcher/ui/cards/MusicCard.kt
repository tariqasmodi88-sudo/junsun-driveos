package it.driveos.launcher.ui.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.apps.AppEntry
import it.driveos.launcher.media.MediaInfo
import it.driveos.launcher.media.PlaybackMath
import it.driveos.launcher.ui.icons.PauseIcon
import kotlinx.coroutines.delay

/** Avanzamento del brano: si aggiorna una volta al secondo solo mentre suona. */
@Composable
fun rememberProgress(media: MediaInfo): Float {
    val p by produceState(PlaybackMath.progress(media.positionNow(), media.durationMs), media) {
        while (true) {
            value = PlaybackMath.progress(media.positionNow(), media.durationMs)
            if (!media.isPlaying) break
            delay(1000)
        }
    }
    return p
}

@Composable
fun MusicCard(
    access: Boolean,
    media: MediaInfo?,
    sourceEntry: AppEntry?,
    musicApp: AppEntry?,
    onOpenDetail: () -> Unit,
    onGrantAccess: () -> Unit,
    onOpenMusicApp: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DashCard("Musica", Icons.Filled.PlayArrow, onOpenDetail, modifier, trailing = (sourceEntry ?: musicApp)?.label) { size, wide ->
        when {
            !access -> Message(
                size, "Attiva l'accesso per vedere il brano in riproduzione.",
                "Attiva", Icons.Filled.Lock, onGrantAccess,
            )
            media == null || media.title.isBlank() -> Message(
                size, "Nessuna riproduzione in corso.",
                musicApp?.let { "Apri ${it.label}" } ?: "Scegli app", Icons.Filled.PlayArrow, onOpenMusicApp,
            )
            else -> Playing(media, sourceEntry, size, wide, onPlayPause, onNext, onPrevious)
        }
    }
}

@Composable
private fun ColumnScope.Message(size: CardSize, text: String, button: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    if (size != CardSize.COMPACT) {
        Text(text, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
    }
    CardButton(button, icon, onClick, Modifier.fillMaxWidth(), height = if (size == CardSize.COMPACT) 56.dp else 68.dp, primary = true)
}

@Composable
fun AlbumArt(media: MediaInfo, fallback: AppEntry?, modifier: Modifier, corner: Dp = 16.dp) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.clip(RoundedCornerShape(corner)), color = colors.surfaceVariant) {
        val art = media.art
        if (art != null) {
            Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(contentAlignment = Alignment.Center) {
                if (fallback != null) Image(fallback.icon, null, Modifier.fillMaxSize(0.55f))
                else Icon(Icons.Filled.PlayArrow, null, tint = colors.onSurfaceVariant, modifier = Modifier.fillMaxSize(0.5f))
            }
        }
    }
}

@Composable
fun TransportControls(
    playing: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
    showSkip: Boolean = true,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showSkip) CardButton(null, Icons.Filled.KeyboardArrowLeft, onPrevious, Modifier.weight(1f), height = height)
        CardButton(null, if (playing) PauseIcon else Icons.Filled.PlayArrow, onPlayPause, Modifier.weight(1.3f), height = height, primary = true)
        if (showSkip) CardButton(null, Icons.Filled.KeyboardArrowRight, onNext, Modifier.weight(1f), height = height)
    }
}

@Composable
private fun ColumnScope.Playing(
    media: MediaInfo,
    source: AppEntry?,
    size: CardSize,
    wide: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    when {
        size == CardSize.COMPACT -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(media, source, Modifier.fillMaxHeight().aspectRatio(1f), corner = 12.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(media.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurface)
                Text(media.artist, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            CardButton(null, if (media.isPlaying) PauseIcon else Icons.Filled.PlayArrow, onPlayPause, Modifier.width(64.dp), height = 56.dp, primary = true)
        }

        wide -> Row(Modifier.fillMaxSize()) {
            AlbumArt(media, source, Modifier.fillMaxHeight().aspectRatio(1f))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Text(media.title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurface)
                Text(media.artist, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                if (media.durationMs > 0) {
                    val progress = rememberProgress(media)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp))
                    Spacer(Modifier.height(8.dp))
                }
                TransportControls(media.isPlaying, onPlayPause, onNext, onPrevious, height = 56.dp, modifier = Modifier.fillMaxWidth())
            }
        }

        else -> BoxWithConstraints(Modifier.fillMaxSize()) {
            // Scheda alta: copertina in alto, testi e comandi sotto.
            val artSize = minOf(maxWidth, maxHeight - 150.dp).coerceAtLeast(0.dp)
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (artSize >= 72.dp) {
                    AlbumArt(media, source, Modifier.size(artSize))
                    Spacer(Modifier.height(10.dp))
                }
                Text(media.title, fontSize = if (size == CardSize.LARGE) 26.sp else 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurface)
                Text(media.artist, fontSize = if (size == CardSize.LARGE) 19.sp else 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                if (media.durationMs > 0) {
                    val progress = rememberProgress(media)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp))
                    Spacer(Modifier.height(8.dp))
                }
                TransportControls(media.isPlaying, onPlayPause, onNext, onPrevious, height = 64.dp, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

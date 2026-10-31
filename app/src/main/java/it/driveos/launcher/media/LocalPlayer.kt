package it.driveos.launcher.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.ui.graphics.asImageBitmap
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlin.concurrent.thread

/**
 * Lettore musicale interno a DriveOS (ExoPlayer): suona i file di una cartella scelta dall'utente.
 * Casuale, ripeti, barra di avanzamento e copertina (immagine incorporata nel file) funzionano sempre,
 * senza dipendere da un'altra app.
 */
class LocalPlayer(context: Context, private val onChange: (MediaInfo?) -> Unit) {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val prefs = app.getSharedPreferences("local_player", Context.MODE_PRIVATE)

    private class Meta(val title: String, val artist: String, val album: String, val durationMs: Long, val art: Bitmap?)

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(app)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build().also { p ->
                p.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) { publish() }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { loadMeta(mediaItem) }
                })
            }
    }
    private val cache = HashMap<String, Meta>()
    @Volatile private var loading: String? = null

    var hasItems = false
        private set
    val lastFolder: String? get() = prefs.getString("folder", null)
    val isPlaying: Boolean get() = hasItems && player.isPlaying
    val shuffle: Boolean get() = hasItems && player.shuffleModeEnabled
    /** 0 = no, 1 = tutto, 2 = singolo */
    val repeat: Int get() = if (!hasItems) 0 else when (player.repeatMode) { Player.REPEAT_MODE_ALL -> 1; Player.REPEAT_MODE_ONE -> 2; else -> 0 }

    fun playLastFolder(): Boolean {
        val u = lastFolder ?: return false
        playFolder(Uri.parse(u), remember = false)
        return true
    }

    /** Legge la cartella (con sottocartelle) e avvia il primo brano. */
    fun playFolder(tree: Uri, remember: Boolean = true) {
        if (remember) {
            runCatching { app.contentResolver.takePersistableUriPermission(tree, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            prefs.edit().putString("folder", tree.toString()).apply()
        }
        thread(name = "local-scan") {
            val files = ArrayList<DocumentFile>()
            fun walk(d: DocumentFile, depth: Int) {
                if (depth > 6) return
                d.listFiles().sortedBy { it.name.orEmpty().lowercase() }.forEach { f ->
                    if (f.isDirectory) walk(f, depth + 1)
                    else if (isAudio(f)) files.add(f)
                }
            }
            runCatching { DocumentFile.fromTreeUri(app, tree)?.let { walk(it, 0) } }
            main.post {
                if (files.isEmpty()) { onChange(null); return@post }
                val items = files.map { MediaItem.Builder().setUri(it.uri).setMediaId(it.uri.toString()).build() }
                hasItems = true
                player.setMediaItems(items)
                player.prepare()
                player.play()
                loadMeta(player.currentMediaItem)
                publish()
            }
        }
    }

    private fun isAudio(f: DocumentFile): Boolean {
        if (f.type?.startsWith("audio/") == true) return true
        val n = f.name.orEmpty().lowercase()
        return listOf(".mp3", ".flac", ".m4a", ".aac", ".ogg", ".opus", ".wav", ".wma", ".mka").any { n.endsWith(it) }
    }

    private fun loadMeta(item: MediaItem?) {
        val id = item?.mediaId ?: return
        if (cache.containsKey(id) || loading == id) return
        loading = id
        thread(name = "local-meta") {
            val r = MediaMetadataRetriever()
            val meta = runCatching {
                r.setDataSource(app, Uri.parse(id))
                val art = r.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                Meta(
                    title = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty(),
                    artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty(),
                    album = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty(),
                    durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                    art = art?.let { downscale(it) },
                )
            }.getOrNull() ?: Meta("", "", "", 0L, null)
            runCatching { r.release() }
            main.post { cache[id] = meta; if (loading == id) loading = null; publish() }
        }
    }

    private fun downscale(b: Bitmap): Bitmap {
        val max = 600
        val ratio = minOf(1f, max.toFloat() / maxOf(b.width, b.height))
        if (ratio >= 1f) return b
        return Bitmap.createScaledBitmap(b, (b.width * ratio).toInt(), (b.height * ratio).toInt(), true)
    }

    private fun publish() {
        if (!hasItems) return
        val item = player.currentMediaItem
        val meta = item?.mediaId?.let { cache[it] }
        val name = item?.mediaId?.let { Uri.parse(it).lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') }.orEmpty()
        val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: meta?.durationMs ?: 0L
        onChange(
            MediaInfo(
                packageName = app.packageName,
                title = meta?.title?.takeIf { it.isNotBlank() } ?: name,
                artist = meta?.artist.orEmpty(),
                album = meta?.album.orEmpty(),
                art = meta?.art?.asImageBitmap(),
                isPlaying = player.isPlaying,
                durationMs = dur,
                positionMs = player.currentPosition,
                positionUpdateTime = SystemClock.elapsedRealtime(),
                speed = 1f,
            ),
        )
    }

    fun playPause() { if (!hasItems) return; if (player.isPlaying) player.pause() else player.play() }
    fun pause() { if (hasItems) player.pause() }
    fun play() { if (hasItems) player.play() }
    fun next() { if (hasItems) { if (player.hasNextMediaItem()) player.seekToNextMediaItem() else player.seekTo(0, 0L) } }
    fun previous() { if (hasItems) { if (player.currentPosition > 3000 || !player.hasPreviousMediaItem()) player.seekTo(0) else player.seekToPreviousMediaItem() } }
    fun seekTo(ms: Long) { if (hasItems) { player.seekTo(ms); publish() } }
    fun toggleShuffle() { if (hasItems) player.shuffleModeEnabled = !player.shuffleModeEnabled }
    fun cycleRepeat() {
        if (!hasItems) return
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}

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

    /** Cartella ("" = tutti i brani) dalla raccolta Audio; la ricorda per la volta dopo. */
    fun playFolder(folder: String) {
        prefs.edit().putString("folder", folder).apply()
        thread(name = "local-scan") {
            val uris = if (folder.isEmpty()) AudioFolders.all(app) else AudioFolders.tracks(app, folder)
            main.post {
                if (uris.isEmpty()) { onChange(null); return@post }
                val items = uris.map { MediaItem.Builder().setUri(it).setMediaId(it.toString()).build() }
                hasItems = true
                player.setMediaItems(items)
                player.prepare()
                player.play()
                loadMeta(player.currentMediaItem)
                publish()
            }
        }
    }

    /** Brani scelti dal navigatore file: parte da [start] e prosegue con gli altri della cartella. */
    fun playFiles(files: List<java.io.File>, start: Int, folder: String) {
        if (files.isEmpty()) return
        prefs.edit().putString("folder", folder).apply()
        val items = files.map { val u = Uri.fromFile(it); MediaItem.Builder().setUri(u).setMediaId(u.toString()).build() }
        hasItems = true
        player.setMediaItems(items, start.coerceIn(0, items.lastIndex), 0L)
        player.prepare()
        player.play()
        loadMeta(player.currentMediaItem)
        publish()
    }

    fun playLastFolder(): Boolean { val f = lastFolder ?: return false; playFolder(f); return true }

    private fun loadMeta(item: MediaItem?) {
        val id = item?.mediaId ?: return
        if (cache.containsKey(id) || loading == id) return
        loading = id
        thread(name = "local-meta") {
            val r = MediaMetadataRetriever()
            val meta = runCatching {
                r.setDataSource(app, Uri.parse(id))
                var art = r.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                if (art == null) {
                    // niente immagine nel file: cerca cover/folder.jpg nella stessa cartella
                    Uri.parse(id).path?.takeIf { Uri.parse(id).scheme == "file" }?.let { path ->
                        val dir = java.io.File(path).parentFile
                        val names = listOf("cover", "folder", "front", "album", "albumart")
                        val img = dir?.listFiles()?.firstOrNull { f -> names.any { n -> f.name.lowercase().let { x -> x.startsWith(n) && (x.endsWith(".jpg") || x.endsWith(".jpeg") || x.endsWith(".png")) } } }
                        if (img != null) art = BitmapFactory.decodeFile(img.path)
                    }
                }
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

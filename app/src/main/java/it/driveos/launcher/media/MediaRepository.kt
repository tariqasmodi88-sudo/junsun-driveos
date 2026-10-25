package it.driveos.launcher.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import it.driveos.launcher.notifications.DriveNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MediaInfo(
    val packageName: String,
    val title: String,
    val artist: String,
    val album: String,
    val art: ImageBitmap?,
    val isPlaying: Boolean,
    val durationMs: Long,
    val positionMs: Long,
    val positionUpdateTime: Long,
    val speed: Float,
) {
    fun positionNow(now: Long = SystemClock.elapsedRealtime()): Long =
        PlaybackMath.currentPosition(positionMs, positionUpdateTime, now, speed, isPlaying, durationMs)
}

/**
 * Brano in riproduzione tramite MediaSessionManager (API pubblica).
 * Richiede l'accesso alle notifiche concesso dall'utente; senza, [access] resta false
 * e la scheda Musica lo dice chiaramente.
 */
class MediaRepository(context: Context) {
    private val app = context.applicationContext
    private val manager = app.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(app, DriveNotificationListener::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val _access = MutableStateFlow(false)
    val access: StateFlow<Boolean> = _access.asStateFlow()

    private val _media = MutableStateFlow<MediaInfo?>(null)
    val media: StateFlow<MediaInfo?> = _media.asStateFlow()

    private var controller: MediaController? = null
    private var listening = false
    private var lastArtSource: Bitmap? = null
    private var lastArt: ImageBitmap? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        onSessions(list.orEmpty())
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            // Se un'altra sessione ha iniziato a suonare, passiamo a quella.
            refreshSessions()
        }
        override fun onSessionDestroyed() = refreshSessions()
    }

    fun hasAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(app).contains(app.packageName)

    /** Da chiamare all'avvio e al ritorno nel launcher: e' idempotente. */
    fun start() = handler.post {
        val ok = hasAccess()
        _access.value = ok
        if (!ok || manager == null) {
            stopInternal()
            return@post
        }
        if (!listening) {
            try {
                manager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, handler)
                listening = true
            } catch (e: SecurityException) {
                _access.value = false
                return@post
            }
        }
        refreshSessions()
    }

    private fun stopInternal() {
        if (listening) runCatching { manager?.removeOnActiveSessionsChangedListener(sessionsListener) }
        listening = false
        attach(null)
    }

    private fun refreshSessions() {
        val list = try {
            manager?.getActiveSessions(listenerComponent).orEmpty()
        } catch (e: SecurityException) {
            _access.value = false
            emptyList()
        }
        onSessions(list)
    }

    private fun onSessions(list: List<MediaController>) {
        val best = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list.firstOrNull { it.sessionToken == controller?.sessionToken }
            ?: list.firstOrNull()
        if (best?.sessionToken != controller?.sessionToken) attach(best) else publish()
    }

    private fun attach(c: MediaController?) {
        controller?.let { runCatching { it.unregisterCallback(callback) } }
        controller = c
        c?.registerCallback(callback, handler)
        publish()
    }

    private fun publish() {
        val c = controller
        if (c == null) {
            _media.value = null
            return
        }
        val md = c.metadata
        val st = c.playbackState
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val album = md?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val artSource = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (artSource !== lastArtSource) {
            lastArtSource = artSource
            lastArt = artSource?.let { runCatching { downscale(it).asImageBitmap() }.getOrNull() }
        }
        _media.value = MediaInfo(
            packageName = c.packageName,
            title = title,
            artist = artist,
            album = album,
            art = lastArt,
            isPlaying = st?.state == PlaybackState.STATE_PLAYING,
            durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            positionMs = st?.position ?: 0L,
            positionUpdateTime = st?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = st?.playbackSpeed ?: 1f,
        )
    }

    private fun downscale(b: Bitmap): Bitmap {
        val max = 512
        if (b.width <= max && b.height <= max) return b
        val ratio = max.toFloat() / maxOf(b.width, b.height)
        return Bitmap.createScaledBitmap(b, (b.width * ratio).toInt(), (b.height * ratio).toInt(), true)
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }
}

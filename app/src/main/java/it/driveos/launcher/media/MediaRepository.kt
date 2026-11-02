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

    // --- Lettore interno (cartella di file) ---
    private val local: LocalPlayer by lazy {
        lateinit var lp: LocalPlayer
        lp = LocalPlayer(app) { info ->
            _media.value = info
            if (info != null) { _shuffle.value = lp.shuffle; _repeat.value = lp.repeat }
        }
        lp
    }
    @Volatile private var localActive = false

    /** Chiamato prima di far partire la musica: spegne la radio (impostato da DriveOSApp). */
    @Volatile var beforePlay: (() -> Unit)? = null
    private fun willPlay() { runCatching { beforePlay?.invoke() } }

    fun playFolder(folder: String) = handler.post {
        willPlay()
        runCatching { controller?.transportControls?.pause() }
        localActive = true
        local.playFolder(folder)
    }
    fun playFiles(files: List<java.io.File>, start: Int, folder: String) = handler.post {
        willPlay()
        runCatching { controller?.transportControls?.pause() }
        localActive = true
        local.playFiles(files, start, folder)
    }
    fun isLocal(): Boolean = localActive
    fun lastFolder(): String? = local.lastFolder
    fun playLastFolder() = handler.post {
        willPlay()
        runCatching { controller?.transportControls?.pause() }
        localActive = true
        local.playLastFolder()
    }

    private fun publish() {
        val c = controller
        if (localActive) {
            if (c?.playbackState?.state == PlaybackState.STATE_PLAYING) { local.pause(); localActive = false }
            else return
        }
        if (c == null) {
            _media.value = null
            return
        }
        val md = c.metadata
        val st = c.playbackState
        val title = firstText(
            md?.getString(MediaMetadata.METADATA_KEY_TITLE),
            md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
        )
        val artist = firstText(
            md?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),
            md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
        )
        val album = firstText(md?.getString(MediaMetadata.METADATA_KEY_ALBUM))
        val artSource: Bitmap? = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART,
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON,
        ).firstNotNullOfOrNull { key -> md?.getBitmap(key) }
        if (artSource !== lastArtSource) {
            lastArtSource = artSource
            lastArt = artSource?.let { runCatching { downscale(it).asImageBitmap() }.getOrNull() }
        }
        var art = lastArt
        if (art == null) {
            // niente copertina dal lettore: la ricaviamo dal file stesso (immagine incorporata nel flac/mp3…)
            val keyUri = listOf(
                MediaMetadata.METADATA_KEY_MEDIA_URI, MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
                MediaMetadata.METADATA_KEY_ART_URI, MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
            ).firstNotNullOfOrNull { k -> md?.getString(k)?.takeIf { it.isNotBlank() } }
            val key = keyUri ?: title.takeIf { it.isNotBlank() }
            if (key != null) {
                if (key == fileArtKey) art = fileArt
                else if (key != fileArtLoading) loadFileArt(key, keyUri != null, title)
            }
        }
        _media.value = MediaInfo(
            packageName = c.packageName,
            title = title,
            artist = artist,
            album = album,
            art = art,
            isPlaying = st?.state == PlaybackState.STATE_PLAYING,
            durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            positionMs = st?.position ?: 0L,
            positionUpdateTime = st?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = st?.playbackSpeed ?: 1f,
        )
    }

    private var fileArtKey: String? = null
    private var fileArt: ImageBitmap? = null
    @Volatile private var fileArtLoading: String? = null

    private fun loadFileArt(key: String, isUri: Boolean, title: String) {
        fileArtLoading = key
        Thread {
            val bmp: Bitmap? = runCatching {
                if (isUri) fromUri(android.net.Uri.parse(key)) else fromMediaStore(title)
            }.getOrNull() ?: if (isUri) runCatching { fromMediaStore(title) }.getOrNull() else null
            handler.post {
                fileArtLoading = null
                fileArtKey = key
                fileArt = bmp?.let { runCatching { downscale(it).asImageBitmap() }.getOrNull() }
                publish()
            }
        }.start()
    }

    private fun fromUri(uri: android.net.Uri): Bitmap? {
        // 1) immagine incorporata nel file audio
        runCatching {
            val r = android.media.MediaMetadataRetriever()
            try {
                r.setDataSource(app, uri)
                r.embeddedPicture?.let { return android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
            } finally { runCatching { r.release() } }
        }
        // 2) l'indirizzo e' direttamente un'immagine
        return runCatching { app.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) } }.getOrNull()
    }

    /** Cerca il file per nome in MediaStore (serve il permesso audio; se manca, niente copertina). */
    private fun fromMediaStore(title: String): Bitmap? {
        val name = title.trim()
        if (name.isEmpty()) return null
        val coll = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        app.contentResolver.query(
            coll, arrayOf(android.provider.MediaStore.Audio.Media._ID),
            "${android.provider.MediaStore.Audio.Media.DISPLAY_NAME} = ? OR ${android.provider.MediaStore.Audio.Media.TITLE} = ?",
            arrayOf(name, name), null,
        )?.use { c ->
            if (c.moveToFirst()) return fromUri(android.content.ContentUris.withAppendedId(coll, c.getLong(0)))
        }
        return null
    }

    private fun downscale(b: Bitmap): Bitmap {
        val max = 512
        if (b.width <= max && b.height <= max) return b
        val ratio = max.toFloat() / maxOf(b.width, b.height)
        return Bitmap.createScaledBitmap(b, (b.width * ratio).toInt(), (b.height * ratio).toInt(), true)
    }

    private fun firstText(vararg values: String?): String =
        values.firstOrNull { !it.isNullOrBlank() }.orEmpty()

    /** Porta il brano al punto indicato (se il lettore lo permette). */
    fun seekTo(ms: Long) {
        if (localActive) { local.seekTo(ms); return }
        runCatching { controller?.transportControls?.seekTo(ms.coerceAtLeast(0)) }
    }

    /** Mette in pausa se sta suonando (per lasciare l'audio alla radio). */
    @Volatile private var pausedForRadio = false

    fun pauseIfPlaying() {
        if (localActive) { if (local.isPlaying) { local.pause(); pausedForRadio = true }; return }
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) { runCatching { c.transportControls.pause() }; pausedForRadio = true }
    }

    /** Se la musica era stata fermata dalla Radio, la riprende dal punto in cui era. */
    fun resumeIfPausedForRadio(): Boolean {
        if (!pausedForRadio) return false
        pausedForRadio = false
        willPlay()
        if (localActive) { local.play(); return true }
        runCatching { controller?.transportControls?.play() }
        return true
    }

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()
    /** 0 = no, 1 = tutto, 2 = singolo */
    private val _repeat = MutableStateFlow(0)
    val repeat: StateFlow<Int> = _repeat.asStateFlow()

    private fun customAction(vararg words: String): PlaybackState.CustomAction? =
        controller?.playbackState?.customActions.orEmpty().firstOrNull { a ->
            words.any { w -> a.action.contains(w, true) || a.name.toString().contains(w, true) }
        }

    /** Casuale on/off. false se il lettore non espone questo comando. */
    fun toggleShuffle(): Boolean {
        if (localActive) { local.toggleShuffle(); return true }
        val a = customAction("shuffle", "random", "casual") ?: return false
        runCatching { controller?.transportControls?.sendCustomAction(a, null) }
        _shuffle.value = !_shuffle.value
        return true
    }

    /** Ripeti: no → tutto → singolo. false se il lettore non espone il comando. */
    fun cycleRepeat(): Boolean {
        if (localActive) { local.cycleRepeat(); return true }
        val a = customAction("repeat", "loop", "ripet") ?: return false
        runCatching { controller?.transportControls?.sendCustomAction(a, null) }
        _repeat.value = (_repeat.value + 1) % 3
        return true
    }

    /** Passando a Musica: se il lettore interno ha una coda ferma, riparte. */
    fun resumeLocalIfIdle() = handler.post {
        if (localActive && !local.isPlaying) { willPlay(); local.play() }
        // dopo un riavvio il lettore interno e' vuoto: riprende l'ultima cartella dal punto in cui era,
        // a meno che stia gia' suonando un'altra app
        else if (!localActive && local.lastFolder != null && controller?.playbackState?.state != PlaybackState.STATE_PLAYING) {
            willPlay(); localActive = true; local.playLastFolder()
        }
    }

    fun playPause() {
        val playingNow = if (localActive) local.isPlaying else controller?.playbackState?.state == PlaybackState.STATE_PLAYING
        if (!playingNow) willPlay()
        if (localActive) { local.playPause(); return }
        val c = controller ?: run { if (local.lastFolder != null) { localActive = true; local.playLastFolder() }; return }
        if (playingNow) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() {
        if (localActive) { local.next(); return }
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        if (localActive) { local.previous(); return }
        controller?.transportControls?.skipToPrevious()
    }

    /** Numero di sessioni che risultano in riproduzione (per sapere se "play" ha funzionato). */
    fun playingCount(): Int = runCatching {
        manager?.getActiveSessions(listenerComponent).orEmpty().count { it.playbackState?.state == PlaybackState.STATE_PLAYING }
    }.getOrDefault(0)

    /**
     * Avvia/riprende la riproduzione di un pacchetto specifico (avvio intelligente), SENZA
     * aprire la sua interfaccia: se esiste già una sessione la comanda, altrimenti invia il
     * tasto "play" all'app. Best-effort: se l'app non espone una sessione, potrebbe non partire.
     */
    fun resumeOrPlay(packageName: String?) = handler.post {
        refreshSessions()
        val sessions = runCatching { manager?.getActiveSessions(listenerComponent).orEmpty() }.getOrDefault(emptyList())
        val target = packageName?.let { pkg -> sessions.firstOrNull { it.packageName == pkg } }
            ?: sessions.firstOrNull()
        if (target != null) {
            runCatching { target.transportControls.play() }
        } else if (packageName != null) {
            sendMediaButton(packageName)
        }
    }

    /** Invia KEYCODE_MEDIA_PLAY direttamente a un pacchetto (non apre finestre). */
    private fun sendMediaButton(packageName: String) {
        runCatching {
            val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
            val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
            listOf(down, up).forEach { ev ->
                val i = android.content.Intent(android.content.Intent.ACTION_MEDIA_BUTTON).apply {
                    setPackage(packageName)
                    putExtra(android.content.Intent.EXTRA_KEY_EVENT, ev)
                }
                app.sendBroadcast(i)
            }
        }
    }
}

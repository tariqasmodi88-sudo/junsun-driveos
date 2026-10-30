package it.driveos.launcher.media

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.IBinder
import android.provider.MediaStore

/**
 * Lettore musica interno: suona in ordine casuale TUTTI i brani audio del dispositivo (memoria
 * interna, SD, USB). Mai video: usa solo la raccolta Audio di Android. Ha una sessione media,
 * quindi la scheda Musica di DriveOS lo controlla come qualsiasi altra app.
 */
class LocalMusicService : Service() {

    companion object {
        @Volatile var lastError: String? = null

        fun permission(): String = Manifest.permission.READ_EXTERNAL_STORAGE
        fun hasPermission(ctx: Context) =
            ctx.checkSelfPermission(permission()) == PackageManager.PERMISSION_GRANTED
        fun start(ctx: Context) {
            runCatching { ctx.startForegroundService(Intent(ctx, LocalMusicService::class.java)) }
        }
    }

    private data class Track(val uri: Uri, val title: String, val artist: String, val durationMs: Long)

    private var tracks = listOf<Track>()
    private var index = 0
    private var player: MediaPlayer? = null
    private lateinit var session: MediaSession
    private var focusRequest: AudioFocusRequest? = null
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("localmusic", "Musica del dispositivo", NotificationManager.IMPORTANCE_LOW))
        if (!::session.isInitialized) {
            session = MediaSession(this, "DriveOSLocalMusic").apply {
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() { resume() }
                    override fun onPause() { pause() }
                    override fun onSkipToNext() { next() }
                    override fun onSkipToPrevious() { prev() }
                    override fun onStop() { stopSelf() }
                })
                isActive = true
            }
        }
        startForeground(2, notification("Cerco i brani…"))
        if (!started) {
            started = true
            Thread {
                tracks = loadTracks().shuffled()
                if (tracks.isEmpty()) {
                    lastError = "Nessun brano trovato sul dispositivo."
                    stopSelf()
                } else {
                    lastError = null
                    android.os.Handler(mainLooper).post { playIndex(0) }
                }
            }.start()
        } else resume()
        return START_NOT_STICKY
    }

    private fun loadTracks(): List<Track> {
        val out = ArrayList<Track>()
        runCatching {
            MediaStore.getExternalVolumeNames(this).forEach { vol ->
                val base = MediaStore.Audio.Media.getContentUri(vol)
                runCatching {
                    contentResolver.query(
                        base,
                        arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION),
                        "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 30000",
                        null, null,
                    )?.use { c ->
                        while (c.moveToNext()) {
                            out.add(Track(ContentUris.withAppendedId(base, c.getLong(0)),
                                c.getString(1) ?: "Brano", c.getString(2) ?: "", c.getLong(3)))
                        }
                    }
                }
            }
        }
        return out
    }

    private fun playIndex(i: Int) {
        if (tracks.isEmpty()) return
        index = ((i % tracks.size) + tracks.size) % tracks.size
        val t = tracks[index]
        runCatching { player?.release() }
        val mp = MediaPlayer()
        player = mp
        runCatching {
            mp.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            mp.setDataSource(this, t.uri)
            mp.setOnCompletionListener { next() }
            mp.setOnErrorListener { _, _, _ -> next(); true }
            mp.setOnPreparedListener {
                if (requestFocus()) { it.start(); publish(true) }
            }
            mp.prepareAsync()
        }.onFailure { next() }
        session.setMetadata(MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, t.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, t.artist)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, t.durationMs).build())
        getSystemService(NotificationManager::class.java).notify(2, notification(t.title))
    }

    private fun requestFocus(): Boolean {
        val am = getSystemService(AudioManager::class.java)
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pause()
            }.build()
        focusRequest = req
        return am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun resume() { if (requestFocus()) { runCatching { player?.start() }; publish(true) } }
    private fun pause() { runCatching { player?.pause() }; publish(false) }
    private fun next() = playIndex(index + 1)
    private fun prev() = playIndex(index - 1)

    private fun publish(playing: Boolean) {
        val pos = runCatching { player?.currentPosition?.toLong() }.getOrNull() ?: 0L
        session.setPlaybackState(PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_STOP)
            .setState(if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, pos, 1f).build())
    }

    private fun notification(text: String): Notification =
        Notification.Builder(this, "localmusic")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Musica del dispositivo")
            .setContentText(text)
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken))
            .setOngoing(true).build()

    override fun onDestroy() {
        runCatching { player?.release() }
        runCatching { session.release() }
        super.onDestroy()
    }
}

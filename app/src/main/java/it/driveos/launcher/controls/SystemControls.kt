package it.driveos.launcher.controls

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Comandi di sistema reali, con grafica DriveOS:
 *  - Volume multimediale tramite AudioManager (nessun permesso).
 *  - Luminosita' della finestra DriveOS (nessun permesso); opzionalmente luminosita'
 *    di sistema se l'utente concede WRITE_SETTINGS.
 * Non inventa valori: legge lo stato effettivo dal sistema.
 */
class SystemControls(context: Context) {
    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)

    data class VolumeState(val index: Int, val max: Int, val percent: Int)

    private val _volume = MutableStateFlow(readVolume())
    val volume: StateFlow<VolumeState> = _volume.asStateFlow()

    /** Frazione 0f..1f applicata alla finestra; -1f = segui il sistema. */
    private val _windowBrightness = MutableStateFlow(-1f)
    val windowBrightness: StateFlow<Float> = _windowBrightness.asStateFlow()

    fun refresh() {
        _volume.value = readVolume()
    }

    private fun readVolume(): VolumeState {
        val a = audio ?: return VolumeState(0, 0, 0)
        val max = a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val index = a.getStreamVolume(AudioManager.STREAM_MUSIC)
        return VolumeState(index, max, ControlMath.toPercent(index, max))
    }

    fun setVolumePercent(percent: Int) {
        val a = audio ?: return
        val max = a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        runCatching { a.setStreamVolume(AudioManager.STREAM_MUSIC, ControlMath.fromPercent(percent, max), 0) }
        refresh()
    }

    fun nudgeVolume(delta: Int) {
        val a = audio ?: return
        val max = a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val next = ControlMath.step(a.getStreamVolume(AudioManager.STREAM_MUSIC), delta, max)
        runCatching { a.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0) }
        refresh()
    }

    fun setWindowBrightness(fraction: Float) {
        _windowBrightness.value = fraction.coerceIn(0.05f, 1f)
    }

    fun followSystemBrightness() {
        _windowBrightness.value = -1f
    }

    fun canWriteSystemBrightness(): Boolean = Settings.System.canWrite(app)

    /** Luminosita' di sistema (solo se l'utente ha concesso WRITE_SETTINGS). */
    fun setSystemBrightness(fraction: Float): Boolean {
        if (!canWriteSystemBrightness()) return false
        return runCatching {
            Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS,
                ControlMath.toSystemBrightness(fraction))
            true
        }.getOrDefault(false)
    }

    fun systemBrightnessFraction(): Float = runCatching {
        ControlMath.brightnessFraction(Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS))
    }.getOrDefault(0.5f)
}

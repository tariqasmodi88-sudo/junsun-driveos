package it.driveos.launcher.vehicle

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Azioni assegnabili a un tasto (dell'autoradio o del volante). */
enum class KeyAction(val label: String) {
    VOL_UP("Volume +"), VOL_DOWN("Volume −"), MUTE("Muto"),
    NEXT("Avanti (brano / stazione)"), PREV("Indietro (brano / stazione)"), PLAY_PAUSE("Play / pausa"),
    HOME("Home DriveOS"), RADIO("Radio"), NAV("Navigatore"), PHONE("Telefono"), MUSIC("Musica"),
}

/** Un tasto "imparato": quando il segnale [signal] della seriale assume [value], si esegue [action]. */
data class KeyBinding(val signal: String, val value: String, val action: KeyAction)

object KeyBindingCodec {
    private const val FIELD = '\u001F'
    private const val RECORD = '\u001E'

    fun encode(list: List<KeyBinding>) =
        list.joinToString(RECORD.toString()) { "${it.signal}$FIELD${it.value}$FIELD${it.action.name}" }

    fun decode(raw: String?): List<KeyBinding> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(RECORD).mapNotNull { rec ->
            val f = rec.split(FIELD)
            if (f.size < 3) return@mapNotNull null
            val a = KeyAction.entries.firstOrNull { it.name == f[2] } ?: return@mapNotNull null
            KeyBinding(f[0], f[1], a)
        }
    }

    fun set(list: List<KeyBinding>, b: KeyBinding) = list.filterNot { it.action == b.action } + b
    fun clear(list: List<KeyBinding>, a: KeyAction) = list.filterNot { it.action == a }
}

/** Azioni che servono la schermata (cambio pagina, apertura app): le esegue la MainActivity. */
object KeyEvents {
    val flow = MutableSharedFlow<KeyAction>(extraBufferCapacity = 8)
}

/**
 * Tasti dell'autoradio e del volante letti dalla seriale. Modalita' "impara": si sceglie l'azione,
 * si preme il tasto e si salva il primo segnale nuovo che cambia (esclusi i segnali di stato noti:
 * porte, fari, retromarcia, batteria, sensori…). Poi, ogni volta che quel segnale riassume quel
 * valore, si esegue l'azione.
 */
class KeyMapper(context: Context) {
    private val app = context.applicationContext

    @Volatile var bindings: List<KeyBinding> = emptyList()
    /** Chiamata quando un tasto e' stato imparato (salva le impostazioni). */
    @Volatile var onLearned: ((KeyBinding) -> Unit)? = null

    private val _learning = MutableStateFlow<KeyAction?>(null)
    val learning: StateFlow<KeyAction?> = _learning.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    @Volatile private var learnDeadline = 0L

    fun startLearning(action: KeyAction) {
        learnDeadline = System.currentTimeMillis() + LEARN_MS
        _message.value = "Premi ora il tasto per «${action.label}»…"
        _learning.value = action
    }

    fun cancelLearning() { _learning.value = null }

    /** Variazioni arrivate dalla seriale. */
    fun onChanges(changes: Map<String, String>) {
        val action = _learning.value
        if (action != null) {
            if (System.currentTimeMillis() > learnDeadline) {
                _learning.value = null
                _message.value = "Nessun tasto rilevato. Riprova."
                return
            }
            val cand = changes.entries.firstOrNull { isKeyCandidate(it.key) } ?: return
            val b = KeyBinding(cand.key, cand.value, action)
            _learning.value = null
            _message.value = "Imparato: ${action.label} = ${cand.key} → ${cand.value}"
            onLearned?.invoke(b)
            return
        }
        for ((k, v) in changes) {
            bindings.filter { it.signal == k && it.value == v }.forEach { perform(it.action) }
        }
    }

    private fun perform(a: KeyAction) {
        val audio = app.getSystemService(AudioManager::class.java)
        when (a) {
            KeyAction.VOL_UP -> volume(audio, +1)
            KeyAction.VOL_DOWN -> volume(audio, -1)
            KeyAction.MUTE -> mute(audio)
            KeyAction.NEXT -> media(audio, KeyEvent.KEYCODE_MEDIA_NEXT)
            KeyAction.PREV -> media(audio, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            KeyAction.PLAY_PAUSE -> media(audio, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            else -> KeyEvents.flow.tryEmit(a)
        }
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var volBeforeMute = -1

    /**
     * Volume: prima il modo normale di Android; se dopo un attimo il livello non e' cambiato (su questa radio
     * il volume lo governa l'app Junsun) si manda il comando Junsun, che lo gestisce da solo.
     */
    private fun volume(audio: AudioManager?, dir: Int) {
        val a = audio ?: return
        val max = a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val before = a.getStreamVolume(AudioManager.STREAM_MUSIC)
        val target = (before + dir).coerceIn(0, max)
        if (target == before) return
        runCatching { a.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI) }
        handler.postDelayed({
            if (runCatching { a.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(before) == before) {
                val action = if (dir > 0) "forfan.intent.action.VOLUMEUP" else "forfan.intent.action.VOLUMEDN"
                runCatching { app.sendBroadcast(android.content.Intent(action)) }
            }
        }, 250)
    }

    /** Muto: porta il volume a 0 e lo ripristina al tocco successivo. */
    private fun mute(audio: AudioManager?) {
        val a = audio ?: return
        val cur = a.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (cur > 0) {
            volBeforeMute = cur
            runCatching { a.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI) }
        } else {
            val back = if (volBeforeMute > 0) volBeforeMute else a.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2
            runCatching { a.setStreamVolume(AudioManager.STREAM_MUSIC, back, AudioManager.FLAG_SHOW_UI) }
        }
    }

    private fun media(audio: AudioManager?, code: Int) {
        runCatching {
            audio?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            audio?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        }
    }

    companion object {
        private const val LEARN_MS = 8000L

        /** Segnali di stato dell'auto gia' noti: non possono essere un tasto. */
        private val STATE_KEYS = setOf(
            CanDecoder.DOOR_FL, CanDecoder.DOOR_FR, CanDecoder.DOOR_RL, CanDecoder.DOOR_RR, CanDecoder.TRUNK,
            CanDecoder.DOORS_MASK, CanDecoder.HANDBRAKE, CanDecoder.REVERSE, CanDecoder.BATTERY_V,
            CanDecoder.LIGHTS, CanDecoder.LOWBEAM, CanDecoder.FOG, CanDecoder.BELT,
            CanDecoder.REAR_SENSORS, CanDecoder.FRONT_SENSORS, "can:frames",
        )
        private val STATE_PREFIXES = listOf("can:S", "can:C22/", "can:C23/", "can:C24/", "can:C26/", "can:C41/", "can:C7d/")

        fun isKeyCandidate(key: String): Boolean =
            key.startsWith("can:") && key !in STATE_KEYS && STATE_PREFIXES.none { key.startsWith(it) }
    }
}

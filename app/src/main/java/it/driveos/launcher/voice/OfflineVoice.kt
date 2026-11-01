package it.driveos.launcher.voice

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Riconoscimento vocale OFFLINE con Vosk: il modello italiano (~50 MB) si scarica una volta
 * sola, poi tutto gira sulla radio, senza internet.
 */
class OfflineVoice(context: Context) {
    sealed class State {
        object NotInstalled : State()
        data class Downloading(val percent: Int) : State()
        object Loading : State()
        object Ready : State()
        data class Listening(val partial: String) : State()
        data class Error(val message: String) : State()
    }

    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dir = File(app.filesDir, "vosk-it")
    private val _state = MutableStateFlow<State>(if (installed()) State.Ready else State.NotInstalled)
    val state: StateFlow<State> = _state
    @Volatile private var model: Model? = null
    @Volatile private var service: SpeechService? = null

    fun installed() = File(dir, "DONE").exists()

    /** Scarica e installa il modello italiano (serve internet solo questa volta). */
    fun install(onDone: (Boolean) -> Unit = {}) {
        if (_state.value is State.Downloading) return
        scope.launch {
            val ok = runCatching {
                _state.value = State.Downloading(0)
                val c = URL(MODEL_URL).openConnection() as HttpURLConnection
                c.connectTimeout = 15000; c.readTimeout = 30000
                val total = c.contentLengthLong.coerceAtLeast(1)
                dir.deleteRecursively(); dir.mkdirs()
                val counting = object : java.io.FilterInputStream(c.inputStream) {
                    var n = 0L
                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        val r = super.read(b, off, len)
                        if (r > 0) { n += r; _state.value = State.Downloading((n * 100 / total).toInt().coerceIn(0, 99)) }
                        return r
                    }
                }
                ZipInputStream(counting.buffered()).use { zip ->
                    while (true) {
                        val e = zip.nextEntry ?: break
                        // il contenuto e' in una cartella "vosk-model-small-it-0.22/": la togliamo
                        val name = e.name.substringAfter('/', "")
                        if (name.isEmpty()) continue
                        val out = File(dir, name)
                        if (!out.canonicalPath.startsWith(dir.canonicalPath)) continue
                        if (e.isDirectory) out.mkdirs() else { out.parentFile?.mkdirs(); out.outputStream().use { zip.copyTo(it) } }
                    }
                }
                File(dir, "DONE").writeText("ok")
            }.isSuccess
            _state.value = if (ok) State.Ready else State.Error("Download del modello non riuscito: controlla la connessione e riprova.")
            if (!ok) dir.deleteRecursively()
            withContext(Dispatchers.Main) { onDone(ok) }
        }
    }

    /** Ascolta una frase; [onText] riceve il testo finale (vuoto se non ha capito nulla). */
    fun listen(onText: (String) -> Unit) {
        if (!installed()) { _state.value = State.NotInstalled; return }
        stop()
        scope.launch {
            val m = model ?: runCatching { _state.value = State.Loading; Model(dir.absolutePath) }.getOrNull()?.also { model = it }
            if (m == null) { _state.value = State.Error("Modello vocale danneggiato: reinstallalo."); return@launch }
            withContext(Dispatchers.Main) {
                runCatching {
                    val rec = Recognizer(m, 16000f)
                    val sv = SpeechService(rec, 16000f)
                    service = sv
                    _state.value = State.Listening("")
                    var done = false
                    fun finish(json: String?) {
                        if (done) return
                        done = true
                        val text = json?.let { runCatching { JSONObject(it).optString("text") }.getOrNull() }.orEmpty()
                        runCatching { sv.stop(); sv.shutdown() }
                        service = null
                        _state.value = State.Ready
                        onText(text)
                    }
                    sv.startListening(object : RecognitionListener {
                        override fun onPartialResult(hypothesis: String?) {
                            val p = hypothesis?.let { runCatching { JSONObject(it).optString("partial") }.getOrNull() }.orEmpty()
                            if (!done) _state.value = State.Listening(p)
                        }
                        override fun onResult(hypothesis: String?) {
                            val t = hypothesis?.let { runCatching { JSONObject(it).optString("text") }.getOrNull() }.orEmpty()
                            if (t.isNotBlank()) finish(hypothesis)
                        }
                        override fun onFinalResult(hypothesis: String?) = finish(hypothesis)
                        override fun onError(exception: Exception?) { _state.value = State.Error(exception?.message ?: "Errore microfono"); done = true; service = null }
                        override fun onTimeout() = finish(null)
                    }, 8000)
                }.onFailure { _state.value = State.Error(it.message ?: "Microfono non disponibile") }
            }
        }
    }

    fun stop() {
        runCatching { service?.stop(); service?.shutdown() }
        service = null
        if (_state.value is State.Listening) _state.value = State.Ready
    }

    fun dismissError() { if (_state.value is State.Error) _state.value = if (installed()) State.Ready else State.NotInstalled }

    companion object {
        const val MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-it-0.22.zip"
    }
}

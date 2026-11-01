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
    /** Lingua attiva (codice DriveOS: it, en, fr, de, es, pt). Ogni lingua ha la sua cartella. */
    @Volatile var language: String = "it"
        set(v) { if (v != field) { field = v; model = null; _state.value = if (installed()) State.Ready else State.NotInstalled } }
    private val dir get() = File(app.filesDir, "vosk-$language")
    private val _state = MutableStateFlow<State>(State.NotInstalled)
    val state: StateFlow<State> = _state
    @Volatile private var model: Model? = null
    @Volatile private var service: SpeechService? = null

    fun installed() = File(dir, "DONE").exists()
    /** Nome del modello installato per la lingua attiva (es. vosk-model-small-it-0.22). */
    fun installedModel(): String? = runCatching { File(dir, "DONE").readText().trim().ifEmpty { null } }.getOrNull()
        ?.let { if (it == "ok") VoiceLanguages.of(language).fallbackModel else it } // installazioni precedenti

    /**
     * Modello piu' recente per la lingua attiva, dall'elenco ufficiale Vosk (model-list.json):
     * tipo "small", non obsoleto, versione piu' alta. Se l'elenco non e' raggiungibile, quello di riserva.
     */
    fun latestModel(): Pair<String, String> {
        val lang = VoiceLanguages.of(language)
        runCatching {
            val c = URL(LIST_URL).openConnection() as HttpURLConnection
            c.connectTimeout = 10000; c.readTimeout = 15000
            val arr = org.json.JSONArray(c.inputStream.bufferedReader().use { it.readText() })
            val best = (0 until arr.length()).map { arr.getJSONObject(it) }
                .filter { it.optString("lang") == lang.vosk && it.optString("type") == "small" && it.optString("obsolete") != "true" }
                .maxWithOrNull(compareBy<org.json.JSONObject>({ versionKey(it.optString("version")) }))
            if (best != null) return best.getString("name") to best.getString("url")
        }
        return lang.fallbackModel to "https://alphacephei.com/vosk/models/${lang.fallbackModel}.zip"
    }

    private fun versionKey(v: String): String = v.split('.', '-').joinToString(".") { p -> p.toIntOrNull()?.let { "%06d".format(it) } ?: p }

    /** true se c'e' un modello piu' nuovo di quello installato (serve internet). */
    suspend fun updateAvailable(): Boolean = withContext(Dispatchers.IO) {
        val cur = installedModel() ?: return@withContext false
        latestModel().first != cur
    }

    init { _state.value = if (installed()) State.Ready else State.NotInstalled }

    /** Scarica e installa il modello italiano (serve internet solo questa volta). */
    fun install(onDone: (Boolean) -> Unit = {}) {
        if (_state.value is State.Downloading) return
        scope.launch {
            val ok = runCatching {
                _state.value = State.Downloading(0)
                val (modelName, modelUrl) = latestModel()
                val c = URL(modelUrl).openConnection() as HttpURLConnection
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
                val target = dir
                ZipInputStream(counting.buffered()).use { zip ->
                    while (true) {
                        val e = zip.nextEntry ?: break
                        // il contenuto e' in una cartella "vosk-model-small-it-0.22/": la togliamo
                        val name = e.name.substringAfter('/', "")
                        if (name.isEmpty()) continue
                        val out = File(target, name)
                        if (!out.canonicalPath.startsWith(target.canonicalPath)) continue
                        if (e.isDirectory) out.mkdirs() else { out.parentFile?.mkdirs(); out.outputStream().use { zip.copyTo(it) } }
                    }
                }
                File(dir, "DONE").writeText(modelName)
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
        const val LIST_URL = "https://alphacephei.com/vosk/models/model-list.json"
    }
}

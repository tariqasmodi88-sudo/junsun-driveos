package it.driveos.launcher.vehicle

import it.driveos.launcher.system.PowerGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Ascolta /dev/ttyS1 (sola lettura, in primo piano o in background) e pubblica le variazioni.
 * Se la porta non si apre (permessi, altro firmware) riprova ogni 2 s senza fare danni; se tace per 20 s la riapre.
 *
 * La linea e' la STESSA con cui la radio Junsun (com.ts.MainUI) parla con l'MCU, quindi:
 *  - mai aperta a quadro spento ne' nei primi secondi dopo l'accensione ([PowerGate]): in quei momenti la
 *    radio fa il "saluto" con l'MCU e non va disturbata;
 *  - si scrive solo il minimo (richieste dati gia' usate dalla radio stessa), mai durante il cancello;
 *  - si accettano solo messaggi con somma di controllo giusta ([CanStreamParser]).
 */
class CanSerialReader(private val path: String = "/dev/ttyS1") {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sinks = CopyOnWriteArrayList<(Map<String, String>) -> Unit>()
    @Volatile private var job: Job? = null
    @Volatile private var stream: FileInputStream? = null
    @Volatile var framesSeen: Long = 0
        private set
    /** Messaggi scartati per somma di controllo sbagliata (pezzi di messaggio divisi con la radio). */
    @Volatile var framesRejected: Long = 0
        private set
    /** Ultimo errore della richiesta dati motore (null = inviata). */
    @Volatile var queryError: String? = null
        private set
    private var queryJob: Job? = null
    @Volatile private var lastByteAt = 0L
    private var lastRejectLog = 0L

    fun addSink(s: (Map<String, String>) -> Unit) { if (!sinks.contains(s)) sinks.add(s) }
    fun removeSink(s: (Map<String, String>) -> Unit) { sinks.remove(s) }
    private fun publish(m: Map<String, String>) { if (m.isNotEmpty()) sinks.forEach { runCatching { it(m) } } }

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            // Mai durante l'avvio o il risveglio della radio: in quei secondi la radio Junsun "saluta" l'MCU
            // su questa stessa linea (vedi pause()). Si aspetta [notBefore] e che lo schermo sia acceso.
            while (isActive && (System.currentTimeMillis() < notBefore || !screenOn() || !PowerGate.ready())) delay(1000)
            startQueries()
            val filter = CanSignalFilter()
            try {
                while (isActive) {
                    // cancello richiuso senza passare da pause() (es. secondo segnale di accensione): si aspetta
                    // qui, senza riaprire la porta ogni 2 s
                    while (isActive && !PowerGate.ready()) delay(500)
                    try {
                        FileInputStream(path).use { ins ->
                            stream = ins
                            publish(filter.reset()) // dopo una riapertura si ripubblica lo stato corrente
                            lastByteAt = System.currentTimeMillis()
                            // Guardiano: se la porta tace per 20 s (lettura bloccata dopo un ciclo del quadro, ecc.)
                            // la si chiude: la lettura fallisce e riparte da capo.
                            val dog = launch {
                                while (isActive) {
                                    delay(5_000)
                                    if (System.currentTimeMillis() - lastByteAt > 20_000) { runCatching { ins.close() }; break }
                                }
                            }
                            val parser = CanStreamParser()
                            val rejectedBefore = framesRejected
                            val buf = ByteArray(512)
                            var lastExpiryCheck = 0L
                            while (isActive) {
                                val nowMs = System.currentTimeMillis()
                                // Sensori: senza aggiornamenti si azzerano, altrimenti resterebbero "rossi".
                                if (nowMs - lastExpiryCheck > 300) {
                                    lastExpiryCheck = nowMs
                                    publish(filter.expire(nowMs))
                                }
                                // Lettura SENZA attesa bloccata: si legge solo cio' che e' gia' arrivato.
                                // Cosi' allo spegnimento si smette all'istante e non resta una lettura "in sospeso"
                                // che ruberebbe alla radio la risposta dell'MCU (saluto di spegnimento/riaccensione).
                                if (paused || !PowerGate.ready()) break
                                val avail = runCatching { ins.available() }.getOrDefault(-1)
                                if (avail < 0) break
                                if (avail == 0) { delay(5); continue }
                                val n = ins.read(buf, 0, minOf(avail, buf.size))
                                if (n < 0) break
                                lastByteAt = System.currentTimeMillis()
                                val changes = LinkedHashMap<String, String>()
                                for (f in parser.feed(buf, n)) {
                                    framesSeen++
                                    // tasti fisici dell'autoradio (messaggi 'K'): nel registro, per riconoscerne di nuovi
                                    if (f.type == 'K') android.util.Log.i("DriveOSKey", f.payload.joinToString(" ") { "%02x".format(it.toInt() and 0xFF) })
                                    filter.apply(CanDecoder.decode(f), lastByteAt, changes)
                                }
                                val rejected = rejectedBefore + parser.rejected
                                if (rejected != framesRejected) {
                                    framesRejected = rejected
                                    // al massimo una riga ogni 10 s: dal registro si capisce quanto la linea e' "contesa"
                                    if (lastByteAt - lastRejectLog > 10_000) {
                                        lastRejectLog = lastByteAt
                                        android.util.Log.i("DriveOS", "seriale: $rejected messaggi scartati (somma di controllo) su $framesSeen buoni")
                                    }
                                }
                                publish(changes)
                            }
                            dog.cancel()
                        }
                    } catch (_: Throwable) { }
                    stream = null
                    if (isActive) delay(2_000)
                }
            } finally {
                // lettura ferma (quadro spento, seriale disattivata): i sensori non si conoscono piu'
                stream = null
                publish(filter.reset())
            }
        }
    }

    val running: Boolean get() = job?.isActive == true
    /** Prima di questo istante la seriale non si apre (impostato all'avvio e a ogni riaccensione). */
    @Volatile var notBefore: Long = System.currentTimeMillis() + 20_000
    /** Stato dello schermo (impostato da DriveOSApp). */
    @Volatile var screenOn: () -> Boolean = { true }
    @Volatile private var paused = false

    /**
     * Quadro spento: la seriale e' la STESSA linea con cui la radio Junsun parla con l'MCU.
     * Durante lo spegnimento/riaccensione (QuickBoot) la radio fa un "saluto" con l'MCU su questa linea:
     * se DriveOS legge o scrive in quel momento, ruba i byte e la radio mostra ErrorCode:001 e resta bloccata.
     * Quindi a schermo spento si chiude tutto, e si riapre solo qualche secondo dopo la riaccensione.
     */
    fun pause() {
        paused = paused || running || job != null
        if (!paused) return
        stopQueries()
        job?.cancel(); job = null
        runCatching { stream?.close() }
    }

    /** Riprende solo se era stata messa in pausa (non riaccende una seriale spenta nelle impostazioni). */
    fun resumeIfPaused() { if (paused) { paused = false; start() } }
    fun stopQueries() { queryJob?.cancel(); queryJob = null }
    /** true in retromarcia: chiede anche l'angolo del volante. */
    @Volatile var steerWanted = false

    /**
     * Chiede alla centralina i dati motore ogni [periodMs], come fa la schermata dati auto della radio
     * quando e' aperta (senza la richiesta la centralina non li manda). Ogni 5 giri anche stato, fari e
     * porte. Non scrive mai mentre il cancello di accensione e' chiuso.
     */
    fun startQueries(periodMs: Long = 1000) {
        if (queryJob?.isActive == true) return
        queryJob = scope.launch {
            var n = 0
            while (isActive) {
                while (isActive && (paused || !PowerGate.ready())) delay(500)
                try {
                    java.io.FileOutputStream(path).use { out ->
                        var lastEngine = 0L
                        while (isActive && !paused && PowerGate.ready()) {
                            val now = System.currentTimeMillis()
                            if (now - lastEngine >= periodMs) {
                                lastEngine = now
                                out.write(CanQuery.ENGINE); out.flush()
                                if (n++ % 5 == 0) {
                                    delay(50); out.write(CanQuery.STATUS); out.flush()
                                    // fari e porte: la centralina li manda solo quando cambiano, quindi
                                    // all'accensione DriveOS non li sapeva finche' non si toccavano i fari
                                    delay(50); out.write(CanQuery.LIGHTS); out.flush()
                                    delay(50); out.write(CanQuery.DOORS_FLAGS); out.flush()
                                }
                                queryError = null
                            }
                            // in retromarcia: angolo volante per le linee che curvano
                            if (steerWanted) { delay(20); out.write(CanQuery.EPS); out.flush(); delay(120) }
                            else delay(100)
                        }
                    }
                } catch (t: Throwable) {
                    if (t is kotlinx.coroutines.CancellationException) throw t
                    queryError = t.message ?: t.javaClass.simpleName
                    if (isActive) delay(5_000)
                }
            }
        }
    }

    fun stop() {
        paused = false
        queryJob?.cancel(); queryJob = null
        job?.cancel(); job = null
        runCatching { stream?.close() }
    }
}

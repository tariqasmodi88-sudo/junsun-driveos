package it.driveos.launcher.vehicle

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.ZonedDateTime

/**
 * Registro delle variazioni del veicolo su file, per la registrazione continua di giorni.
 * Tiene solo i CAMBIAMENTI (segnale, valore prima/dopo, ora), così dopo una settimana il
 * file contiene "tutto quello che la macchina ha fatto" senza essere enorme.
 *
 * Stato condiviso (conteggio, attivo/no) per l'interfaccia.
 */
object VehicleLog {
    private const val FILE = "vehicle_log.csv"
    private const val FILE_OLD = "vehicle_log.1.csv"
    private const val MAX_BYTES = 8L * 1024 * 1024 // 8 MB, poi ruota

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _changes = MutableStateFlow(0)
    val changes: StateFlow<Int> = _changes.asStateFlow()

    private val lock = Any()

    fun setRunning(on: Boolean) { _running.value = on }

    fun file(context: Context) = File(context.filesDir, FILE)

    /** Scrive l'intestazione se il file non esiste ancora. */
    fun ensureHeader(context: Context) = synchronized(lock) {
        val f = file(context)
        if (!f.exists() || f.length() == 0L) {
            runCatching { f.writeText(LogFormat.HEADER + "\n") }
            _changes.value = 0
        }
    }

    /** Aggiunge le righe-cambiamento al file (con rotazione se diventa grande). */
    fun append(context: Context, lines: List<String>) {
        if (lines.isEmpty()) return
        synchronized(lock) {
            val f = file(context)
            runCatching {
                if (f.length() > MAX_BYTES) {
                    val old = File(context.filesDir, FILE_OLD)
                    old.delete(); f.renameTo(old)
                    f.writeText(LogFormat.HEADER + "\n")
                }
                f.appendText(lines.joinToString("\n", postfix = "\n"))
            }
            _changes.value += lines.size
        }
    }

    fun clear(context: Context) = synchronized(lock) {
        runCatching { file(context).delete() }
        runCatching { File(context.filesDir, FILE_OLD).delete() }
        _changes.value = 0
    }

    /** Numero di righe registrate (approssimato dal contatore, aggiornato leggendo il file). */
    fun recount(context: Context) = synchronized(lock) {
        val f = file(context)
        val n = runCatching { if (f.exists()) f.bufferedReader().useLines { it.count() } - 1 else 0 }.getOrDefault(0)
        _changes.value = n.coerceAtLeast(0)
    }

    /** Contenuto completo del registro (file corrente + eventuale ruotato), per l'export. */
    fun readAll(context: Context): ByteArray = synchronized(lock) {
        val sb = StringBuilder()
        val old = File(context.filesDir, FILE_OLD)
        if (old.exists()) runCatching { sb.append(old.readText()) }
        val f = file(context)
        if (f.exists()) runCatching { sb.append(f.readText()) }
        if (sb.isEmpty()) sb.append(LogFormat.HEADER).append("\n")
        sb.toString().toByteArray()
    }

    fun nowIso(): String = ZonedDateTime.now().toString()
}

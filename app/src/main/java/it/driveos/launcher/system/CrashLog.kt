package it.driveos.launcher.system

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZonedDateTime

/**
 * Se DriveOS si chiude per un errore, salva il motivo (stack trace) in un file.
 * Al riavvio lo mostra in Impostazioni e lo include nelle esportazioni:
 * cosi' un crash sulla radio si puo' analizzare senza ADB.
 */
object CrashLog {
    private const val FILE = "ultimo_crash.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val sw = StringWriter()
                error.printStackTrace(PrintWriter(sw))
                val text = "Data: ${ZonedDateTime.now()}\nThread: ${thread.name}\n\n$sw"
                File(app.filesDir, FILE).writeText(text)
                // copia anche nella cartella dell'app sulla memoria esterna/chiavetta, se presente
                app.getExternalFilesDirs(null).filterNotNull().forEach { dir ->
                    runCatching { File(dir, FILE).writeText(text) }
                }
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? =
        runCatching { File(context.filesDir, FILE).takeIf { it.exists() }?.readText() }.getOrNull()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE).delete() }
    }
}

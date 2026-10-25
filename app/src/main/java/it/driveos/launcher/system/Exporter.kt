package it.driveos.launcher.system

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Salvataggio di file senza il selettore di sistema (che sul firmware Junsun manca o si
 * chiude: era la causa del crash di "Esporta JSON") e senza permessi:
 *  1. cartella Download/DriveOS della memoria interna (MediaStore, Android 10+);
 *  2. cartella dell'app su ogni memoria esterna montata, chiavetta USB compresa
 *     (Android/data/it.driveos.launcher/files/).
 * Restituisce dove il file e' stato scritto, in parole semplici.
 */
object Exporter {

    data class Result(val saved: List<String>, val errors: List<String>) {
        val ok get() = saved.isNotEmpty()
    }

    fun save(context: Context, fileName: String, mime: String, bytes: ByteArray): Result {
        val saved = ArrayList<String>()
        val errors = ArrayList<String>()

        // 1) Download/DriveOS
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DriveOS")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore non disponibile")
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("scrittura non riuscita")
            saved.add("Memoria interna › Download › DriveOS › $fileName")
        }.onFailure { errors.add("Download: ${it.message}") }

        // 2) cartella dell'app su ogni volume (la prima e' la memoria interna)
        val dirs = runCatching { context.getExternalFilesDirs(null).toList() }.getOrDefault(emptyList())
        dirs.forEachIndexed { index, dir ->
            if (dir == null) return@forEachIndexed
            runCatching {
                File(dir, fileName).writeBytes(bytes)
                if (index > 0) saved.add("Chiavetta/scheda (${volumeName(dir)}) › Android › data › ${context.packageName} › files › $fileName")
                else if (saved.isEmpty()) saved.add("Memoria interna › Android › data › ${context.packageName} › files › $fileName")
            }.onFailure { errors.add("${dir.path}: ${it.message}") }
        }
        return Result(saved, errors)
    }

    /** "/storage/1A2B-3C4D/Android/data/..." → "1A2B-3C4D" */
    private fun volumeName(dir: File): String =
        dir.path.removePrefix("/storage/").substringBefore('/')
}

package it.driveos.launcher.system

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Copia in Download/DriveOS/apk i file APK di app GIÀ installate sulla radio (le stesse
 * che girano sul tuo dispositivo), così si possono analizzare sul computer. Non modifica
 * nulla: legge solo i file di installazione, leggibili da qualunque app.
 */
object ApkExporter {

    /** App del firmware da analizzare (dalle schermate Informazioni app della radio). */
    val targets = listOf(
        "com.suding.speedplay",        // AutoLink / SpeedPlay (CarPlay, Android Auto)
        "com.txznet.trace",            // Junsun Security
        "com.txznet.aipal",            // Toppal Service
        "com.txznet.smartadapter",     // Toppal Voice
        "com.ts.car.client",           // client auto Junsun
        "com.ts.dvdplayer",            // lettore USB/SD
        "com.ts.MainUI",               // interfaccia Junsun (grande)
    )

    data class Result(val copied: List<String>, val skipped: List<String>)

    fun export(context: Context, packages: List<String> = targets): Result {
        val pm = context.packageManager
        val copied = ArrayList<String>()
        val skipped = ArrayList<String>()
        packages.forEach { pkg ->
            val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
            if (info == null) { skipped.add("$pkg (non installata)"); return@forEach }
            val files = listOf(info.sourceDir) + info.splitSourceDirs.orEmpty().toList()
            files.forEachIndexed { index, path ->
                val name = if (index == 0) "$pkg.apk" else "$pkg.split$index.apk"
                runCatching {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DriveOS/apk")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: error("MediaStore non disponibile")
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        File(path).inputStream().use { it.copyTo(out) }
                    } ?: error("scrittura non riuscita")
                    copied.add("$name (${File(path).length() / 1024 / 1024} MB)")
                }.onFailure { skipped.add("$name: ${it.message}") }
            }
        }
        return Result(copied, skipped)
    }
}

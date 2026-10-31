package it.driveos.launcher.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class AudioFolder(val path: String, val name: String, val count: Int)

/** Cartelle con musica, ricavate dalla raccolta Audio di Android (memoria interna, SD, USB). */
object AudioFolders {
    private val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    private val exts = listOf(".mp3", ".flac", ".m4a", ".aac", ".ogg", ".opus", ".wav", ".wma", ".mka")

    private class Row(val path: String, val uri: Uri)

    @Suppress("DEPRECATION")
    private fun fromMediaStore(ctx: Context): List<Row> = runCatching {
        val out = ArrayList<Row>()
        ctx.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val p = c.getString(1) ?: continue
                out.add(Row(p, ContentUris.withAppendedId(uri, c.getLong(0))))
            }
        }
        out
    }.getOrDefault(emptyList())

    /** Se l'indicizzatore di Android non ha catalogato i file (succede su alcune radio), si cercano direttamente nelle cartelle. */
    private fun fromDisk(): List<Row> {
        val out = ArrayList<Row>()
        val roots = ArrayList<java.io.File>()
        roots.add(java.io.File("/storage/emulated/0"))
        java.io.File("/storage").listFiles()?.forEach { if (it.name != "emulated" && it.name != "self") roots.add(it) }
        fun walk(d: java.io.File, depth: Int) {
            if (depth > 6 || out.size > 8000) return
            val kids = d.listFiles() ?: return
            for (f in kids) {
                if (f.isDirectory) { if (f.name != "Android" && !f.name.startsWith(".")) walk(f, depth + 1) }
                else if (exts.any { f.name.lowercase().endsWith(it) }) out.add(Row(f.path, Uri.fromFile(f)))
            }
        }
        roots.forEach { runCatching { walk(it, 0) } }
        return out
    }

    private fun rows(ctx: Context): List<Row> {
        val ms = fromMediaStore(ctx)
        val disk = fromDisk()
        val known = ms.map { it.path }.toHashSet()
        return ms + disk.filter { it.path !in known }
    }

    fun folders(ctx: Context): List<AudioFolder> =
        rows(ctx).groupBy { it.path.substringBeforeLast('/') }
            .map { (path, l) -> AudioFolder(path, path.substringAfterLast('/'), l.size) }
            .sortedBy { it.path.lowercase() }

    /** Brani della cartella e delle sue sottocartelle, in ordine di percorso. */
    fun tracks(ctx: Context, folder: String): List<Uri> =
        rows(ctx).filter { it.path.startsWith("$folder/") }.sortedBy { it.path.lowercase() }.map { it.uri }

    fun all(ctx: Context): List<Uri> = rows(ctx).sortedBy { it.path.lowercase() }.map { it.uri }
}

package it.driveos.launcher.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class AudioFolder(val path: String, val name: String, val count: Int)

/** Cartelle con musica, ricavate dalla raccolta Audio di Android (memoria interna, SD, USB). */
object AudioFolders {
    private val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    @Suppress("DEPRECATION")
    private fun rows(ctx: Context): List<Pair<Long, String>> = runCatching {
        val out = ArrayList<Pair<Long, String>>()
        ctx.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val p = c.getString(1) ?: continue
                out.add(c.getLong(0) to p)
            }
        }
        out
    }.getOrDefault(emptyList())

    fun folders(ctx: Context): List<AudioFolder> =
        rows(ctx).groupBy { it.second.substringBeforeLast('/') }
            .map { (path, l) -> AudioFolder(path, path.substringAfterLast('/'), l.size) }
            .sortedBy { it.path.lowercase() }

    /** Brani della cartella e delle sue sottocartelle, in ordine di percorso. */
    fun tracks(ctx: Context, folder: String): List<Uri> =
        rows(ctx).filter { it.second.startsWith("$folder/") }
            .sortedBy { it.second.lowercase() }
            .map { ContentUris.withAppendedId(uri, it.first) }

    fun all(ctx: Context): List<Uri> = rows(ctx).sortedBy { it.second.lowercase() }.map { ContentUris.withAppendedId(uri, it.first) }
}

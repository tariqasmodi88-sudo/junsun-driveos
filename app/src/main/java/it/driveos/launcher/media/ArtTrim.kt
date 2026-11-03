package it.driveos.launcher.media

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Molte copertine (MP3 presi da video) hanno bande nere "stampate" dentro l'immagine:
 * qui si tolgono, cosi' la copertina riempie davvero il riquadro.
 */
object ArtTrim {
    private fun dark(px: Int): Boolean {
        val r = Color.red(px); val g = Color.green(px); val b = Color.blue(px)
        return r < 28 && g < 28 && b < 28
    }

    /** Riga/colonna "nera" se almeno il 96% dei punti campionati e' scuro. */
    private fun rowDark(b: Bitmap, y: Int, x0: Int, x1: Int): Boolean {
        val step = maxOf(1, (x1 - x0) / 64); var n = 0; var d = 0; var x = x0
        while (x < x1) { n++; if (dark(b.getPixel(x, y))) d++; x += step }
        return n > 0 && d * 100 >= n * 96
    }
    private fun colDark(b: Bitmap, x: Int, y0: Int, y1: Int): Boolean {
        val step = maxOf(1, (y1 - y0) / 64); var n = 0; var d = 0; var y = y0
        while (y < y1) { n++; if (dark(b.getPixel(x, y))) d++; y += step }
        return n > 0 && d * 100 >= n * 96
    }

    fun trim(src: Bitmap): Bitmap = runCatching {
        val b = if (src.config == Bitmap.Config.HARDWARE) src.copy(Bitmap.Config.ARGB_8888, false) else src
        val w = b.width; val h = b.height
        if (w < 16 || h < 16) return src
        var top = 0; var bottom = h; var left = 0; var right = w
        while (top < h / 2 && rowDark(b, top, 0, w)) top++
        while (bottom > top + h / 4 && rowDark(b, bottom - 1, 0, w)) bottom--
        while (left < w / 2 && colDark(b, left, top, bottom)) left++
        while (right > left + w / 4 && colDark(b, right - 1, top, bottom)) right--
        val cw = right - left; val ch = bottom - top
        // niente da togliere, o immagine quasi tutta scura (copertina nera vera): si lascia com'e'
        if ((cw == w && ch == h) || cw < w / 4 || ch < h / 4) return src
        Bitmap.createBitmap(b, left, top, cw, ch)
    }.getOrDefault(src)
}

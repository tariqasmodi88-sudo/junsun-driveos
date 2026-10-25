package it.driveos.launcher.apps

import java.text.Normalizer
import java.util.Locale

/** Ricerca e ordinamento delle app insensibili a maiuscole e accenti ("citta" trova "Città"). */
object AppSearch {
    private val DIACRITICS = "\\p{M}+".toRegex()
    private val SPACES = "\\s+".toRegex()

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase(Locale.ROOT)
            .trim()

    /** Ogni parola della ricerca deve comparire nell'etichetta. Ricerca vuota = tutto. */
    fun matches(label: String, query: String): Boolean {
        val words = normalize(query).split(SPACES).filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val normalizedLabel = normalize(label)
        return words.all { normalizedLabel.contains(it) }
    }

    fun <T> sortByLabel(items: List<T>, label: (T) -> String): List<T> =
        items.sortedWith(compareBy<T> { normalize(label(it)) }.thenBy { label(it) })
}

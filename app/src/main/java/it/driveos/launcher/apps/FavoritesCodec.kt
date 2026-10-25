package it.driveos.launcher.apps

/**
 * Serializza l'elenco ordinato dei preferiti in una stringa (una chiave per riga).
 * Le voci malformate o duplicate vengono scartate in lettura.
 */
object FavoritesCodec {
    private const val SEPARATOR = "\n"

    fun decode(raw: String?): List<String> =
        raw?.split(SEPARATOR)
            ?.map { it.trim() }
            ?.filter { AppKey.decode(it) != null }
            ?.distinct()
            ?: emptyList()

    fun encode(favorites: List<String>): String = favorites.distinct().joinToString(SEPARATOR)

    /** Aggiunge in fondo se assente, rimuove se presente. */
    fun toggle(favorites: List<String>, key: String): List<String> =
        if (key in favorites) favorites - key else favorites + key
}

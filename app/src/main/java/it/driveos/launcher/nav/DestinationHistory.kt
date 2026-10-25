package it.driveos.launcher.nav

/** Ultime destinazioni cercate: piu' recente in testa, senza doppioni (maiuscole ignorate). */
object DestinationHistory {
    const val MAX = 8
    private const val SEPARATOR = "\n"

    fun decode(raw: String?): List<String> =
        raw?.split(SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinctBy { it.lowercase() }
            ?.take(MAX)
            ?: emptyList()

    fun encode(list: List<String>): String = list.joinToString(SEPARATOR)

    fun add(list: List<String>, destination: String): List<String> {
        val clean = destination.replace(SEPARATOR, " ").trim()
        if (clean.isEmpty()) return list
        return (listOf(clean) + list.filterNot { it.equals(clean, ignoreCase = true) }).take(MAX)
    }

    fun remove(list: List<String>, destination: String): List<String> =
        list.filterNot { it.equals(destination.trim(), ignoreCase = true) }
}

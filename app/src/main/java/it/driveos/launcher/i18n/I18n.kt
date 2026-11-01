package it.driveos.launcher.i18n

import android.content.Context
import java.util.Locale

/**
 * Traduzioni dell'interfaccia. Le scritte nel codice sono in italiano e fanno da chiave;
 * assets/i18n.tsv contiene le traduzioni (colonne: it, en, fr, de, es, pt).
 * Una scritta senza traduzione resta in italiano.
 */
object I18n {
    @Volatile var lang: String = "it"
    private var table: Map<String, Map<String, String>> = emptyMap()

    val locale: Locale get() = when (lang) { "en" -> Locale.UK; "fr" -> Locale.FRANCE; "de" -> Locale.GERMANY; "es" -> Locale("es", "ES"); "pt" -> Locale("pt", "PT"); else -> Locale.ITALY }

    fun load(context: Context) {
        runCatching {
            val lines = context.assets.open("i18n.tsv").bufferedReader().readLines()
            val head = lines.first().split('\t')
            val out = HashMap<String, Map<String, String>>()
            for (l in lines.drop(1)) {
                if (l.isBlank()) continue
                val cols = l.split('\t').map { it.replace("\\n", "\n") }
                val m = HashMap<String, String>()
                head.forEachIndexed { i, code -> cols.getOrNull(i)?.let { m[code] = it } }
                out[cols[0]] = m
            }
            table = out
        }
    }

    fun t(s: String): String = if (lang == "it") s else table[s]?.get(lang)?.takeIf { it.isNotEmpty() } ?: s
}

fun tr(s: String): String = I18n.t(s)

/** Scritta con valori: "{0} km" → "12 km". */
fun trf(template: String, vararg args: Any?): String {
    var r = I18n.t(template)
    args.forEachIndexed { i, a -> r = r.replace("{$i}", a?.toString() ?: "") }
    return r
}

package it.driveos.launcher.nav

import java.net.URLEncoder

/** Pacchetti noti con URI di navigazione documentati pubblicamente. */
object KnownNavApps {
    const val GOOGLE_MAPS = "com.google.android.apps.maps"
    const val WAZE = "com.waze"
}

/** Descrizione indipendente da Android di un intent ACTION_VIEW da lanciare. */
data class NavRequest(val uri: String, val packageName: String?)

/**
 * Costruisce gli URI ufficiali:
 * - Google Maps: google.navigation:q=...  (avvia subito la navigazione)
 * - Waze:        https://waze.com/ul?q=...&navigate=yes
 * - Altre app:   geo:0,0?q=...           (standard Android; l'app mostra il luogo)
 */
object NavIntentBuilder {

    fun forDestination(navPackage: String?, destination: String): NavRequest? {
        val clean = destination.trim()
        if (clean.isEmpty()) return null
        val q = encode(clean)
        return when (navPackage) {
            KnownNavApps.GOOGLE_MAPS -> NavRequest("google.navigation:q=$q", KnownNavApps.GOOGLE_MAPS)
            KnownNavApps.WAZE -> NavRequest("https://waze.com/ul?q=$q&navigate=yes", KnownNavApps.WAZE)
            else -> NavRequest("geo:0,0?q=$q", navPackage?.takeIf { it.isNotBlank() })
        }
    }

    /** Ripiego quando l'app scelta non accetta l'URI: geo: senza pacchetto, sceglie Android. */
    fun genericFallback(destination: String): NavRequest? {
        val clean = destination.trim()
        if (clean.isEmpty()) return null
        return NavRequest("geo:0,0?q=${encode(clean)}", null)
    }

    fun encode(text: String): String = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
}

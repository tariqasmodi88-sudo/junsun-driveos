package it.driveos.launcher.media

import it.driveos.launcher.apps.AppSearch

/**
 * Sceglie le app da proporre come sorgenti audio: quelle che si dichiarano lettori
 * musicali (MediaBrowserService / CATEGORY_APP_MUSIC), quella scelta dall'utente e
 * quelle con nomi tipici (radio, musica, Spotify...). E' un suggerimento, non un filtro.
 */
object MediaSources {
    private val KEYWORDS = listOf("music", "musica", "radio", "spotify", "youtube", "audio", "podcast", "deezer", "tidal", "amazon music", "bt music")

    fun isLikelyAudio(label: String, packageName: String, declaredPackages: Set<String>, preferredPackage: String?): Boolean {
        if (packageName == preferredPackage) return true
        if (packageName in declaredPackages) return true
        val l = AppSearch.normalize(label)
        return KEYWORDS.any { l.contains(it) }
    }
}

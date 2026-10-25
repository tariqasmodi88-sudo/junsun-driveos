package it.driveos.launcher.apps

/**
 * Identifica in modo stabile un'activity avviabile come "pacchetto/classe".
 * E' la forma con cui le scelte dell'utente vengono salvate nelle preferenze.
 */
data class AppKey(val packageName: String, val className: String) {

    fun encode(): String = "$packageName/$className"

    companion object {
        /** Restituisce null per valori vuoti o malformati, invece di lanciare eccezioni. */
        fun decode(value: String?): AppKey? {
            if (value.isNullOrBlank()) return null
            val slash = value.indexOf('/')
            if (slash <= 0 || slash == value.length - 1) return null
            val pkg = value.substring(0, slash).trim()
            val cls = value.substring(slash + 1).trim()
            if (pkg.isEmpty() || cls.isEmpty() || cls.contains('/')) return null
            return AppKey(pkg, cls)
        }
    }
}

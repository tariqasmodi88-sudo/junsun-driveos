package it.driveos.launcher.vehicle

/**
 * Riconosce le finestre del firmware Junsun che "prendono lo schermo" (nomi visti nel registro:
 * CanDoorMsgbox, CanRadarActivity, BackcarMainActivity). Logica pura, testabile.
 */
enum class WindowKind { DOOR, RADAR, REVERSE, PROJECTION, OTHER }

object WindowRules {
    fun classify(pkg: String?, cls: String?): WindowKind {
        val c = cls.orEmpty().lowercase()
        val p = pkg.orEmpty()
        val simple = c.substringAfterLast('.')
        // pagine di impostazioni Junsun (es. FsBackCarSetActivity): mai coprirle
        val settingsPage = simple.startsWith("fs") || simple.contains("set")
        return when {
            settingsPage -> WindowKind.OTHER
            c.contains("candoor") || c.contains("doormsg") -> WindowKind.DOOR
            c.contains("canradar") || c.contains("radaractivity") -> WindowKind.RADAR
            c.contains("backcar") || c.contains("rearview") -> WindowKind.REVERSE
            p == "com.suding.speedplay" -> WindowKind.PROJECTION
            else -> WindowKind.OTHER
        }
    }

    /** Pagine di impostazioni Junsun (es. FsBackCarSetActivity): aperte da te, mai coprirle. */
    fun isSettingsPage(cls: String?): Boolean {
        val simple = cls.orEmpty().lowercase().substringAfterLast('.')
        return simple.startsWith("fs") || simple.contains("set")
    }

    /** Finestre di sistema da cui tornare alla Home (la proiezione solo se richiesto). */
    fun bounceHome(kind: WindowKind, includeProjection: Boolean): Boolean = when (kind) {
        WindowKind.DOOR, WindowKind.RADAR, WindowKind.REVERSE -> true
        WindowKind.PROJECTION -> includeProjection
        WindowKind.OTHER -> false
    }
}

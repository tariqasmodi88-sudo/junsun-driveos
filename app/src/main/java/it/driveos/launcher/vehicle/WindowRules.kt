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
        return when {
            c.contains("candoor") || c.contains("doormsg") -> WindowKind.DOOR
            c.contains("canradar") || c.contains("radaractivity") -> WindowKind.RADAR
            c.contains("backcar") || c.contains("rearview") -> WindowKind.REVERSE
            p == "com.suding.speedplay" -> WindowKind.PROJECTION
            else -> WindowKind.OTHER
        }
    }

    /** Finestre di sistema da cui tornare alla Home (la proiezione solo se richiesto). */
    fun bounceHome(kind: WindowKind, includeProjection: Boolean): Boolean = when (kind) {
        WindowKind.DOOR, WindowKind.RADAR, WindowKind.REVERSE -> true
        WindowKind.PROJECTION -> includeProjection
        WindowKind.OTHER -> false
    }
}

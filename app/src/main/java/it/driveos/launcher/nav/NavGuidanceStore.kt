package it.driveos.launcher.nav

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Indicazione corrente pubblicata dall'app di navigazione nella sua notifica di guida. */
data class NavGuidance(
    val packageName: String,
    val key: String,
    val title: String,
    val text: String,
    val subText: String,
    val icon: ImageBitmap?,
)

/**
 * Riceve dal NotificationListener solo le notifiche delle app di navigazione:
 * nessun'altra notifica viene letta o conservata. Nulla viene salvato su disco.
 */
object NavGuidanceStore {
    val KNOWN_NAV_PACKAGES = setOf(KnownNavApps.GOOGLE_MAPS, KnownNavApps.WAZE)

    @Volatile
    var watchedPackages: Set<String> = KNOWN_NAV_PACKAGES
        private set

    private val _guidance = MutableStateFlow<Map<String, NavGuidance>>(emptyMap())
    val guidance: StateFlow<Map<String, NavGuidance>> = _guidance.asStateFlow()

    fun setPreferredPackage(pkg: String?) {
        watchedPackages = KNOWN_NAV_PACKAGES + listOfNotNull(pkg)
    }

    fun update(g: NavGuidance) = _guidance.update { it + (g.packageName to g) }

    fun remove(packageName: String, key: String) = _guidance.update { current ->
        if (current[packageName]?.key == key) current - packageName else current
    }

    fun clear() {
        _guidance.value = emptyMap()
    }
}

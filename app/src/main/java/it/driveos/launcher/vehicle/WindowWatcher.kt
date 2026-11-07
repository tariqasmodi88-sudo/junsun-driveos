package it.driveos.launcher.vehicle

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class WindowEvent(val pkg: String, val cls: String, val kind: WindowKind)

/** Ponte tra il servizio di accessibilità e la Home DriveOS. */
object WindowWatcher {
    private val _events = MutableSharedFlow<WindowEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<WindowEvent> = _events.asSharedFlow()

    @Volatile var service: DriveAccessibilityService? = null

    fun enabled(): Boolean = service != null
    @Volatile private var lastPkg: String? = null
    /** Pacchetto dell'ultima finestra apparsa (null se l'Accessibilita' non e' attiva). */
    fun currentPackage(): String? = lastPkg
    fun emit(e: WindowEvent) { lastPkg = e.pkg; _events.tryEmit(e) }

    /**
     * Porta davanti la Home DriveOS. Il tasto "Home" di Android si usa solo se DriveOS e' davvero
     * il launcher predefinito: altrimenti porterebbe davanti la Home Junsun (com.ts.MainUI) e la
     * radio nativa resterebbe li'. In quel caso si apre direttamente DriveOS.
     */
    /** Fino a quando un "Home" ricevuto da DriveOS e' stato mandato da DriveOS stesso (non riporta alla dashboard). */
    @Volatile var selfHomeUntil = 0L

    fun goHome(): Boolean {
        val s = service ?: return false
        selfHomeUntil = System.currentTimeMillis() + 2_500
        if (isDefaultHome(s) && s.goHome()) return true
        return runCatching {
            s.startActivity(android.content.Intent(s, it.driveos.launcher.MainActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP))
            true
        }.getOrDefault(false)
    }

    /** Tasto Indietro di sistema (per chiudere la finestrella porte della radio). */
    @Volatile var ignoreBackUntil = 0L
    fun back(): Boolean {
        val s = service ?: return false
        ignoreBackUntil = System.currentTimeMillis() + 1_500
        return s.back()
    }

    fun isDefaultHome(ctx: android.content.Context): Boolean = runCatching {
        val home = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME)
        ctx.packageManager.resolveActivity(home, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName == ctx.packageName
    }.getOrDefault(false)
}

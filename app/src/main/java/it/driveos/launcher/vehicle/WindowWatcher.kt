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

    /** Azione "Home" di Android: con DriveOS come launcher porta davanti la sua Home. */
    fun goHome(): Boolean = service?.goHome() ?: false
}

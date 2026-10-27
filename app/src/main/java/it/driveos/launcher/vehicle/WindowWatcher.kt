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
    fun emit(e: WindowEvent) { _events.tryEmit(e) }

    /** Azione "Home" di Android: con DriveOS come launcher porta davanti la sua Home. */
    fun goHome(): Boolean = service?.goHome() ?: false
}

package it.driveos.launcher.vehicle

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Velocità dal GPS della radio e stato "fermo / in marcia" (con [ParkedDetector]).
 * Serve il permesso di posizione, chiesto solo quando attivi la schermata automatica.
 * La posizione non viene salvata né inviata: si usa solo la velocità, in memoria.
 */
class SpeedWatcher(context: Context) {
    private val app = context.applicationContext
    private val lm = app.getSystemService(LocationManager::class.java)
    private val detector = ParkedDetector()

    private val _speedKmh = MutableStateFlow<Double?>(null)
    val speedKmh: StateFlow<Double?> = _speedKmh.asStateFlow()

    private val _parked = MutableStateFlow(false)
    val parked: StateFlow<Boolean> = _parked.asStateFlow()

    private var running = false
    private var fixes = 0
    private val _location = MutableStateFlow<Location?>(null)
    /** Ultima posizione GPS (per la mappa del percorso). */
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _location.value = location
            if (!location.hasSpeed()) return
            // Appena acceso il quadro il GPS non ha ancora agganciato bene i satelliti e per qualche secondo
            // da' velocita' inventate (15/11: "145 km/h" da fermo, a motore spento). Si scartano le prime
            // letture dopo l'avvio e quelle che il GPS stesso dichiara imprecise.
            if (++fixes <= SKIP_FIRST_FIXES) return
            if (location.hasAccuracy() && location.accuracy > MAX_ACCURACY_M) return
            if (android.os.Build.VERSION.SDK_INT >= 26 && location.hasSpeedAccuracy() &&
                location.speedAccuracyMetersPerSecond > MAX_SPEED_ERROR_MS) return
            val kmh = location.speed * 3.6
            _speedKmh.value = kmh
            detector.update(kmh, System.currentTimeMillis())
            _parked.value = detector.parked
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun hasPermission(): Boolean =
        app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    fun start() {
        if (running || !hasPermission() || lm == null) return
        runCatching {
            fixes = 0
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
            running = true
        }
    }

    fun stop() {
        if (!running) return
        runCatching { lm?.removeUpdates(listener) }
        running = false
        // a quadro spento l'ultima velocita' non vale piu' (alla riaccensione si riparte da "non nota")
        _speedKmh.value = null
    }

    private companion object {
        const val SKIP_FIRST_FIXES = 3
        const val MAX_ACCURACY_M = 35f
        const val MAX_SPEED_ERROR_MS = 2.5f
    }
}

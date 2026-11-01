package it.driveos.launcher.nav

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Percorso da disegnare sulla mappa della Home. Quando avvii una destinazione da DriveOS:
 *  - la destinazione viene cercata su OpenStreetMap (Nominatim);
 *  - il percorso stradale viene calcolato da OSRM dalla posizione GPS attuale;
 *  - viene ricalcolato ogni 2 minuti o se ti allontani dal tracciato.
 * Serve internet (lo stesso che usa il navigatore). Il navigatore vero resta Waze/Maps:
 * questa e' solo la vista del percorso in Home.
 */
object NavRoute {
    data class Route(
        val destination: String,
        val destLat: Double, val destLon: Double,
        val points: List<Pair<Double, Double>>, // lat, lon
        val distanceM: Double, val durationS: Double,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val _route = MutableStateFlow<Route?>(null)
    val route: StateFlow<Route?> = _route
    private val _active = MutableStateFlow<String?>(null)
    /** Destinazione in corso (anche prima che il percorso sia calcolato). */
    val active: StateFlow<String?> = _active

    fun start(destination: String, location: () -> Pair<Double, Double>?) {
        stop()
        _active.value = destination
        job = scope.launch {
            val dest = runCatching { geocode(destination) }.getOrNull() ?: return@launch
            var lastCalc = 0L
            while (isActive) {
                val here = location()
                val r = _route.value
                val off = here != null && r != null && distanceToRoute(here, r.points) > 150
                if (here != null && (r == null || off || System.currentTimeMillis() - lastCalc > 120_000)) {
                    runCatching { osrm(here, dest) }.getOrNull()?.let { (pts, d, t) ->
                        _route.value = Route(destination, dest.first, dest.second, pts, d, t); lastCalc = System.currentTimeMillis()
                    }
                }
                // arrivato: a meno di 60 m dalla destinazione si chiude
                if (here != null && haversine(here, dest) < 60) { stop(); break }
                delay(10_000)
            }
        }
    }

    fun stop() { job?.cancel(); job = null; _route.value = null; _active.value = null }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "DriveOS-launcher/1.0 (uso personale)")
        c.connectTimeout = 8000; c.readTimeout = 10000
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun geocode(q: String): Pair<Double, Double>? {
        val a = JSONArray(get("https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=it&q=" + URLEncoder.encode(q, "UTF-8")))
        if (a.length() == 0) return null
        val o = a.getJSONObject(0)
        return o.getString("lat").toDouble() to o.getString("lon").toDouble()
    }

    private fun osrm(from: Pair<Double, Double>, to: Pair<Double, Double>): Triple<List<Pair<Double, Double>>, Double, Double>? {
        val u = "https://router.project-osrm.org/route/v1/driving/${from.second},${from.first};${to.second},${to.first}?overview=full&geometries=geojson"
        val r = JSONObject(get(u)).getJSONArray("routes")
        if (r.length() == 0) return null
        val route = r.getJSONObject(0)
        val c = route.getJSONObject("geometry").getJSONArray("coordinates")
        val pts = (0 until c.length()).map { val p = c.getJSONArray(it); p.getDouble(1) to p.getDouble(0) }
        return Triple(pts, route.getDouble("distance"), route.getDouble("duration"))
    }

    fun haversine(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(b.first - a.first); val dLon = Math.toRadians(b.second - a.second)
        val h = Math.sin(dLat / 2).let { it * it } + Math.cos(Math.toRadians(a.first)) * Math.cos(Math.toRadians(b.first)) * Math.sin(dLon / 2).let { it * it }
        return 2 * r * Math.asin(Math.sqrt(h))
    }

    private fun distanceToRoute(p: Pair<Double, Double>, pts: List<Pair<Double, Double>>): Double =
        if (pts.isEmpty()) 0.0 else pts.asSequence().filterIndexed { i, _ -> i % 3 == 0 }.minOf { haversine(p, it) }
}

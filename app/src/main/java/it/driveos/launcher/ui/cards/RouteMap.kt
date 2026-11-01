package it.driveos.launcher.ui.cards

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import it.driveos.launcher.nav.NavRoute
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYZTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

/** Mappe CARTO (dati OpenStreetMap): scura per la notte, chiara "Voyager" per il giorno. */
private fun cartoSource(dark: Boolean): XYZTileSource {
    val style = if (dark) "dark_all" else "rastertiles/voyager"
    return XYZTileSource(
        if (dark) "CartoDark" else "CartoVoyager", 0, 20, 256, ".png",
        arrayOf("a", "b", "c", "d").map { "https://$it.basemaps.cartocdn.com/$style/" }.toTypedArray(),
        "© OpenStreetMap © CARTO",
    )
}

/** Freccia di navigazione dell'auto: chevron colorato con bordo bianco e alone. */
private fun carArrow(color: Int, px: Int): Bitmap {
    val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val w = px.toFloat()
    val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; alpha = 60 }
    c.drawCircle(w / 2, w / 2, w / 2, halo)
    val path = Path().apply {
        moveTo(w * 0.5f, w * 0.18f)
        lineTo(w * 0.76f, w * 0.80f)
        lineTo(w * 0.5f, w * 0.66f)
        lineTo(w * 0.24f, w * 0.80f)
        close()
    }
    c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = w * 0.09f; strokeJoin = Paint.Join.ROUND; this.color = Color.WHITE
    })
    c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; this.color = color })
    return b
}

/** Destinazione: cerchio pieno con bordo bianco e punto centrale. */
private fun destDot(px: Int): Bitmap {
    val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val r = px / 2f
    c.drawCircle(r, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
    c.drawCircle(r, r, r * 0.78f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0xF0, 0x3E, 0x5A) })
    c.drawCircle(r, r, r * 0.28f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
    return b
}

private class MapParts(map: MapView, arrow: Bitmap, dest: Bitmap) {
    val casing = Polyline(map).apply {
        outlinePaint.strokeCap = Paint.Cap.ROUND; outlinePaint.strokeJoin = Paint.Join.ROUND
    }
    val line = Polyline(map).apply {
        outlinePaint.strokeCap = Paint.Cap.ROUND; outlinePaint.strokeJoin = Paint.Join.ROUND
    }
    val destination = Marker(map).apply {
        icon = BitmapDrawable(map.resources, dest); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null)
    }
    val car = Marker(map).apply {
        icon = BitmapDrawable(map.resources, arrow); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null)
    }
    var dark: Boolean? = null
}

/**
 * Mappa del percorso in stile navigatore: solo la parte di strada ancora da fare, auto come freccia
 * nel terzo basso, mappa orientata nel senso di marcia, zoom secondo la velocita'.
 * Non riceve tocchi: il tocco lo gestisce la scheda (apre il navigatore).
 */
@Composable
fun RouteMap(
    route: NavRoute.Route?,
    location: Location?,
    dark: Boolean,
    modifier: Modifier = Modifier,
    traveledIndex: Int = 0,
    accent: Int = Color.rgb(0x3F, 0xA9, 0xFF),
) {
    val ctx = LocalContext.current
    val density = ctx.resources.displayMetrics.density
    val map = remember {
        Configuration.getInstance().apply {
            userAgentValue = ctx.packageName
            osmdroidBasePath = File(ctx.cacheDir, "osm").apply { mkdirs() }
            osmdroidTileCache = File(ctx.cacheDir, "osm/tiles").apply { mkdirs() }
        }
        MapView(ctx).apply {
            setMultiTouchControls(false)
            isFlingEnabled = false
            isClickable = false
            setOnTouchListener { _, _ -> false }
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            controller.setZoom(16.5)
        }
    }
    val parts = remember(accent) { MapParts(map, carArrow(accent, (40 * density).toInt()), destDot((22 * density).toInt())) }
    DisposableEffect(Unit) { map.onResume(); onDispose { map.onPause(); map.onDetach() } }
    AndroidView(factory = { map }, modifier = modifier, update = { m ->
        if (parts.dark != dark) {
            m.setTileSource(cartoSource(dark))
            parts.dark = dark
        }
        // l'auto sta nel terzo basso: davanti si vede piu' strada
        m.setMapCenterOffset(0, (m.height * 0.22f).toInt())
        parts.casing.outlinePaint.apply {
            color = if (dark) Color.rgb(0x0B, 0x2E, 0x52) else Color.rgb(0x0D, 0x47, 0xA1); strokeWidth = 13 * density
        }
        parts.line.outlinePaint.apply { color = accent; strokeWidth = 8 * density }

        m.overlays.clear()
        val here = location?.let { GeoPoint(it.latitude, it.longitude) }
        route?.let { r ->
            val rest = r.points.drop(traveledIndex.coerceIn(0, r.points.size)).map { GeoPoint(it.first, it.second) }
            val pts = listOfNotNull(here) + rest
            parts.casing.setPoints(pts); parts.line.setPoints(pts)
            m.overlays.add(parts.casing); m.overlays.add(parts.line)
            parts.destination.position = GeoPoint(r.destLat, r.destLon)
            m.overlays.add(parts.destination)
        }
        if (location != null && here != null) {
            parts.car.position = here
            m.overlays.add(parts.car)
            val kmh = if (location.hasSpeed()) location.speed * 3.6 else 0.0
            val zoom = when { kmh > 90 -> 15.0; kmh > 50 -> 15.8; else -> 16.6 }
            if (kotlin.math.abs(m.zoomLevelDouble - zoom) > 0.05) m.controller.setZoom(zoom)
            m.controller.setCenter(here)
            if (location.hasBearing() && kmh > 3) m.mapOrientation = -location.bearing
        } else route?.let { r -> m.controller.setCenter(GeoPoint(r.destLat, r.destLon)) }
        m.invalidate()
    })
}

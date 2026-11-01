package it.driveos.launcher.ui.cards

import android.graphics.Color
import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import it.driveos.launcher.nav.NavRoute
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File

/**
 * Mappa OpenStreetMap con il percorso (linea), la destinazione e la posizione dell'auto.
 * Segue l'auto; con il tema scuro i colori della mappa vengono invertiti.
 */
@Composable
fun RouteMap(route: NavRoute.Route?, location: Location?, dark: Boolean, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val map = remember {
        Configuration.getInstance().apply {
            userAgentValue = ctx.packageName
            osmdroidBasePath = File(ctx.cacheDir, "osm").apply { mkdirs() }
            osmdroidTileCache = File(ctx.cacheDir, "osm/tiles").apply { mkdirs() }
        }
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(16.0)
            isTilesScaledToDpi = true
        }
    }
    DisposableEffect(Unit) { map.onResume(); onDispose { map.onPause(); map.onDetach() } }
    AndroidView(factory = { map }, modifier = modifier, update = { m ->
        m.overlayManager.tilesOverlay.setColorFilter(if (dark) TilesOverlay.INVERT_COLORS else null)
        m.overlays.clear()
        route?.let { r ->
            m.overlays.add(Polyline(m).apply {
                setPoints(r.points.map { GeoPoint(it.first, it.second) })
                outlinePaint.color = Color.rgb(0x2E, 0xA8, 0xFF)
                outlinePaint.strokeWidth = 14f
            })
            m.overlays.add(Marker(m).apply {
                position = GeoPoint(r.destLat, r.destLon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = r.destination
            })
        }
        location?.let { l ->
            val p = GeoPoint(l.latitude, l.longitude)
            m.overlays.add(Marker(m).apply {
                position = p
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = android.graphics.drawable.ShapeDrawable(android.graphics.drawable.shapes.OvalShape()).apply {
                    intrinsicWidth = 44; intrinsicHeight = 44
                    paint.color = Color.rgb(0xFF, 0x9A, 0x3D)
                }
                setInfoWindow(null)
            })
            m.controller.setCenter(p)
            if (l.hasBearing()) m.mapOrientation = -l.bearing
        } ?: route?.let { r -> m.controller.setCenter(GeoPoint(r.destLat, r.destLon)) }
        m.invalidate()
    })
}

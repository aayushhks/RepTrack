package edu.bu.reptrack.ui.run

import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import edu.bu.reptrack.domain.run.RouteGeoJson
import edu.bu.reptrack.domain.run.RoutePoint
import edu.bu.reptrack.ui.theme.Chalk
import edu.bu.reptrack.ui.theme.EmberDeep
import edu.bu.reptrack.ui.theme.Ink
import edu.bu.reptrack.ui.theme.Lime
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * The maps come from OpenFreeMap: free vector map tiles made from OpenStreetMap data, with no
 * API key or account. They are drawn by MapLibre, the open-source map library. Their licenses
 * ask for the credit in R.string.map_attribution wherever a map is shown.
 */
object MapStyles {
    const val DARK = "https://tiles.openfreemap.org/styles/dark"
    const val LIGHT = "https://tiles.openfreemap.org/styles/positron"

    /** Closer than this a short walk is one street corner. */
    const val MAX_ZOOM = 17.0
}

private const val ROUTE_SOURCE = "route"
private const val START_SOURCE = "route-start"
private const val END_SOURCE = "route-end"

/**
 * A map with the route drawn on it, zoomed to fit. It follows the app theme: a dark map with a
 * lime line, or a light map with an orange one. The map is an Android View inside Compose
 * (AndroidView), so [rememberMapViewWithLifecycle] passes it the screen's lifecycle.
 */
@Composable
fun RouteMap(route: List<RoutePoint>, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val lineColor = (if (dark) Lime else EmberDeep).toArgb()
    val loadColor = MaterialTheme.colorScheme.surfaceContainerLowest.toArgb()
    val padding = with(LocalDensity.current) { 36.dp.roundToPx() }
    val mapView = rememberMapViewWithLifecycle(loadColor)

    AndroidView(factory = { mapView }, modifier = modifier)

    LaunchedEffect(mapView, route, dark) {
        if (route.size < 2) return@LaunchedEffect
        val map = mapView.awaitMap()
        map.setMaxZoomPreference(MapStyles.MAX_ZOOM)
        map.awaitStyle(routeStyle(route, if (dark) MapStyles.DARK else MapStyles.LIGHT, lineColor))
        // Fitting the camera needs the map's size, so wait until it has been laid out.
        mapView.awaitLayout()
        val bounds = LatLngBounds.Builder()
            .includes(route.map { LatLng(it.latitude, it.longitude) })
            .build()
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }
}

/** The map style plus the route on top: a soft glow, the line, a white start dot and a finish dot. */
private fun routeStyle(route: List<RoutePoint>, styleUri: String, lineColor: Int): Style.Builder =
    Style.Builder()
        .fromUri(styleUri)
        .withSource(GeoJsonSource(ROUTE_SOURCE, RouteGeoJson.line(route)))
        .withSource(GeoJsonSource(START_SOURCE, RouteGeoJson.point(route.first())))
        .withSource(GeoJsonSource(END_SOURCE, RouteGeoJson.point(route.last())))
        .withLayer(
            LineLayer("route-glow", ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(lineColor),
                PropertyFactory.lineWidth(14f),
                PropertyFactory.lineOpacity(0.25f),
                PropertyFactory.lineBlur(8f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        .withLayer(
            LineLayer("route-line", ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(lineColor),
                PropertyFactory.lineWidth(4.5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        .withLayer(
            CircleLayer("route-start-dot", START_SOURCE).withProperties(
                PropertyFactory.circleRadius(6f),
                PropertyFactory.circleColor(Chalk.toArgb()),
                PropertyFactory.circleStrokeColor(Ink.toArgb()),
                PropertyFactory.circleStrokeWidth(2.5f),
            ),
        )
        .withLayer(
            CircleLayer("route-end-dot", END_SOURCE).withProperties(
                PropertyFactory.circleRadius(7f),
                PropertyFactory.circleColor(lineColor),
                PropertyFactory.circleStrokeColor(Ink.toArgb()),
                PropertyFactory.circleStrokeWidth(3f),
            ),
        )

/**
 * One MapView for as long as the composable is on screen, moved through the same lifecycle
 * steps as the screen (create, start, resume and back), as the map library asks. Without this
 * the map keeps rendering in the background and leaks when the screen closes.
 */
@Composable
private fun rememberMapViewWithLifecycle(loadColor: Int): MapView {
    val context = LocalContext.current
    val mapView = remember {
        // The map library needs to be set up once before the first map; later calls do nothing.
        MapLibre.getInstance(context)
        val options = MapLibreMapOptions.createFromAttributes(context)
            // A TextureView instead of a SurfaceView, so the map slides with the screen transitions.
            .textureMode(true)
            .foregroundLoadColor(loadColor)
            // The credit is shown as text under the map instead of a small "i" button.
            .attributionEnabled(false)
            .logoEnabled(false)
            .compassEnabled(false)
            .rotateGesturesEnabled(false)
            .tiltGesturesEnabled(false)
        MapView(context, options)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val steps = MapViewLifecycle(mapView)
        val observer = LifecycleEventObserver { _, event -> steps.moveTo(event.targetState) }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            steps.moveTo(Lifecycle.State.DESTROYED)
        }
    }
    return mapView
}

/** Walks a MapView one lifecycle step at a time, so it never skips onStop or gets onDestroy twice. */
private class MapViewLifecycle(private val view: MapView) {
    private var state = Lifecycle.State.INITIALIZED

    fun moveTo(target: Lifecycle.State) {
        while (state != target && state != Lifecycle.State.DESTROYED) {
            state = if (target > state) stepUp() else stepDown()
        }
    }

    private fun stepUp(): Lifecycle.State = when (state) {
        Lifecycle.State.INITIALIZED -> Lifecycle.State.CREATED.also { view.onCreate(null) }
        Lifecycle.State.CREATED -> Lifecycle.State.STARTED.also { view.onStart() }
        Lifecycle.State.STARTED -> Lifecycle.State.RESUMED.also { view.onResume() }
        else -> Lifecycle.State.DESTROYED
    }

    private fun stepDown(): Lifecycle.State = when (state) {
        Lifecycle.State.RESUMED -> Lifecycle.State.STARTED.also { view.onPause() }
        Lifecycle.State.STARTED -> Lifecycle.State.CREATED.also { view.onStop() }
        Lifecycle.State.CREATED -> Lifecycle.State.DESTROYED.also { view.onDestroy() }
        else -> Lifecycle.State.DESTROYED
    }
}

private suspend fun MapView.awaitMap(): MapLibreMap = suspendCancellableCoroutine { continuation ->
    getMapAsync { map -> continuation.resume(map) }
}

private suspend fun MapLibreMap.awaitStyle(builder: Style.Builder): Style = suspendCancellableCoroutine { continuation ->
    setStyle(builder) { style -> continuation.resume(style) }
}

private suspend fun View.awaitLayout() {
    if (isLaidOut && width > 0 && height > 0) return
    suspendCancellableCoroutine<Unit> { continuation -> doOnLayout { continuation.resume(Unit) } }
}

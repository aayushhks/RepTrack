package edu.bu.reptrack.domain.run

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.sin
import kotlin.math.sinh

/** Measuring and trimming a route. Plain Kotlin so it is easy to test. */
object RouteShape {
    /** How much of each end is hidden on a shared picture by default. */
    const val HIDE_ENDS_M = 200.0

    fun lengthM(points: List<RoutePoint>): Double =
        points.zipWithNext { a, b -> RunMath.distanceM(a, b) }.sum()

    /**
     * Cuts [meters] off both ends of the route, so a shared picture does not show where the
     * run started and ended, which is often home. Like Strava's "hide start and end". A short
     * route always keeps its middle half, so there is still a line to show.
     */
    fun hideEnds(points: List<RoutePoint>, meters: Double = HIDE_ENDS_M): List<RoutePoint> {
        if (points.size < 2) return points
        val total = lengthM(points)
        val cut = minOf(meters, total / 4)
        if (cut <= 0.0) return points
        return slice(points, fromM = cut, toM = total - cut)
    }

    /** The part of the route between two distances along it, cut exactly at those distances. */
    fun slice(points: List<RoutePoint>, fromM: Double, toM: Double): List<RoutePoint> {
        val out = ArrayList<RoutePoint>()
        var walked = 0.0
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            val step = RunMath.distanceM(a, b)
            val start = walked
            val end = walked + step
            walked = end
            if (step <= 0.0 || end < fromM || start >= toM) continue
            if (out.isEmpty()) out += lerp(a, b, (fromM - start) / step)
            if (end <= toM) {
                out += b
            } else {
                out += lerp(a, b, (toM - start) / step)
                break
            }
        }
        return out
    }

    private fun lerp(a: RoutePoint, b: RoutePoint, fraction: Double): RoutePoint {
        val t = fraction.coerceIn(0.0, 1.0)
        return RoutePoint(
            latitude = a.latitude + (b.latitude - a.latitude) * t,
            longitude = a.longitude + (b.longitude - a.longitude) * t,
        )
    }
}

/** Builds GeoJSON, the format map libraries take shapes in. Coordinates go longitude first. */
object RouteGeoJson {
    fun line(points: List<RoutePoint>): String =
        feature(points.joinToString(separator = ",", prefix = """{"type":"LineString","coordinates":[""", postfix = "]}") { it.coordinates() })

    fun point(point: RoutePoint): String = feature("""{"type":"Point","coordinates":${point.coordinates()}}""")

    private fun feature(geometry: String) = """{"type":"Feature","properties":{},"geometry":$geometry}"""

    private fun RoutePoint.coordinates() = "[$longitude,$latitude]"
}

/**
 * Web Mercator, the projection web maps use. x and y run from 0 to 1 across the whole world:
 * x from the 180th meridian eastward, y from the top (north) down.
 */
object WebMercator {
    /** Beyond this the map would be infinitely tall, so web maps stop here. */
    private const val MAX_LATITUDE = 85.05112878

    fun x(longitude: Double): Double = (longitude + 180.0) / 360.0

    fun y(latitude: Double): Double {
        val s = sin(Math.toRadians(latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE)))
        return 0.5 - ln((1 + s) / (1 - s)) / (4 * PI)
    }

    fun longitude(x: Double): Double = x * 360.0 - 180.0

    fun latitude(y: Double): Double = Math.toDegrees(atan(sinh(PI * (1 - 2 * y))))
}

/** A spot in an image, in pixels from the top left corner. */
data class ScreenPoint(val x: Float, val y: Float)

/** The part of an image the route has to fit into, in pixels. */
data class PixelBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2
    val centerY: Float get() = (top + bottom) / 2
}

/**
 * Where a route lands in an image [width] × [height] pixels big. Points go through Web
 * Mercator, like a map, so a route drawn with [project] lines up exactly with a map picture
 * taken at [centerLatitude], [centerLongitude] and [zoom].
 */
class RouteFrame private constructor(
    val width: Int,
    val height: Int,
    private val centerX: Double,
    private val centerY: Double,
    /** Image pixels per whole world width. */
    private val scale: Double,
) {
    fun project(point: RoutePoint): ScreenPoint = ScreenPoint(
        x = ((WebMercator.x(point.longitude) - centerX) * scale + width / 2.0).toFloat(),
        y = ((WebMercator.y(point.latitude) - centerY) * scale + height / 2.0).toFloat(),
    )

    /** The middle of the image, where a map camera has to point. */
    val centerLatitude: Double get() = WebMercator.latitude(centerY)
    val centerLongitude: Double get() = WebMercator.longitude(centerX)

    /** The map zoom level that matches this scale when each map pixel is [pixelRatio] image pixels. */
    fun zoom(pixelRatio: Double): Double = log2(scale / (TILE_SIZE * pixelRatio))

    companion object {
        /** Map libraries draw the world 512 pixels wide at zoom 0. */
        const val TILE_SIZE = 512.0

        /**
         * Fits the route into [box], as big as it goes without zooming past [maxZoom], so a
         * short walk does not fill the picture with one street corner. Null without points.
         */
        fun fit(
            points: List<RoutePoint>,
            width: Int,
            height: Int,
            box: PixelBox,
            pixelRatio: Double,
            maxZoom: Double,
        ): RouteFrame? {
            if (points.isEmpty() || width <= 0 || height <= 0) return null
            val xs = points.map { WebMercator.x(it.longitude) }
            val ys = points.map { WebMercator.y(it.latitude) }
            val spanX = xs.max() - xs.min()
            val spanY = ys.max() - ys.min()
            val maxScale = TILE_SIZE * pixelRatio * Math.pow(2.0, maxZoom)
            val scale = minOf(
                if (spanX > 0) box.width / spanX else maxScale,
                if (spanY > 0) box.height / spanY else maxScale,
                maxScale,
            )
            // Put the middle of the route in the middle of the box, not of the whole image.
            val routeX = (xs.max() + xs.min()) / 2
            val routeY = (ys.max() + ys.min()) / 2
            return RouteFrame(
                width = width,
                height = height,
                centerX = routeX - (box.centerX - width / 2.0) / scale,
                centerY = routeY - (box.centerY - height / 2.0) / scale,
                scale = scale,
            )
        }
    }
}

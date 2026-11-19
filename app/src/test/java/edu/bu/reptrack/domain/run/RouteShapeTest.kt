package edu.bu.reptrack.domain.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteShapeTest {

    /** A straight line north from Boston, one point every 10 m. 0.001 degrees of latitude is about 111 m. */
    private fun lineNorth(meters: Int) = (0..meters / 10).map { RoutePoint(42.35 + it * 10 / 111_195.0, -71.10) }

    @Test
    fun length_addsUpTheSteps() {
        assertEquals(1_000.0, RouteShape.lengthM(lineNorth(1_000)), 1.0)
        assertEquals(0.0, RouteShape.lengthM(lineNorth(0)), 0.0)
    }

    @Test
    fun hideEnds_cuts200mOffEachEnd() {
        val route = lineNorth(1_000)
        val hidden = RouteShape.hideEnds(route)

        assertEquals(600.0, RouteShape.lengthM(hidden), 1.0)
        assertEquals(200.0, RunMath.distanceM(route.first(), hidden.first()), 1.0)
        assertEquals(200.0, RunMath.distanceM(route.last(), hidden.last()), 1.0)
    }

    @Test
    fun hideEnds_onAShortWalk_keepsTheMiddleHalf() {
        val route = lineNorth(400)
        val hidden = RouteShape.hideEnds(route)
        assertEquals(200.0, RouteShape.lengthM(hidden), 1.0)
    }

    @Test
    fun hideEnds_withTooFewPoints_changesNothing() {
        val one = listOf(RoutePoint(42.35, -71.10))
        assertEquals(one, RouteShape.hideEnds(one))
        assertTrue(RouteShape.hideEnds(emptyList()).isEmpty())
    }

    @Test
    fun slice_cutsBetweenPoints() {
        // Two points 100 m apart: the slice from 25 m to 75 m is one 50 m piece in the middle.
        val route = listOf(RoutePoint(42.35, -71.10), RoutePoint(42.35 + 100 / 111_195.0, -71.10))
        val piece = RouteShape.slice(route, fromM = 25.0, toM = 75.0)
        assertEquals(2, piece.size)
        assertEquals(50.0, RouteShape.lengthM(piece), 0.5)
    }

    @Test
    fun geoJson_putsLongitudeFirst() {
        val json = RouteGeoJson.line(listOf(RoutePoint(42.5, -71.25), RoutePoint(42.75, -71.5)))
        assertEquals(
            """{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[[-71.25,42.5],[-71.5,42.75]]}}""",
            json,
        )
        assertEquals(
            """{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[-71.25,42.5]}}""",
            RouteGeoJson.point(RoutePoint(42.5, -71.25)),
        )
    }

    @Test
    fun webMercator_goesThereAndBack() {
        assertEquals(0.5, WebMercator.x(0.0), 1e-12)
        assertEquals(0.5, WebMercator.y(0.0), 1e-12)
        assertEquals(-71.1, WebMercator.longitude(WebMercator.x(-71.1)), 1e-9)
        assertEquals(42.35, WebMercator.latitude(WebMercator.y(42.35)), 1e-9)
        // North is up: a higher latitude has a smaller y.
        assertTrue(WebMercator.y(43.0) < WebMercator.y(42.0))
    }

    private val box = PixelBox(left = 80f, top = 480f, right = 1_000f, bottom = 1_150f)

    @Test
    fun frame_fitsTheRouteInsideTheBox_andCentersIt() {
        val route = (0..100).map { RoutePoint(42.35 + it * 0.0001, -71.10 + it * 0.0003) }
        val frame = RouteFrame.fit(route, 1_080, 1_920, box, pixelRatio = 2.5, maxZoom = 17.0)!!

        val spots = route.map(frame::project)
        assertTrue(spots.all { it.x >= box.left - 0.5f && it.x <= box.right + 0.5f })
        assertTrue(spots.all { it.y >= box.top - 0.5f && it.y <= box.bottom + 0.5f })
        // As wide as the box (this route is wider than tall), and in its middle.
        assertEquals(box.width, spots.maxOf { it.x } - spots.minOf { it.x }, 1f)
        assertEquals(box.centerX, (spots.maxOf { it.x } + spots.minOf { it.x }) / 2, 1f)
        assertEquals(box.centerY, (spots.maxOf { it.y } + spots.minOf { it.y }) / 2, 1f)
    }

    @Test
    fun frame_cameraCenter_landsInTheMiddleOfTheImage() {
        val route = listOf(RoutePoint(42.35, -71.10), RoutePoint(42.36, -71.08))
        val frame = RouteFrame.fit(route, 1_080, 1_920, box, pixelRatio = 2.5, maxZoom = 17.0)!!

        val middle = frame.project(RoutePoint(frame.centerLatitude, frame.centerLongitude))
        assertEquals(540f, middle.x, 0.5f)
        assertEquals(960f, middle.y, 0.5f)
    }

    @Test
    fun frame_zoom_matchesTheScale() {
        // At zoom z the world is 512 × 2^z map pixels wide, each pixelRatio image pixels.
        val route = listOf(RoutePoint(42.35, -71.10), RoutePoint(42.35, -71.00))
        val frame = RouteFrame.fit(route, 1_080, 1_920, box, pixelRatio = 2.0, maxZoom = 20.0)!!
        val worldWidthPx = box.width / (WebMercator.x(-71.00) - WebMercator.x(-71.10))
        assertEquals(Math.log(worldWidthPx / (512 * 2.0)) / Math.log(2.0), frame.zoom(2.0), 1e-6)
    }

    @Test
    fun frame_neverZoomsPastTheLimit() {
        // Two points a meter apart would otherwise fill the whole box.
        val route = listOf(RoutePoint(42.35, -71.10), RoutePoint(42.35001, -71.10))
        val frame = RouteFrame.fit(route, 1_080, 1_920, box, pixelRatio = 2.5, maxZoom = 17.0)!!
        assertEquals(17.0, frame.zoom(2.5), 1e-9)
    }

    @Test
    fun frame_withNoPoints_isNull() {
        assertNull(RouteFrame.fit(emptyList(), 1_080, 1_920, box, pixelRatio = 2.5, maxZoom = 17.0))
    }
}

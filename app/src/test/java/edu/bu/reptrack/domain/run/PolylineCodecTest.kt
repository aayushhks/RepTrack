package edu.bu.reptrack.domain.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolylineCodecTest {

    // The worked example from Google's description of the format.
    private val googleExample = listOf(
        RoutePoint(38.5, -120.2),
        RoutePoint(40.7, -120.95),
        RoutePoint(43.252, -126.453),
    )

    @Test
    fun encode_matchesGooglesExample() {
        assertEquals("_p~iF~ps|U_ulLnnqC_mqNvxq`@", PolylineCodec.encode(googleExample))
    }

    @Test
    fun decode_matchesGooglesExample() {
        val points = PolylineCodec.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(googleExample.size, points.size)
        googleExample.zip(points).forEach { (expected, actual) ->
            assertEquals(expected.latitude, actual.latitude, 1e-6)
            assertEquals(expected.longitude, actual.longitude, 1e-6)
        }
    }

    @Test
    fun aRunAroundBoston_comesBackWithinAMeter() {
        val route = (0 until 500).map { i -> RoutePoint(42.35 + i * 0.00003, -71.10 - i * 0.00002) }
        val back = PolylineCodec.decode(PolylineCodec.encode(route))
        assertEquals(route.size, back.size)
        route.zip(back).forEach { (a, b) -> assertTrue(RunMath.distanceM(a, b) < 1.0) }
    }

    @Test
    fun allFourSignCombinations_survive() {
        val route = listOf(RoutePoint(1.5, 2.5), RoutePoint(-33.9, 151.2), RoutePoint(-0.00001, -0.00001), RoutePoint(51.5, -0.12))
        val back = PolylineCodec.decode(PolylineCodec.encode(route))
        route.zip(back).forEach { (a, b) ->
            assertEquals(a.latitude, b.latitude, 1e-5)
            assertEquals(a.longitude, b.longitude, 1e-5)
        }
    }

    @Test
    fun emptyRoute_isEmptyText() {
        assertEquals("", PolylineCodec.encode(emptyList()))
        assertTrue(PolylineCodec.decode("").isEmpty())
    }

    @Test
    fun damagedText_keepsThePointsBeforeTheDamage_insteadOfCrashing() {
        val text = PolylineCodec.encode(googleExample)
        // Cut in the middle of the last number, then add a character that is not polyline text.
        assertEquals(2, PolylineCodec.decode(text.dropLast(2)).size)
        assertEquals(1, PolylineCodec.decode(text.take(10) + "\n").size)
    }
}

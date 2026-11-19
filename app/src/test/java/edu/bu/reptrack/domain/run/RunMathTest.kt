package edu.bu.reptrack.domain.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunMathTest {

    @Test
    fun distance_oneThousandthOfADegreeNorth_isAbout111Meters() {
        val a = GeoPoint(42.350, -71.100, 5f, 0)
        val b = GeoPoint(42.351, -71.100, 5f, 0)
        assertEquals(111.2, RunMath.distanceM(a, b), 0.2)
    }

    @Test
    fun distance_samePoint_isZero() {
        val a = GeoPoint(42.35, -71.10, 5f, 0)
        assertEquals(0.0, RunMath.distanceM(a, a), 0.0001)
    }

    @Test
    fun pace_fiveKilometersInTwentyFiveMinutes_isFiveMinutesPerKm() {
        assertEquals(300L, RunMath.paceSecondsPerKm(5_000.0, 25 * 60_000L))
    }

    @Test
    fun pace_isNull_untilThereIsEnoughDistance() {
        assertNull(RunMath.paceSecondsPerKm(20.0, 60_000L))
    }

    @Test
    fun met_followsTheCompendiumTable() {
        // 1.34 m/s is 3.0 mph: moderate walking, 3.8 MET.
        assertEquals(3.8, RunMath.metFor(1.34), 0.0)
        // 2.68 m/s is 6.0 mph: running 6 to 6.3 mph, 9.3 MET.
        assertEquals(9.3, RunMath.metFor(2.69), 0.0)
        // Standing still still counts as the slowest walk.
        assertEquals(2.3, RunMath.metFor(0.0), 0.0)
    }

    @Test
    fun calories_70kg_fiveKmInThirtyMinutes() {
        // 10 km/h is 6.2 mph, so 9.3 MET for half an hour: 9.3 x 70 x 0.5 = 325.5.
        assertEquals(326, RunMath.caloriesKcal(70f, 5_000.0, 30 * 60_000L))
    }

    @Test
    fun calories_areZero_withNoDistanceOrTime() {
        assertEquals(0, RunMath.caloriesKcal(70f, 0.0, 60_000L))
        assertEquals(0, RunMath.caloriesKcal(70f, 100.0, 0L))
    }
}

package edu.bu.reptrack.domain.run

import edu.bu.reptrack.testing.pointNorth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunProgressTest {

    @Test
    fun firstGoodFix_startsTheRun_withNoDistance() {
        val progress = RunProgress().add(pointNorth(0.0, timeMillis = 0))
        assertTrue(progress.hasFix)
        assertEquals(0.0, progress.distanceM, 0.0)
    }

    @Test
    fun steadyRun_addsUpTheDistance() {
        // 10 m every 3 seconds is 3.3 m/s, a normal run.
        var progress = RunProgress()
        for (i in 0..10) progress = progress.add(pointNorth(i * 10.0, timeMillis = i * 3_000L))
        assertEquals(100.0, progress.distanceM, 0.5)
    }

    @Test
    fun fuzzyFix_isIgnored() {
        val progress = RunProgress()
            .add(pointNorth(0.0, 0))
            .add(pointNorth(50.0, 15_000, accuracyM = 80f))
        assertEquals(0.0, progress.distanceM, 0.0)
    }

    @Test
    fun gpsJump_addsNoDistance_andTheRunCarriesOn() {
        val progress = RunProgress()
            .add(pointNorth(0.0, 0))
            .add(pointNorth(500.0, 2_000)) // 250 m/s: a jump, not a runner
            .add(pointNorth(10.0, 4_000)) // back on track: also a jump, adds nothing
            .add(pointNorth(20.0, 6_000))
        assertEquals(10.0, progress.distanceM, 0.2)
    }

    @Test
    fun farAwayFirstFix_doesNotBlockTheRun() {
        // The first fix is an old place 50 km away. The real run starts right after.
        var progress = RunProgress().add(pointNorth(50_000.0, 0))
        for (i in 1..11) progress = progress.add(pointNorth((i - 1) * 10.0, timeMillis = i * 3_000L))
        assertEquals(100.0, progress.distanceM, 0.5)
    }

    @Test
    fun standingStill_driftDoesNotAddUp() {
        var progress = RunProgress().add(pointNorth(0.0, 0))
        for (i in 1..20) progress = progress.add(pointNorth(if (i % 2 == 0) 1.0 else 0.0, i * 2_000L))
        assertEquals(0.0, progress.distanceM, 0.0)
    }

    @Test
    fun fixFromThePast_isIgnored() {
        val progress = RunProgress()
            .add(pointNorth(0.0, 10_000))
            .add(pointNorth(20.0, 5_000))
        assertEquals(0.0, progress.distanceM, 0.0)
        assertFalse(progress.lastPoint?.timeMillis == 5_000L)
    }

    // ---- The route drawn on the map ----

    @Test
    fun route_keepsEveryGoodFix_inOrder() {
        var progress = RunProgress()
        for (i in 0..10) progress = progress.add(pointNorth(i * 10.0, timeMillis = i * 3_000L))
        assertEquals(11, progress.route.size)
        assertEquals(pointNorth(0.0, 0).latitude, progress.route.first().latitude, 1e-12)
        assertEquals(pointNorth(100.0, 0).latitude, progress.route.last().latitude, 1e-12)
    }

    @Test
    fun route_skipsFuzzyFixesAndDrift() {
        var progress = RunProgress().add(pointNorth(0.0, 0))
        progress = progress.add(pointNorth(50.0, 15_000, accuracyM = 80f)) // fuzzy
        progress = progress.add(pointNorth(1.0, 17_000)) // drift while standing
        progress = progress.add(pointNorth(10.0, 20_000)) // a real step
        assertEquals(2, progress.route.size)
    }

    @Test
    fun route_dropsAFarAwayFirstFix_soTheMapDoesNotDrawALineFromThere() {
        var progress = RunProgress().add(pointNorth(50_000.0, 0))
        for (i in 1..11) progress = progress.add(pointNorth((i - 1) * 10.0, timeMillis = i * 3_000L))
        assertEquals(11, progress.route.size)
        assertTrue(progress.route.all { it.latitude < pointNorth(1_000.0, 0).latitude })
    }

    @Test
    fun route_afterAJumpMidRun_carriesOnFromTheNewFix() {
        val progress = RunProgress()
            .add(pointNorth(0.0, 0))
            .add(pointNorth(10.0, 3_000))
            .add(pointNorth(600.0, 5_000)) // a jump after the run has started
        assertEquals(10.0, progress.distanceM, 0.2)
        assertEquals(3, progress.route.size)
    }
}

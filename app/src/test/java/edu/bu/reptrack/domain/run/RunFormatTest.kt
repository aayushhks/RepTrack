package edu.bu.reptrack.domain.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunFormatTest {

    @Test
    fun km_hasTwoDecimals() {
        assertEquals("1.23", RunFormat.km(1_234.5))
        assertEquals("0.00", RunFormat.km(0.0))
    }

    @Test
    fun duration_minutesAndSeconds_thenHours() {
        assertEquals("05:07", RunFormat.duration(307_000))
        assertEquals("1:02:03", RunFormat.duration(3_723_000))
    }

    @Test
    fun pace_showsMinutesAndSeconds_orDashes() {
        assertEquals("5:12", RunFormat.pace(312))
        assertEquals("--:--", RunFormat.pace(null))
        assertEquals("--:--", RunFormat.pace(4_000))
    }

    @Test
    fun walkOrRun_isDecidedByAverageSpeed() {
        // 5 km in 60 min is 1.4 m/s: a walk. 5 km in 25 min is 3.3 m/s: a run.
        assertTrue(RunSummary(startedAt = 0, finishedAt = 0, distanceM = 5_000.0, durationMs = 3_600_000, calories = 0).isWalk)
        assertFalse(RunSummary(startedAt = 0, finishedAt = 0, distanceM = 5_000.0, durationMs = 1_500_000, calories = 0).isWalk)
    }
}

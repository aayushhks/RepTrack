package edu.bu.reptrack.domain.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class RestTest {

    @Test
    fun format_roundsUpToTheNextSecond() {
        assertEquals("1:30", Rest.format(90_000))
        assertEquals("1:23", Rest.format(82_100))
        assertEquals("0:01", Rest.format(1))
        assertEquals("0:00", Rest.format(0))
    }

    @Test
    fun remaining_neverGoesBelowZero() {
        assertEquals(30_000L, Rest.remainingMs(endsAt = 100_000, now = 70_000))
        assertEquals(0L, Rest.remainingMs(endsAt = 100_000, now = 120_000))
    }

    @Test
    fun fractionLeft_isBetweenZeroAndOne() {
        assertEquals(0.5f, Rest.fractionLeft(45_000, 90_000), 0.0001f)
        assertEquals(1f, Rest.fractionLeft(120_000, 90_000), 0.0001f)
        assertEquals(0f, Rest.fractionLeft(10_000, 0), 0.0001f)
    }
}

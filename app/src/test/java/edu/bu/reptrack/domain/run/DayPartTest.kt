package edu.bu.reptrack.domain.run

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DayPartTest {

    @Test
    fun hours_namedLikeStrava() {
        assertEquals(DayPart.NIGHT, DayPart.of(4))
        assertEquals(DayPart.MORNING, DayPart.of(5))
        assertEquals(DayPart.MORNING, DayPart.of(10))
        assertEquals(DayPart.LUNCH, DayPart.of(11))
        assertEquals(DayPart.LUNCH, DayPart.of(13))
        assertEquals(DayPart.AFTERNOON, DayPart.of(14))
        assertEquals(DayPart.AFTERNOON, DayPart.of(17))
        assertEquals(DayPart.EVENING, DayPart.of(18))
        assertEquals(DayPart.EVENING, DayPart.of(21))
        assertEquals(DayPart.NIGHT, DayPart.of(22))
        assertEquals(DayPart.NIGHT, DayPart.of(0))
    }

    @Test
    fun time_usesTheLocalHour() {
        val boston = ZoneId.of("America/New_York")
        val sevenAm = ZonedDateTime.of(2026, 11, 19, 7, 42, 0, 0, boston).toInstant().toEpochMilli()
        assertEquals(DayPart.MORNING, DayPart.of(sevenAm, boston))
        // The same moment is 12:42 in London: lunch there.
        assertEquals(DayPart.LUNCH, DayPart.of(sevenAm, ZoneId.of("Europe/London")))
    }
}

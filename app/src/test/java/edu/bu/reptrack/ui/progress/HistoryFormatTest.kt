package edu.bu.reptrack.ui.progress

import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryFormatTest {

    @Test
    fun date_isShortDayAndMonth() {
        // 1794240000000 is Monday, November 9, 2026 at 16:00 UTC.
        assertEquals("Mon, Nov 9", formatWorkoutDate(1_794_240_000_000, ZoneOffset.UTC, Locale.US))
    }

    @Test
    fun minutes_splitIntoHoursAndMinutes() {
        assertEquals(1L to 15L, splitHoursMinutes(75))
        assertEquals(0L to 52L, splitHoursMinutes(52))
    }
}

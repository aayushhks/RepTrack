package edu.bu.reptrack.domain.workout

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeekActivityTest {

    private val zone = ZoneOffset.UTC

    // Noon UTC on a date, so the day never shifts.
    private fun at(date: String) = LocalDate.parse(date).atTime(12, 0).toInstant(zone).toEpochMilli()

    // Wednesday, November 18, 2026.
    private val now = at("2026-11-18")

    @Test
    fun week_runsMondayToSunday_andMarksToday() {
        val week = WeekActivity.thisWeek(emptyList(), emptyList(), now, zone)

        assertEquals(7, week.size)
        assertEquals(LocalDate.parse("2026-11-16"), week.first().date)
        assertEquals(LocalDate.parse("2026-11-22"), week.last().date)
        assertEquals(listOf(false, false, true, false, false, false, false), week.map { it.isToday })
    }

    @Test
    fun week_showsWorkoutsAndRuns_andAWorkoutWinsOnTheSameDay() {
        val week = WeekActivity.thisWeek(
            workoutTimes = listOf(at("2026-11-16"), at("2026-11-18"), at("2026-11-09")),
            runTimes = listOf(at("2026-11-17"), at("2026-11-18")),
            nowMillis = now,
            zone = zone,
        )

        assertEquals(
            listOf(DayKind.WORKOUT, DayKind.RUN, DayKind.WORKOUT, DayKind.NONE, DayKind.NONE, DayKind.NONE, DayKind.NONE),
            week.map { it.kind },
        )
    }

    @Test
    fun streak_countsDaysInARow_endingToday() {
        val times = listOf(at("2026-11-16"), at("2026-11-17"), at("2026-11-18"), at("2026-11-14"))

        assertEquals(3, WeekActivity.streakDays(times, now, zone))
    }

    @Test
    fun streak_isNotBrokenYet_byNothingToday() {
        val times = listOf(at("2026-11-16"), at("2026-11-17"))

        assertEquals(2, WeekActivity.streakDays(times, now, zone))
    }

    @Test
    fun streak_isZero_afterAMissedDay() {
        assertEquals(0, WeekActivity.streakDays(listOf(at("2026-11-15")), now, zone))
        assertEquals(0, WeekActivity.streakDays(emptyList(), now, zone))
    }

    @Test
    fun nextRoutine_goesRoundThePlan() {
        val routines = StarterRoutines.forPlan(edu.bu.reptrack.domain.profile.PlanType.UPPER_LOWER)

        assertEquals("upper", nextRoutine(routines, lastRoutineId = null)!!.id)
        assertEquals("lower", nextRoutine(routines, lastRoutineId = "upper")!!.id)
        assertEquals("upper", nextRoutine(routines, lastRoutineId = "lower")!!.id)
        assertEquals("upper", nextRoutine(routines, lastRoutineId = "full_body_a")!!.id)
        assertNull(nextRoutine(emptyList(), lastRoutineId = null))
    }
}

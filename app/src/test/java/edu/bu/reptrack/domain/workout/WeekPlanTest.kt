package edu.bu.reptrack.domain.workout

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeekPlanTest {

    private val a = WorkoutTemplate("a", "Full body A", listOf("Squat"))
    private val b = WorkoutTemplate("b", "Full body B", listOf("Deadlift"))

    /** Wednesday. */
    private val today = LocalDate.parse("2026-11-18")
    private val monWedFri = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

    @Test
    fun defaultDays_areSpreadOverTheWeek() {
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), WeekPlan.defaultDays(2))
        assertEquals(monWedFri, WeekPlan.defaultDays(3))
        assertEquals(4, WeekPlan.defaultDays(4).size)
        assertEquals(7, WeekPlan.defaultDays(9).size)
    }

    @Test
    fun trainingDays_preferTheUsersOwn() {
        assertEquals(setOf(DayOfWeek.SUNDAY), WeekPlan.trainingDays(setOf(DayOfWeek.SUNDAY), 3))
        assertEquals(monWedFri, WeekPlan.trainingDays(emptySet(), 3))
    }

    @Test
    fun week_showsDone_missed_today_planned_andDaysOff() {
        val week = WeekPlan.build(
            today = today,
            trainingDays = monWedFri,
            routines = listOf(a, b),
            next = b,
            done = listOf(WeekPlan.Done(LocalDate.parse("2026-11-16"), "a", "Full body A")),
            runDates = setOf(LocalDate.parse("2026-11-17")),
        )

        assertEquals(DayOfWeek.MONDAY, week.first().date.dayOfWeek)
        assertEquals(PlanDayStatus.DONE, week[0].status)
        assertEquals("Full body A", week[0].routineName)
        assertEquals(PlanDayStatus.RAN, week[1].status)
        // Today gets the next routine, then the plan goes round: Friday is A again.
        assertEquals(PlanDayStatus.TODAY, week[2].status)
        assertEquals("b", week[2].routineId)
        assertEquals(PlanDayStatus.PLANNED, week[4].status)
        assertEquals("a", week[4].routineId)
        assertEquals(PlanDayStatus.REST, week[6].status)
        assertNull(week[6].routineId)
    }

    @Test
    fun aMissedDay_doesNotUseUpARoutine() {
        val week = WeekPlan.build(today, monWedFri, listOf(a, b), next = a, done = emptyList(), runDates = emptySet())

        assertEquals(PlanDayStatus.MISSED, week[0].status)
        assertEquals("a", week[2].routineId)
        assertEquals("b", week[4].routineId)
    }

    @Test
    fun workoutDoneToday_showsAsDone_andTheNextOneMovesOn() {
        val week = WeekPlan.build(
            today = today,
            trainingDays = monWedFri,
            routines = listOf(a, b),
            next = b,
            done = listOf(WeekPlan.Done(today, "a", "Full body A")),
            runDates = emptySet(),
        )

        assertEquals(PlanDayStatus.DONE, week[2].status)
        assertEquals("b", week[4].routineId)
    }

    @Test
    fun aWorkoutOnADayOff_stillCounts() {
        val saturday = LocalDate.parse("2026-11-21")
        val week = WeekPlan.build(saturday, monWedFri, listOf(a, b), next = a, done = listOf(WeekPlan.Done(saturday, "b", "Full body B")), runDates = emptySet())
        assertEquals(PlanDayStatus.DONE, week[5].status)
    }
}

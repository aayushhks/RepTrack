package edu.bu.reptrack.domain.workout

import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.TrainingPlace
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // ---- The user's own plan: a routine for each day ----

    private val push = WorkoutTemplate("custom_1", "Push", listOf("Bench press"))
    private val legs = WorkoutTemplate("custom_2", "Legs", listOf("Squat"))
    private val ownWeek = mapOf(DayOfWeek.MONDAY to push, DayOfWeek.WEDNESDAY to legs, DayOfWeek.SATURDAY to push)

    @Test
    fun ownPlan_eachDayHasItsRoutine() {
        val week = WeekPlan.buildScheduled(
            today = today,
            schedule = ownWeek,
            done = emptyList(),
            runDates = setOf(LocalDate.parse("2026-11-17")),
        )

        assertEquals(PlanDayStatus.MISSED, week[0].status)
        assertEquals("Push", week[0].routineName)
        assertEquals(PlanDayStatus.RAN, week[1].status)
        assertEquals(PlanDayStatus.TODAY, week[2].status)
        assertEquals("Legs", week[2].routineName)
        assertEquals(PlanDayStatus.REST, week[3].status)
        assertEquals(PlanDayStatus.PLANNED, week[5].status)
        assertEquals("Push", week[5].routineName)
        assertEquals(PlanDayStatus.REST, week[6].status)
    }

    @Test
    fun ownPlan_doneDaysShowWhatWasDone() {
        val week = WeekPlan.buildScheduled(today, ownWeek, listOf(WeekPlan.Done(LocalDate.parse("2026-11-16"), "custom_1", "Push")), emptySet())
        assertEquals(PlanDayStatus.DONE, week[0].status)
        assertEquals("Push", week[0].routineName)
    }

    @Test
    fun ownPlan_upNext_isTodays_untilItIsDone_thenTheNextDays() {
        assertEquals(legs, WeekPlan.nextScheduled(today, ownWeek, doneToday = false))
        assertEquals(push, WeekPlan.nextScheduled(today, ownWeek, doneToday = true))
        // Thursday: nothing until Saturday.
        assertEquals(push, WeekPlan.nextScheduled(today.plusDays(1), ownWeek, doneToday = false))
        assertNull(WeekPlan.nextScheduled(today, emptyMap(), doneToday = false))
    }

    @Test
    fun ownPlan_oneDayAWeek_findsThatDayNextWeek() {
        val onlyWednesday = mapOf(DayOfWeek.WEDNESDAY to legs)
        assertEquals(legs, WeekPlan.nextScheduled(today, onlyWednesday, doneToday = true))
    }

    @Test
    fun snapshot_usesTheOwnPlan_whenThereIsOne() {
        val custom = CustomPlan(listOf(push, legs), mapOf(DayOfWeek.MONDAY to "custom_1", DayOfWeek.WEDNESDAY to "custom_2"))
        val snapshot = PlanSnapshot.of(
            goal = FitnessGoal.BUILD_MUSCLE,
            place = TrainingPlace.GYM,
            edits = emptyMap(),
            chosenDays = setOf(DayOfWeek.SUNDAY),
            history = emptyList(),
            runTimes = emptyList(),
            nowMillis = today.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
            zone = ZoneOffset.UTC,
            custom = custom,
        )

        assertTrue(snapshot.isCustom)
        assertEquals(listOf(push, legs), snapshot.routines)
        assertEquals(legs, snapshot.next)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), snapshot.trainingDays)
        assertEquals("Legs", snapshot.week[2].routineName)
    }
}

package edu.bu.reptrack.domain.workout

import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomPlanTest {

    private val push = WorkoutTemplate("custom_1", "Push", listOf("Bench press"))
    private val pull = WorkoutTemplate("custom_2", "Pull", listOf("Pull-up"))

    @Test
    fun rotation_givesTheDaysTheRoutinesInTurn() {
        assertEquals(
            mapOf(MONDAY to 0, TUESDAY to 1, THURSDAY to 0, FRIDAY to 1),
            CustomPlan.rotation(2, setOf(FRIDAY, MONDAY, THURSDAY, TUESDAY)),
        )
        assertEquals(mapOf(MONDAY to 0, WEDNESDAY to 0), CustomPlan.rotation(1, setOf(MONDAY, WEDNESDAY)))
        assertEquals(emptyMap<Any, Any>(), CustomPlan.rotation(0, setOf(MONDAY)))
    }

    @Test
    fun scheduledRoutines_skipDaysOnARoutineThatIsGone() {
        val plan = CustomPlan(listOf(push, pull), mapOf(MONDAY to "custom_1", WEDNESDAY to "custom_9"))
        assertEquals(mapOf(MONDAY to push), plan.scheduledRoutines)
        assertTrue(plan.isActive)
        assertFalse(CustomPlan().isActive)
    }

    @Test
    fun deletingARoutine_makesItsDaysRestDays() {
        val schedule = mapOf(MONDAY to "custom_1", WEDNESDAY to "custom_2", FRIDAY to "custom_1")
        assertEquals(mapOf(WEDNESDAY to "custom_2"), CustomPlan.scheduleAfterDelete(schedule, "custom_1", listOf("custom_2")))
    }

    @Test
    fun deletingTheOnlyScheduledRoutine_givesItsDaysToTheFirstOneLeft() {
        val schedule = mapOf(MONDAY to "custom_1", FRIDAY to "custom_1")
        assertEquals(
            mapOf(MONDAY to "custom_2", FRIDAY to "custom_2"),
            CustomPlan.scheduleAfterDelete(schedule, "custom_1", listOf("custom_2", "custom_3")),
        )
    }

    @Test
    fun routineName_isTrimmed_andCut() {
        assertEquals("Push day", RoutineName.clean("  Push   day "))
        assertNull(RoutineName.clean("   "))
        assertEquals(RoutineName.MAX_LENGTH, RoutineName.clean("x".repeat(50))?.length)
    }

    @Test
    fun findRoutine_ownFirst_thenBuiltInWithEdits() {
        val own = CustomPlan(listOf(push))
        assertEquals(push, findRoutine("custom_1", emptyMap(), own))
        assertEquals(listOf("Dips"), findRoutine("upper", mapOf("upper" to listOf("Dips")), own)?.exercises)
        assertNull(findRoutine("nope", emptyMap(), own))
    }
}

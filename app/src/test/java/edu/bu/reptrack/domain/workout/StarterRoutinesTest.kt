package edu.bu.reptrack.domain.workout

import edu.bu.reptrack.domain.profile.PlanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StarterRoutinesTest {

    private val allRoutines = PlanType.entries.flatMap { StarterRoutines.forPlan(it) }

    @Test
    fun everyPlan_hasAtLeastOneRoutine() {
        for (type in PlanType.entries) {
            assertTrue("$type has no routines", StarterRoutines.forPlan(type).isNotEmpty())
        }
    }

    @Test
    fun everyRoutine_hasThreeOrMoreExercises() {
        for (routine in allRoutines) {
            assertTrue("${routine.id} is too short", routine.exercises.size >= 3)
        }
    }

    @Test
    fun routineIds_areUnique() {
        val ids = allRoutines.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun find_returnsTheRoutine_orNull() {
        assertEquals("Upper body", StarterRoutines.find("upper")?.name)
        assertNull(StarterRoutines.find("does_not_exist"))
    }
}

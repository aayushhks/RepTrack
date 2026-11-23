package edu.bu.reptrack.domain.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineEditsTest {

    private val upper = listOf("Bench press", "Barbell row", "Overhead press")

    @Test
    fun add_putsTheExerciseAtTheEnd_once() {
        assertEquals(upper + "Pull-up", RoutineEdits.add(upper, "Pull-up"))
        assertEquals(upper, RoutineEdits.add(upper, "bench PRESS"))
        assertEquals(upper, RoutineEdits.add(upper, "  "))
    }

    @Test
    fun remove_takesOneOut_butNeverTheLast() {
        assertEquals(listOf("Bench press", "Overhead press"), RoutineEdits.remove(upper, 1))
        assertEquals(listOf("Plank"), RoutineEdits.remove(listOf("Plank"), 0))
        assertEquals(upper, RoutineEdits.remove(upper, 7))
    }

    @Test
    fun replace_swapsInPlace_andKeepsTheRest() {
        assertEquals(listOf("Bench press", "Lat pulldown", "Overhead press"), RoutineEdits.replace(upper, 1, "Lat pulldown"))
        // Already elsewhere in the routine: no duplicate.
        assertEquals(upper, RoutineEdits.replace(upper, 1, "Overhead press"))
    }

    @Test
    fun move_shiftsOnePlace_andStopsAtTheEnds() {
        assertEquals(listOf("Barbell row", "Bench press", "Overhead press"), RoutineEdits.move(upper, 1, -1))
        assertEquals(listOf("Bench press", "Overhead press", "Barbell row"), RoutineEdits.move(upper, 1, 1))
        assertEquals(upper, RoutineEdits.move(upper, 0, -1))
        assertEquals(upper, RoutineEdits.move(upper, 2, 1))
    }

    @Test
    fun withEdits_usesTheSavedList_andMarksItEdited() {
        val routine = WorkoutTemplate("upper", "Upper", upper)

        val edited = routine.withEdits(mapOf("upper" to listOf("Pull-up")))
        assertEquals(listOf("Pull-up"), edited.exercises)
        assertTrue(edited.isEdited)

        assertSame(routine, routine.withEdits(mapOf("lower" to listOf("Squat"))))
        // Saved in the built-in order again: not edited.
        assertFalse(routine.withEdits(mapOf("upper" to upper)).isEdited)
    }
}

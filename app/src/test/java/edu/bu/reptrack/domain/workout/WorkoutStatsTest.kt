package edu.bu.reptrack.domain.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutStatsTest {

    private fun set(id: Long, exercise: String, weight: Float, reps: Int) =
        LoggedSet(id, sessionId = 1, exerciseName = exercise, setNumber = 1, weightKg = weight, reps = reps, loggedAt = id)

    @Test
    fun totalVolume_isWeightTimesRepsAddedUp() {
        val sets = listOf(set(1, "Squat", 80f, 5), set(2, "Squat", 80f, 5), set(3, "Plank", 0f, 1))
        assertEquals(800f, totalVolumeKg(sets))
    }

    @Test
    fun duration_isNull_untilFinished() {
        assertNull(durationMinutes(WorkoutSession(1, null, "A", startedAt = 0, finishedAt = null)))
    }

    @Test
    fun duration_isWholeMinutes() {
        val session = WorkoutSession(1, null, "A", startedAt = 0, finishedAt = 52 * 60_000L + 30_000)
        assertEquals(52L, durationMinutes(session))
    }

    @Test
    fun exercisesToShow_keepsRoutineOrder_thenAddsOthers() {
        val sets = listOf(set(1, "Curl", 10f, 10), set(2, "Squat", 60f, 10))
        assertEquals(
            listOf("Squat", "Bench press", "Curl"),
            exercisesToShow(listOf("Squat", "Bench press"), sets),
        )
    }
}

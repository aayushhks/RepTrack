package edu.bu.reptrack.domain.workout

import java.time.ZoneOffset
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

    @Test
    fun weekStartsOnMondayAtMidnight() {
        val monday = 1_794_182_400_000L // Mon Nov 9, 2026, 00:00 UTC
        val wednesdayNoon = 1_794_398_400_000L
        val sundayNight = 1_794_783_600_000L // Sun Nov 15, 23:00 UTC, still the same week

        assertEquals(monday, startOfWeekMillis(wednesdayNoon, ZoneOffset.UTC))
        assertEquals(monday, startOfWeekMillis(monday, ZoneOffset.UTC))
        assertEquals(monday, startOfWeekMillis(sundayNight, ZoneOffset.UTC))
    }
}

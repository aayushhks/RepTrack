package edu.bu.reptrack.data.workout

import edu.bu.reptrack.domain.workout.LoggedSet
import edu.bu.reptrack.domain.workout.SessionSummary
import edu.bu.reptrack.domain.workout.WorkoutSession
import kotlinx.coroutines.flow.Flow

/** The only way screens read and write workouts. The real version uses Room. */
interface WorkoutRepository {
    /** Creates a new session and returns its id. */
    suspend fun startSession(routineId: String?, name: String): Long

    /** The session, updating live. Null if it does not exist. */
    fun observeSession(sessionId: Long): Flow<WorkoutSession?>

    /** Sets in a session, oldest first, updating live. */
    fun observeSets(sessionId: Long): Flow<List<LoggedSet>>

    /** Adds a set and returns its id. setNumber is worked out here. */
    suspend fun addSet(sessionId: Long, exerciseName: String, weightKg: Float, reps: Int): Long

    suspend fun updateSet(setId: Long, weightKg: Float, reps: Int)

    suspend fun deleteSet(setId: Long)

    /** Marks the session finished. A session with no sets is deleted instead. */
    suspend fun finishSession(sessionId: Long)

    /** Deletes a session and all its sets. */
    suspend fun deleteSession(sessionId: Long)

    /** Finished sessions with totals, newest first, updating live. */
    fun observeHistory(): Flow<List<SessionSummary>>
}

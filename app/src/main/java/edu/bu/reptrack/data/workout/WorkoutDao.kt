package edu.bu.reptrack.data.workout

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** One history row: a session plus its totals, worked out by the database. */
data class SessionSummaryRow(
    @Embedded val session: WorkoutSessionEntity,
    @ColumnInfo(name = "exercise_count") val exerciseCount: Int,
    @ColumnInfo(name = "set_count") val setCount: Int,
    @ColumnInfo(name = "total_volume_kg") val totalVolumeKg: Double,
)

/**
 * Raw database access. It is an abstract class, not an interface, so the multi-step
 * operations can be @Transaction functions: they either fully happen or not at all.
 */
@Dao
abstract class WorkoutDao {

    // ---- Sessions ----

    @Insert
    abstract suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    abstract fun observeSession(sessionId: Long): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    abstract suspend fun getSession(sessionId: Long): WorkoutSessionEntity?

    @Query("UPDATE workout_sessions SET finished_at = :finishedAt WHERE id = :sessionId")
    abstract suspend fun markFinished(sessionId: Long, finishedAt: Long)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    abstract suspend fun deleteSession(sessionId: Long)

    // ---- Sets ----

    @Insert
    abstract suspend fun insertSet(set: WorkoutSetEntity): Long

    @Query("SELECT * FROM workout_sets WHERE session_id = :sessionId ORDER BY logged_at ASC, id ASC")
    abstract fun observeSets(sessionId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE id = :setId")
    abstract suspend fun getSet(setId: Long): WorkoutSetEntity?

    @Query("SELECT COUNT(*) FROM workout_sets WHERE session_id = :sessionId")
    abstract suspend fun countSetsInSession(sessionId: Long): Int

    @Query("SELECT COUNT(*) FROM workout_sets WHERE session_id = :sessionId AND exercise_name = :exerciseName")
    abstract suspend fun countSetsOfExercise(sessionId: Long, exerciseName: String): Int

    @Query(
        "SELECT * FROM workout_sets WHERE session_id = :sessionId AND exercise_name = :exerciseName " +
            "ORDER BY set_number ASC, id ASC",
    )
    abstract suspend fun getSetsOfExercise(sessionId: Long, exerciseName: String): List<WorkoutSetEntity>

    @Query("UPDATE workout_sets SET weight_kg = :weightKg, reps = :reps WHERE id = :setId")
    abstract suspend fun updateSetValues(setId: Long, weightKg: Float, reps: Int)

    @Query("UPDATE workout_sets SET set_number = :setNumber WHERE id = :setId")
    abstract suspend fun updateSetNumber(setId: Long, setNumber: Int)

    @Query("DELETE FROM workout_sets WHERE id = :setId")
    abstract suspend fun deleteSetRow(setId: Long)

    // ---- History ----

    /**
     * Finished sessions, newest first, with totals. LEFT JOIN so a session is never lost,
     * COALESCE so a session with no sets would show 0 instead of null.
     */
    @Query(
        "SELECT s.id, s.routine_id, s.name, s.started_at, s.finished_at, " +
            "COUNT(DISTINCT t.exercise_name) AS exercise_count, " +
            "COUNT(t.id) AS set_count, " +
            "COALESCE(SUM(t.weight_kg * t.reps), 0.0) AS total_volume_kg " +
            "FROM workout_sessions s " +
            "LEFT JOIN workout_sets t ON t.session_id = s.id " +
            "WHERE s.finished_at IS NOT NULL " +
            "GROUP BY s.id " +
            "ORDER BY s.finished_at DESC, s.id DESC",
    )
    abstract fun observeHistory(): Flow<List<SessionSummaryRow>>

    // ---- Multi-step operations (each runs as one transaction) ----

    /** setNumber = 1 + how many sets of the same exercise are already in the session. */
    @Transaction
    open suspend fun addSet(
        sessionId: Long,
        exerciseName: String,
        weightKg: Float,
        reps: Int,
        loggedAt: Long,
    ): Long {
        val setNumber = countSetsOfExercise(sessionId, exerciseName) + 1
        return insertSet(
            WorkoutSetEntity(
                sessionId = sessionId,
                exerciseName = exerciseName,
                setNumber = setNumber,
                weightKg = weightKg,
                reps = reps,
                loggedAt = loggedAt,
            ),
        )
    }

    /** Deletes the set, then renumbers the rest of that exercise in that session as 1, 2, 3. */
    @Transaction
    open suspend fun deleteSetAndRenumber(setId: Long) {
        val set = getSet(setId) ?: return
        deleteSetRow(setId)
        getSetsOfExercise(set.sessionId, set.exerciseName).forEachIndexed { index, remaining ->
            val expected = index + 1
            if (remaining.setNumber != expected) updateSetNumber(remaining.id, expected)
        }
    }

    /**
     * Finishes the session. A session with no sets is deleted instead, so empty workouts never
     * reach history. A session that is already finished keeps its original finish time.
     */
    @Transaction
    open suspend fun finishOrDiscard(sessionId: Long, finishedAt: Long) {
        val session = getSession(sessionId) ?: return
        if (session.finishedAt != null) return
        if (countSetsInSession(sessionId) == 0) {
            deleteSession(sessionId)
        } else {
            markFinished(sessionId, finishedAt)
        }
    }
}

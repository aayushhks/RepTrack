package edu.bu.reptrack.domain.workout

/** One workout from start to finish. Times are epoch milliseconds. */
data class WorkoutSession(
    val id: Long,
    val routineId: String?,
    val name: String,
    val startedAt: Long,
    val finishedAt: Long?,
)

/** One logged set. The exercise name is copied in, so history stays correct if the catalog changes later. */
data class LoggedSet(
    val id: Long,
    val sessionId: Long,
    val exerciseName: String,
    val setNumber: Int,
    val weightKg: Float,
    val reps: Int,
    val loggedAt: Long,
)

/** A finished session with totals, for the history list. */
data class SessionSummary(
    val session: WorkoutSession,
    val exerciseCount: Int,
    val setCount: Int,
    val totalVolumeKg: Float,
)

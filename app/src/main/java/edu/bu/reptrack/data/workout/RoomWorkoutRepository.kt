package edu.bu.reptrack.data.workout

import edu.bu.reptrack.domain.workout.LoggedSet
import edu.bu.reptrack.domain.workout.SessionSummary
import edu.bu.reptrack.domain.workout.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [WorkoutRepository] backed by Room. It turns database rows into domain models, so no
 * Room type ever reaches a screen. [now] is the clock, so tests can control time.
 *
 * Input rules (weight >= 0, reps >= 1) are checked by the ViewModel, not here.
 */
class RoomWorkoutRepository(
    private val dao: WorkoutDao,
    private val now: () -> Long = System::currentTimeMillis,
) : WorkoutRepository {

    override suspend fun startSession(routineId: String?, name: String): Long =
        dao.insertSession(
            WorkoutSessionEntity(
                routineId = routineId,
                name = name,
                startedAt = now(),
                finishedAt = null,
            ),
        )

    override fun observeSession(sessionId: Long): Flow<WorkoutSession?> =
        dao.observeSession(sessionId).map { it?.toDomain() }

    override fun observeSets(sessionId: Long): Flow<List<LoggedSet>> =
        dao.observeSets(sessionId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun addSet(sessionId: Long, exerciseName: String, weightKg: Float, reps: Int): Long =
        dao.addSet(sessionId, exerciseName, weightKg, reps, loggedAt = now())

    override suspend fun updateSet(setId: Long, weightKg: Float, reps: Int) {
        dao.updateSetValues(setId, weightKg, reps)
    }

    override suspend fun deleteSet(setId: Long) {
        dao.deleteSetAndRenumber(setId)
    }

    override suspend fun finishSession(sessionId: Long) {
        dao.finishOrDiscard(sessionId, finishedAt = now())
    }

    override suspend fun deleteSession(sessionId: Long) {
        dao.deleteSession(sessionId)
    }

    override fun observeHistory(): Flow<List<SessionSummary>> =
        dao.observeHistory().map { rows -> rows.map { it.toDomain() } }
}

private fun WorkoutSessionEntity.toDomain() = WorkoutSession(
    id = id,
    routineId = routineId,
    name = name,
    startedAt = startedAt,
    finishedAt = finishedAt,
)

private fun WorkoutSetEntity.toDomain() = LoggedSet(
    id = id,
    sessionId = sessionId,
    exerciseName = exerciseName,
    setNumber = setNumber,
    weightKg = weightKg,
    reps = reps,
    loggedAt = loggedAt,
)

private fun SessionSummaryRow.toDomain() = SessionSummary(
    session = session.toDomain(),
    exerciseCount = exerciseCount,
    setCount = setCount,
    totalVolumeKg = totalVolumeKg.toFloat(),
)

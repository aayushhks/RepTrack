package edu.bu.reptrack.data.run

import edu.bu.reptrack.domain.run.RunSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [RunRepository] backed by Room. */
class RoomRunRepository(private val dao: RunDao) : RunRepository {

    override suspend fun saveRun(run: RunSummary): Long = dao.insert(
        RunEntity(
            startedAt = run.startedAt,
            finishedAt = run.finishedAt,
            distanceM = run.distanceM,
            durationMs = run.durationMs,
            calories = run.calories,
        ),
    )

    override fun observeRuns(): Flow<List<RunSummary>> =
        dao.observeRuns().map { rows -> rows.map { it.toDomain() } }
}

private fun RunEntity.toDomain() = RunSummary(
    id = id,
    startedAt = startedAt,
    finishedAt = finishedAt,
    distanceM = distanceM,
    durationMs = durationMs,
    calories = calories,
)

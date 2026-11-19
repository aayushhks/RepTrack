package edu.bu.reptrack.data.run

import edu.bu.reptrack.domain.run.PolylineCodec
import edu.bu.reptrack.domain.run.RoutePoint
import edu.bu.reptrack.domain.run.RunDetail
import edu.bu.reptrack.domain.run.RunSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [RunRepository] backed by Room. Routes are stored as encoded polylines next to the run. */
class RoomRunRepository(private val dao: RunDao) : RunRepository {

    override suspend fun saveRun(run: RunSummary, route: List<RoutePoint>): Long = dao.insertWithRoute(
        run = RunEntity(
            startedAt = run.startedAt,
            finishedAt = run.finishedAt,
            distanceM = run.distanceM,
            durationMs = run.durationMs,
            calories = run.calories,
        ),
        // One point is not a line, so it is not worth a row.
        polyline = if (route.size >= 2) PolylineCodec.encode(route) else null,
    )

    override fun observeRuns(): Flow<List<RunSummary>> =
        dao.observeRuns().map { rows -> rows.map { it.toDomain() } }

    override fun observeRun(runId: Long): Flow<RunDetail?> = dao.observeRun(runId).map { row ->
        row?.let {
            RunDetail(
                summary = it.run.toDomain(),
                route = it.route?.polyline?.let(PolylineCodec::decode).orEmpty(),
            )
        }
    }
}

private fun RunEntity.toDomain() = RunSummary(
    id = id,
    startedAt = startedAt,
    finishedAt = finishedAt,
    distanceM = distanceM,
    durationMs = durationMs,
    calories = calories,
)

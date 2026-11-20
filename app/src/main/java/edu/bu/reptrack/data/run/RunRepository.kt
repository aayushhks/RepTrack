package edu.bu.reptrack.data.run

import edu.bu.reptrack.domain.run.RoutePoint
import edu.bu.reptrack.domain.run.RunDetail
import edu.bu.reptrack.domain.run.RunSummary
import kotlinx.coroutines.flow.Flow

/** Saved runs. ViewModels only see this interface, so tests can pass a fake. */
interface RunRepository {
    /** Saves a finished run with the path it took, and returns its new id. */
    suspend fun saveRun(run: RunSummary, route: List<RoutePoint> = emptyList()): Long

    /** Finished runs, newest first. Totals only, so the list stays light. */
    fun observeRuns(): Flow<List<RunSummary>>

    /** One run with its route, or null if it does not exist (for example it was deleted). */
    fun observeRun(runId: Long): Flow<RunDetail?>
}

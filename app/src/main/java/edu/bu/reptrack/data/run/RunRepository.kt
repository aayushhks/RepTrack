package edu.bu.reptrack.data.run

import edu.bu.reptrack.domain.run.RunSummary
import kotlinx.coroutines.flow.Flow

/** Saved runs. ViewModels only see this interface, so tests can pass a fake. */
interface RunRepository {
    /** Saves a finished run and returns its new id. */
    suspend fun saveRun(run: RunSummary): Long

    /** Finished runs, newest first. */
    fun observeRuns(): Flow<List<RunSummary>>
}

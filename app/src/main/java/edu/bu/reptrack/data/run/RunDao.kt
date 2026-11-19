package edu.bu.reptrack.data.run

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Raw database access for runs. An abstract class, not an interface, so saving a run and its
 * route can be one @Transaction: both are saved, or neither.
 */
@Dao
abstract class RunDao {
    @Insert
    abstract suspend fun insert(run: RunEntity): Long

    @Insert
    abstract suspend fun insertRoute(route: RunRouteEntity)

    /** Saves the run, then its route if it has one, and returns the run's new id. */
    @Transaction
    open suspend fun insertWithRoute(run: RunEntity, polyline: String?): Long {
        val id = insert(run)
        if (polyline != null) insertRoute(RunRouteEntity(runId = id, polyline = polyline))
        return id
    }

    /** Newest first, live: the list updates by itself when a run is saved. Totals only, no routes. */
    @Query("SELECT * FROM runs ORDER BY finished_at DESC, id DESC")
    abstract fun observeRuns(): Flow<List<RunEntity>>

    /** One run with its route. Emits null if there is no run with that id. */
    @Transaction
    @Query("SELECT * FROM runs WHERE id = :runId")
    abstract fun observeRun(runId: Long): Flow<RunWithRoute?>
}

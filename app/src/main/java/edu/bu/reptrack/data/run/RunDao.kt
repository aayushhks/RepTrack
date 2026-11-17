package edu.bu.reptrack.data.run

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Raw database access for runs. */
@Dao
interface RunDao {
    @Insert
    suspend fun insert(run: RunEntity): Long

    /** Newest first, live: the list updates by itself when a run is saved. */
    @Query("SELECT * FROM runs ORDER BY finished_at DESC, id DESC")
    fun observeRuns(): Flow<List<RunEntity>>
}

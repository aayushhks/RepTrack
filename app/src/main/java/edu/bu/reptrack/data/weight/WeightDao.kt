package edu.bu.reptrack.data.weight

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Raw database access for weigh-ins. */
@Dao
interface WeightDao {
    @Insert
    suspend fun insert(entry: WeightEntity): Long

    @Query("DELETE FROM weights WHERE id = :id")
    suspend fun delete(id: Long)

    /** Oldest first, the order the chart draws them. Live: updates by itself after a change. */
    @Query("SELECT * FROM weights ORDER BY logged_at ASC, id ASC")
    fun observeAll(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weights ORDER BY logged_at DESC, id DESC LIMIT 1")
    suspend fun latest(): WeightEntity?
}

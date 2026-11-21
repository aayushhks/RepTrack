package edu.bu.reptrack.data.exercise

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Raw database access for the cached exercise catalog. */
@Dao
interface ExerciseDao {
    /** Adds new exercises and updates the ones we already have. */
    @Upsert
    suspend fun upsertAll(rows: List<ExerciseEntity>)

    /** Live: updates by itself when a refresh saves new rows. */
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>
}

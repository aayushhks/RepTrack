package edu.bu.reptrack.data.plan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Raw database access for the user's changes to their plan. An abstract class for the transactions. */
@Dao
abstract class PlanDao {
    @Query("SELECT * FROM routine_exercises ORDER BY routine_id, position")
    abstract fun observeRoutineExercises(): Flow<List<RoutineExerciseEntity>>

    @Query("DELETE FROM routine_exercises WHERE routine_id = :routineId")
    abstract suspend fun deleteRoutine(routineId: String)

    @Insert
    abstract suspend fun insertRoutineExercises(rows: List<RoutineExerciseEntity>)

    /** Replaces the routine's whole list in one go, so it is never half saved. */
    @Transaction
    open suspend fun replaceRoutine(routineId: String, exercises: List<String>) {
        deleteRoutine(routineId)
        insertRoutineExercises(exercises.mapIndexed { position, name -> RoutineExerciseEntity(routineId, position, name) })
    }

    @Query("SELECT day_of_week FROM training_days ORDER BY day_of_week")
    abstract fun observeTrainingDays(): Flow<List<Int>>

    @Query("DELETE FROM training_days")
    abstract suspend fun clearTrainingDays()

    @Insert
    abstract suspend fun insertTrainingDays(days: List<TrainingDayEntity>)

    @Transaction
    open suspend fun replaceTrainingDays(days: List<Int>) {
        clearTrainingDays()
        insertTrainingDays(days.map(::TrainingDayEntity))
    }
}

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

    // ---- The user's own plan ----

    @Query("SELECT * FROM custom_routines ORDER BY position")
    abstract fun observeCustomRoutines(): Flow<List<CustomRoutineEntity>>

    @Query("SELECT * FROM plan_schedule ORDER BY day_of_week")
    abstract fun observeSchedule(): Flow<List<ScheduleDayEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM custom_routines")
    abstract suspend fun lastPosition(): Int

    @Insert
    abstract suspend fun insertCustomRoutines(rows: List<CustomRoutineEntity>)

    @Query("UPDATE custom_routines SET name = :name WHERE id = :id")
    abstract suspend fun renameCustomRoutine(id: String, name: String)

    @Query("DELETE FROM custom_routines WHERE id = :id")
    abstract suspend fun deleteCustomRoutineRow(id: String)

    @Query("DELETE FROM routine_exercises WHERE routine_id IN (SELECT id FROM custom_routines)")
    abstract suspend fun deleteCustomExercises()

    @Query("DELETE FROM custom_routines")
    abstract suspend fun deleteCustomRoutines()

    @Query("DELETE FROM plan_schedule")
    abstract suspend fun clearSchedule()

    @Insert
    abstract suspend fun insertSchedule(rows: List<ScheduleDayEntity>)

    /** Puts a new plan in place of the old one, routines, exercises and days together. */
    @Transaction
    open suspend fun replaceCustomPlan(
        routines: List<CustomRoutineEntity>,
        exercises: List<RoutineExerciseEntity>,
        schedule: List<ScheduleDayEntity>,
    ) {
        wipeCustomPlan()
        insertCustomRoutines(routines)
        insertRoutineExercises(exercises)
        insertSchedule(schedule)
    }

    /** Adds a routine after the others. */
    @Transaction
    open suspend fun addCustomRoutine(id: String, name: String, exercises: List<String>) {
        insertCustomRoutines(listOf(CustomRoutineEntity(id, name, lastPosition() + 1)))
        insertRoutineExercises(exercises.mapIndexed { position, exercise -> RoutineExerciseEntity(id, position, exercise) })
    }

    /** Deletes a routine and its exercises, and saves the week without it. */
    @Transaction
    open suspend fun removeCustomRoutine(id: String, schedule: List<ScheduleDayEntity>) {
        deleteRoutine(id)
        deleteCustomRoutineRow(id)
        clearSchedule()
        insertSchedule(schedule)
    }

    @Transaction
    open suspend fun replaceSchedule(schedule: List<ScheduleDayEntity>) {
        clearSchedule()
        insertSchedule(schedule)
    }

    /** Back to the suggested plan. Edits to the built-in routines stay. */
    @Transaction
    open suspend fun clearCustomPlan() {
        wipeCustomPlan()
    }

    /** Not a transaction itself, so the transactions above can share it. */
    private suspend fun wipeCustomPlan() {
        // Exercises first: they are found through the routines.
        deleteCustomExercises()
        deleteCustomRoutines()
        clearSchedule()
    }
}

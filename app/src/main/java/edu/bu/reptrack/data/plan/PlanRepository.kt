package edu.bu.reptrack.data.plan

import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

/**
 * The user's own changes to their plan: the exercises of routines they edited, and the days
 * they train. Everything else comes from the built-in plan. An interface, so tests can pass a fake.
 */
interface PlanRepository {
    /** The exercises of every routine the user changed, by routine id. Routines not in the map are unchanged. */
    fun observeRoutineEdits(): Flow<Map<String, List<String>>>

    /** Saves a routine's whole exercise list. */
    suspend fun setRoutineExercises(routineId: String, exercises: List<String>)

    /** Goes back to the built-in exercises. */
    suspend fun resetRoutine(routineId: String)

    /** The days the user picked. Empty: they did not pick any, so the plan's default days apply. */
    fun observeTrainingDays(): Flow<Set<DayOfWeek>>

    suspend fun setTrainingDays(days: Set<DayOfWeek>)
}

package edu.bu.reptrack.data.plan

import edu.bu.reptrack.domain.workout.CustomPlan
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

/** A routine to save on the user's own plan. */
data class NewRoutine(val name: String, val exercises: List<String> = emptyList())

/**
 * The user's own changes to their plan: the exercises of routines they edited, the days they
 * train, and their own plan if they made one. Everything else comes from the built-in plan. An
 * interface, so tests can pass a fake.
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

    /** The user's own plan. Empty when they follow the suggested plan. */
    fun observeCustomPlan(): Flow<CustomPlan>

    /**
     * Starts the user's own plan with [routines], replacing any they had. [schedule] gives each
     * training day the index of its routine in [routines]. Returns the new routines' ids, in order.
     */
    suspend fun startCustomPlan(routines: List<NewRoutine>, schedule: Map<DayOfWeek, Int>): List<String>

    /** Adds a routine to the user's own plan, after the others, on no day yet. Returns its id. */
    suspend fun addRoutine(routine: NewRoutine): String

    suspend fun renameRoutine(id: String, name: String)

    /** Deletes one of the user's own routines and its exercises. [schedule] is the week without it. */
    suspend fun deleteRoutine(id: String, schedule: Map<DayOfWeek, String>)

    /** Saves which routine goes on which day. Days not in [schedule] are days off. */
    suspend fun setSchedule(schedule: Map<DayOfWeek, String>)

    /** Back to the suggested plan: the user's own routines and their days are deleted. */
    suspend fun clearCustomPlan()
}

package edu.bu.reptrack.testing

import edu.bu.reptrack.data.plan.PlanRepository
import java.io.IOException
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [PlanRepository] for tests. */
class FakePlanRepository : PlanRepository {
    val edits = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val days = MutableStateFlow<Set<DayOfWeek>>(emptySet())

    /** Makes saving throw, like a full disk, to test the error state. */
    var failWrites = false

    override fun observeRoutineEdits(): Flow<Map<String, List<String>>> = edits

    override suspend fun setRoutineExercises(routineId: String, exercises: List<String>) {
        if (failWrites) throw IOException("disk full")
        edits.value = edits.value + (routineId to exercises)
    }

    override suspend fun resetRoutine(routineId: String) {
        if (failWrites) throw IOException("disk full")
        edits.value = edits.value - routineId
    }

    override fun observeTrainingDays(): Flow<Set<DayOfWeek>> = days

    override suspend fun setTrainingDays(days: Set<DayOfWeek>) {
        if (failWrites) throw IOException("disk full")
        this.days.value = days
    }
}

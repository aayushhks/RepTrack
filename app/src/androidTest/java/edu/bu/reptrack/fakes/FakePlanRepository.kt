package edu.bu.reptrack.fakes

import edu.bu.reptrack.data.plan.NewRoutine
import edu.bu.reptrack.data.plan.PlanRepository
import edu.bu.reptrack.domain.workout.CustomPlan
import edu.bu.reptrack.domain.workout.WorkoutTemplate
import java.io.IOException
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * In-memory [PlanRepository] for tests. Like the Room one, the user's own routines keep their
 * exercises with the edits, so the routine screen and the picker work on them the same way.
 */
class FakePlanRepository : PlanRepository {
    val edits = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val days = MutableStateFlow<Set<DayOfWeek>>(emptySet())

    /** The user's own routines: id to name, in order. */
    val ownRoutines = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val schedule = MutableStateFlow<Map<DayOfWeek, String>>(emptyMap())

    /** Makes saving throw, like a full disk, to test the error state. */
    var failWrites = false

    private var lastId = 0

    override fun observeRoutineEdits(): Flow<Map<String, List<String>>> = edits

    override suspend fun setRoutineExercises(routineId: String, exercises: List<String>) {
        check()
        edits.value = edits.value + (routineId to exercises)
    }

    override suspend fun resetRoutine(routineId: String) {
        check()
        edits.value = edits.value - routineId
    }

    override fun observeTrainingDays(): Flow<Set<DayOfWeek>> = days

    override suspend fun setTrainingDays(days: Set<DayOfWeek>) {
        check()
        this.days.value = days
    }

    override fun observeCustomPlan(): Flow<CustomPlan> = combine(ownRoutines, edits, schedule) { routines, exercises, days ->
        CustomPlan(routines.map { (id, name) -> WorkoutTemplate(id, name, exercises[id].orEmpty()) }, days)
    }

    override suspend fun startCustomPlan(routines: List<NewRoutine>, schedule: Map<DayOfWeek, Int>): List<String> {
        check()
        clearOwn()
        val ids = routines.map { "custom_${++lastId}" }
        edits.value = edits.value + routines.mapIndexed { index, routine -> ids[index] to routine.exercises }.filter { it.second.isNotEmpty() }
        ownRoutines.value = routines.mapIndexed { index, routine -> ids[index] to routine.name }
        this.schedule.value = schedule.mapValues { (_, index) -> ids[index] }
        return ids
    }

    override suspend fun addRoutine(routine: NewRoutine): String {
        check()
        val id = "custom_${++lastId}"
        if (routine.exercises.isNotEmpty()) edits.value = edits.value + (id to routine.exercises)
        ownRoutines.value = ownRoutines.value + (id to routine.name)
        return id
    }

    override suspend fun renameRoutine(id: String, name: String) {
        check()
        ownRoutines.value = ownRoutines.value.map { if (it.first == id) id to name else it }
    }

    override suspend fun deleteRoutine(id: String, schedule: Map<DayOfWeek, String>) {
        check()
        edits.value = edits.value - id
        ownRoutines.value = ownRoutines.value.filter { it.first != id }
        this.schedule.value = schedule
    }

    override suspend fun setSchedule(schedule: Map<DayOfWeek, String>) {
        check()
        this.schedule.value = schedule
    }

    override suspend fun clearCustomPlan() {
        check()
        clearOwn()
    }

    private fun clearOwn() {
        edits.value = edits.value - ownRoutines.value.map { it.first }.toSet()
        ownRoutines.value = emptyList()
        schedule.value = emptyMap()
    }

    private fun check() {
        if (failWrites) throw IOException("disk full")
    }
}

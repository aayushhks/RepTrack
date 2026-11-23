package edu.bu.reptrack.data.plan

import edu.bu.reptrack.domain.workout.CustomPlan
import edu.bu.reptrack.domain.workout.WorkoutTemplate
import java.time.DayOfWeek
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** [PlanRepository] backed by Room. [newId] makes the ids of the user's own routines. */
class RoomPlanRepository(
    private val dao: PlanDao,
    private val newId: () -> String = { "custom_" + UUID.randomUUID() },
) : PlanRepository {

    override fun observeRoutineEdits(): Flow<Map<String, List<String>>> =
        dao.observeRoutineExercises().map { rows ->
            rows.groupBy { it.routineId }.mapValues { (_, exercises) -> exercises.sortedBy { it.position }.map { it.name } }
        }

    override suspend fun setRoutineExercises(routineId: String, exercises: List<String>) {
        dao.replaceRoutine(routineId, exercises)
    }

    override suspend fun resetRoutine(routineId: String) {
        dao.deleteRoutine(routineId)
    }

    override fun observeTrainingDays(): Flow<Set<DayOfWeek>> =
        dao.observeTrainingDays().map { days -> days.filter { it in 1..7 }.map(DayOfWeek::of).toSet() }

    override suspend fun setTrainingDays(days: Set<DayOfWeek>) {
        dao.replaceTrainingDays(days.map { it.value }.sorted())
    }

    override fun observeCustomPlan(): Flow<CustomPlan> = combine(
        dao.observeCustomRoutines(),
        observeRoutineEdits(),
        dao.observeSchedule(),
    ) { routines, exercises, schedule ->
        CustomPlan(
            routines = routines.map { WorkoutTemplate(id = it.id, name = it.name, exercises = exercises[it.id].orEmpty()) },
            schedule = schedule.filter { it.dayOfWeek in 1..7 }.associate { DayOfWeek.of(it.dayOfWeek) to it.routineId },
        )
    }

    override suspend fun startCustomPlan(routines: List<NewRoutine>, schedule: Map<DayOfWeek, Int>): List<String> {
        val ids = routines.map { newId() }
        dao.replaceCustomPlan(
            routines = routines.mapIndexed { index, routine -> CustomRoutineEntity(ids[index], routine.name, index) },
            exercises = routines.flatMapIndexed { index, routine ->
                routine.exercises.mapIndexed { position, name -> RoutineExerciseEntity(ids[index], position, name) }
            },
            schedule = schedule.mapNotNull { (day, index) -> ids.getOrNull(index)?.let { ScheduleDayEntity(day.value, it) } },
        )
        return ids
    }

    override suspend fun addRoutine(routine: NewRoutine): String =
        newId().also { id -> dao.addCustomRoutine(id, routine.name, routine.exercises) }

    override suspend fun renameRoutine(id: String, name: String) {
        dao.renameCustomRoutine(id, name)
    }

    override suspend fun deleteRoutine(id: String, schedule: Map<DayOfWeek, String>) {
        dao.removeCustomRoutine(id, schedule.toRows())
    }

    override suspend fun setSchedule(schedule: Map<DayOfWeek, String>) {
        dao.replaceSchedule(schedule.toRows())
    }

    override suspend fun clearCustomPlan() {
        dao.clearCustomPlan()
    }

    private fun Map<DayOfWeek, String>.toRows() = map { (day, id) -> ScheduleDayEntity(day.value, id) }
}

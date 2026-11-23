package edu.bu.reptrack.data.plan

import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [PlanRepository] backed by Room. */
class RoomPlanRepository(private val dao: PlanDao) : PlanRepository {

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
}

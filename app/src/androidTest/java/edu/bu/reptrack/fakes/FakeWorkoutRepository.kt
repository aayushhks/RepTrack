package edu.bu.reptrack.fakes

import edu.bu.reptrack.data.workout.WorkoutRepository
import edu.bu.reptrack.domain.workout.LoggedSet
import edu.bu.reptrack.domain.workout.SessionSummary
import edu.bu.reptrack.domain.workout.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * In-memory [WorkoutRepository] so UI tests never touch the real database. It follows the
 * same rules as the Room version (set numbering, renumbering after a delete, empty sessions
 * are discarded, history only lists finished sessions), so UI tests behave like the real app.
 */
class FakeWorkoutRepository(
    private val now: () -> Long = System::currentTimeMillis,
) : WorkoutRepository {

    private val sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    private val sets = MutableStateFlow<List<LoggedSet>>(emptyList())
    private var nextSessionId = 1L
    private var nextSetId = 1L

    /** Current contents, for asserting in tests. */
    val allSessions: List<WorkoutSession> get() = sessions.value
    val allSets: List<LoggedSet> get() = sets.value

    override suspend fun startSession(routineId: String?, name: String): Long {
        val id = nextSessionId++
        sessions.value = sessions.value + WorkoutSession(id, routineId, name, startedAt = now(), finishedAt = null)
        return id
    }

    override fun observeSession(sessionId: Long): Flow<WorkoutSession?> =
        sessions.map { list -> list.firstOrNull { it.id == sessionId } }

    override fun observeSets(sessionId: Long): Flow<List<LoggedSet>> =
        sets.map { list -> list.filter { it.sessionId == sessionId }.sortedWith(compareBy({ it.loggedAt }, { it.id })) }

    override suspend fun addSet(sessionId: Long, exerciseName: String, weightKg: Float, reps: Int): Long {
        val id = nextSetId++
        val number = sets.value.count { it.sessionId == sessionId && it.exerciseName == exerciseName } + 1
        sets.value = sets.value + LoggedSet(id, sessionId, exerciseName, number, weightKg, reps, loggedAt = now())
        return id
    }

    override suspend fun updateSet(setId: Long, weightKg: Float, reps: Int) {
        sets.value = sets.value.map { if (it.id == setId) it.copy(weightKg = weightKg, reps = reps) else it }
    }

    override suspend fun deleteSet(setId: Long) {
        val target = sets.value.firstOrNull { it.id == setId } ?: return
        val remaining = sets.value.filter { it.id != setId }
        val renumbered = remaining
            .filter { it.sessionId == target.sessionId && it.exerciseName == target.exerciseName }
            .sortedWith(compareBy({ it.setNumber }, { it.id }))
            .mapIndexed { index, set -> set.id to index + 1 }
            .toMap()
        sets.value = remaining.map { set -> renumbered[set.id]?.let { set.copy(setNumber = it) } ?: set }
    }

    override suspend fun finishSession(sessionId: Long) {
        val session = sessions.value.firstOrNull { it.id == sessionId } ?: return
        if (session.finishedAt != null) return
        if (sets.value.none { it.sessionId == sessionId }) {
            deleteSession(sessionId)
        } else {
            sessions.value = sessions.value.map { if (it.id == sessionId) it.copy(finishedAt = now()) else it }
        }
    }

    override suspend fun deleteSession(sessionId: Long) {
        sessions.value = sessions.value.filter { it.id != sessionId }
        sets.value = sets.value.filter { it.sessionId != sessionId }
    }

    override fun observeHistory(): Flow<List<SessionSummary>> =
        combine(sessions, sets) { sessionList, setList ->
            sessionList
                .filter { it.finishedAt != null }
                .sortedWith(compareByDescending<WorkoutSession> { it.finishedAt }.thenByDescending { it.id })
                .map { session ->
                    val own = setList.filter { it.sessionId == session.id }
                    SessionSummary(
                        session = session,
                        exerciseCount = own.map { it.exerciseName }.distinct().size,
                        setCount = own.size,
                        totalVolumeKg = own.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat(),
                    )
                }
        }
}

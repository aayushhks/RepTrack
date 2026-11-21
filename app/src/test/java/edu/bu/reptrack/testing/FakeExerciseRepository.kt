package edu.bu.reptrack.testing

import edu.bu.reptrack.data.exercise.ExerciseRepository
import edu.bu.reptrack.domain.exercise.Exercise
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [ExerciseRepository] for tests. A refresh "downloads" [online]. */
class FakeExerciseRepository(saved: List<Exercise> = emptyList()) : ExerciseRepository {
    private val exercises = MutableStateFlow(saved)

    /** What a successful refresh saves. */
    var online: List<Exercise> = emptyList()

    /** Makes refresh throw, like a phone without a connection. */
    var failRefresh = false

    /** When set, refresh waits for it, so a test can look at the loading state. */
    var gate: CompletableDeferred<Unit>? = null

    var refreshCount = 0
        private set

    /** The [force] value of the last refresh call. */
    var lastForce: Boolean? = null
        private set

    override fun observeExercises(): Flow<List<Exercise>> = exercises

    override fun observeExercise(id: Int): Flow<Exercise?> = exercises.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun refresh(force: Boolean) {
        refreshCount++
        lastForce = force
        gate?.await()
        if (failRefresh) throw IOException("no connection")
        exercises.value = online
    }
}

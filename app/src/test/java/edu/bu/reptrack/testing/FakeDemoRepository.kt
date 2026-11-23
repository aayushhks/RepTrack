package edu.bu.reptrack.testing

import edu.bu.reptrack.data.demo.DemoRepository
import edu.bu.reptrack.domain.exercise.ExerciseDemo
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred

/** In-memory [DemoRepository] for tests. */
class FakeDemoRepository(var all: List<ExerciseDemo> = emptyList()) : DemoRepository {
    /** Makes loading throw, like a phone without a connection the first time. */
    var fail = false

    /** When set, loading waits for it, so a test can look at the loading state. */
    var gate: CompletableDeferred<Unit>? = null

    var calls = 0
        private set

    override suspend fun demos(): List<ExerciseDemo> {
        calls++
        gate?.await()
        if (fail) throw IOException("no connection")
        return all
    }
}

fun demo(id: String, name: String = id.replace('_', ' '), equipment: String? = null) = ExerciseDemo(
    id = id,
    name = name,
    imageUrls = listOf("https://photos/$id/0.jpg", "https://photos/$id/1.jpg"),
    steps = listOf("Get ready.", "Do it."),
    primaryMuscles = listOf("chest"),
    equipment = equipment,
)

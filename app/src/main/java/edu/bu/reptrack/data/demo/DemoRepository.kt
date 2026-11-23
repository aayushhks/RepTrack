package edu.bu.reptrack.data.demo

import edu.bu.reptrack.domain.exercise.ExerciseDemo
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** How-to photos and steps for every exercise in Free Exercise DB. A fake replaces it in tests. */
interface DemoRepository {
    /** All of them: from memory, from the copy saved on the phone, or downloaded the first time. Throws if that download fails. */
    suspend fun demos(): List<ExerciseDemo>
}

/** Where Free Exercise DB lives. Pinned to one version, so the photos never move under us. */
object FreeExerciseDb {
    private const val VERSION = "f00c92c7dcf1216a928a52c3706c7ce8e2f71ed5"
    private const val BASE = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/$VERSION"
    const val EXERCISES_URL = "$BASE/dist/exercises.json"
    const val IMAGES_URL = "$BASE/exercises/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /** Reads the downloaded list. Throws if the text is not the list we expect. */
    fun parse(text: String): List<ExerciseDemo> =
        json.decodeFromString<List<FreeExerciseDto>>(text).map { dto ->
            ExerciseDemo(
                id = dto.id,
                name = dto.name,
                imageUrls = dto.images.map { IMAGES_URL + it },
                steps = dto.instructions.map { it.trim() }.filter { it.isNotEmpty() },
                primaryMuscles = dto.primaryMuscles,
                equipment = dto.equipment,
            )
        }
}

/** Downloads Free Exercise DB's one JSON file (about 1 MB). */
fun interface DemoApi {
    suspend fun download(): String
}

class KtorDemoApi(private val client: HttpClient) : DemoApi {
    override suspend fun download(): String = client.get(FreeExerciseDb.EXERCISES_URL).bodyAsText()
}

/**
 * [DemoRepository] that downloads the list once and keeps it in a file, so it works offline
 * after the first time; the photos themselves are cached by Coil. It changes so rarely that it
 * is not worth a Room table.
 */
class CachedDemoRepository(
    private val api: DemoApi,
    private val file: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : DemoRepository {

    private val lock = Mutex()
    private var loaded: List<ExerciseDemo>? = null

    override suspend fun demos(): List<ExerciseDemo> = lock.withLock {
        loaded ?: withContext(io) { readSaved() ?: download() }.also { loaded = it }
    }

    /** The saved copy, or null if there is none or it is damaged (then it is deleted). */
    private fun readSaved(): List<ExerciseDemo>? {
        if (!file.exists()) return null
        return try {
            FreeExerciseDb.parse(file.readText())
        } catch (damaged: Exception) {
            file.delete()
            null
        }
    }

    private suspend fun download(): List<ExerciseDemo> {
        val text = api.download()
        // Parse first, so a bad download is never saved. Then write to a temporary file and
        // rename it, so a half-written file is never read later.
        val demos = FreeExerciseDb.parse(text)
        file.parentFile?.mkdirs()
        val partial = File(file.path + ".part")
        partial.writeText(text)
        if (!partial.renameTo(file)) {
            file.writeText(text)
            partial.delete()
        }
        return demos
    }
}

@Serializable
private data class FreeExerciseDto(
    val id: String,
    val name: String,
    val equipment: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val images: List<String> = emptyList(),
)

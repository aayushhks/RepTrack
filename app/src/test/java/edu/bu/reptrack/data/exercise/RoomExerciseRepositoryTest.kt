package edu.bu.reptrack.data.exercise

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The real repository and Ktor client, with a fake server and an in-memory DAO. */
class RoomExerciseRepositoryTest {

    private class FakeExerciseDao : ExerciseDao {
        val rows = MutableStateFlow<List<ExerciseEntity>>(emptyList())

        override suspend fun upsertAll(rows: List<ExerciseEntity>) {
            this.rows.value = (this.rows.value.filter { old -> rows.none { it.id == old.id } } + rows)
                .sortedBy { it.name }
        }

        override fun observeAll(): Flow<List<ExerciseEntity>> = rows
    }

    private val page = """
        {
          "count": 2,
          "next": null,
          "results": [
            {
              "id": 9,
              "category": {"id": 10, "name": "Abs"},
              "muscles": [{"name": "Biceps femoris", "name_en": "Hamstrings"}],
              "equipment": [{"name": "Kettlebell"}],
              "images": [{"image": "https://x/big.webp", "is_main": true, "thumbnails": {"small": "https://x/small.png"}}],
              "translations": [
                {"name": "Balancier", "description": "<p>Debout</p>", "language": 12},
                {"name": "Kettlebell Swing", "description": "<p>Hinge &amp; swing.</p>", "language": 2}
              ],
              "something_new": 1
            },
            {
              "id": 10,
              "translations": [{"name": "Seulement en francais", "language": 12}]
            }
          ]
        }
    """.trimIndent()

    private fun repository(
        dao: ExerciseDao,
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = page,
        onRequest: () -> Unit = {},
        maxPages: Int = 4,
    ): RoomExerciseRepository {
        val engine = MockEngine {
            onRequest()
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return RoomExerciseRepository(dao, WgerApi(wgerHttpClient(engine), "http://wger.test"), maxPages = maxPages)
    }

    @Test
    fun refresh_savesTheEnglishExercises_andSkipsTheOthers() = runTest {
        val dao = FakeExerciseDao()
        val repository = repository(dao)

        repository.refresh()

        val saved = repository.observeExercises().first()
        assertEquals(1, saved.size)
        val exercise = saved.single()
        assertEquals("Kettlebell Swing", exercise.name)
        assertEquals("Hinge & swing.", exercise.description)
        assertEquals("Abs", exercise.category)
        assertEquals(listOf("Hamstrings"), exercise.muscles)
        assertEquals(listOf("Kettlebell"), exercise.equipment)
        assertEquals("https://x/small.png", exercise.imageUrl)
    }

    @Test
    fun refresh_stopsAfterTheLastPage() = runTest {
        var requests = 0
        repository(FakeExerciseDao(), onRequest = { requests++ }).refresh()
        assertEquals(1, requests)
    }

    @Test
    fun refresh_neverAsksForMoreThanMaxPages() = runTest {
        var requests = 0
        val more = page.replace("\"next\": null", "\"next\": \"http://wger.test/next\"")
        repository(FakeExerciseDao(), body = more, onRequest = { requests++ }, maxPages = 3).refresh()
        assertEquals(3, requests)
    }

    @Test
    fun serverError_throws_andKeepsWhatWasSaved() = runTest {
        val dao = FakeExerciseDao()
        repository(dao).refresh()

        try {
            repository(dao, status = HttpStatusCode.InternalServerError, body = "{}").refresh()
            fail("refresh should throw on a server error")
        } catch (expected: Exception) {
            assertTrue(dao.rows.value.size == 1)
        }
    }
}

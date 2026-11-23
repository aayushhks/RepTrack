package edu.bu.reptrack.data.exercise

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

        override fun observeById(id: Int): Flow<ExerciseEntity?> = rows.map { list -> list.firstOrNull { it.id == id } }

        override suspend fun count(): Int = rows.value.size
    }

    private class FakeStamp(var last: Long? = null) : RefreshStamp {
        override suspend fun lastRefresh(): Long? = last

        override suspend fun setLastRefresh(millis: Long) {
            last = millis
        }
    }

    private var clock = 1_000_000L

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
        stamp: RefreshStamp = FakeStamp(),
    ): RoomExerciseRepository {
        val engine = MockEngine {
            onRequest()
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return RoomExerciseRepository(
            dao = dao,
            api = WgerApi(wgerHttpClient(engine), "http://wger.test"),
            stamp = stamp,
            now = { clock },
            maxPages = maxPages,
        )
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
    fun exerciseWithoutEquipment_hasAnEmptyEquipmentList() = runTest {
        val dao = FakeExerciseDao()
        val noEquipment = """
            {"next": null, "results": [{"id": 5, "translations": [{"name": "Push-up", "language": 2}]}]}
        """.trimIndent()
        repository(dao, body = noEquipment).refresh()

        val exercise = dao.rows.value.single()
        assertEquals("", exercise.equipment)
        assertEquals(null, exercise.imageUrl)
    }

    @Test
    fun refresh_skipsTheDownloadWhenTheSavedCopyIsLessThanADayOld() = runTest {
        val dao = FakeExerciseDao()
        val stamp = FakeStamp()
        var requests = 0
        val repository = repository(dao, onRequest = { requests++ }, stamp = stamp)
        repository.refresh()
        assertEquals(1, requests)

        clock += 60 * 60 * 1000L
        repository.refresh()
        assertEquals(1, requests)

        clock += 24 * 60 * 60 * 1000L
        repository.refresh()
        assertEquals(2, requests)
    }

    @Test
    fun refresh_withForce_downloadsAnyway() = runTest {
        var requests = 0
        val repository = repository(FakeExerciseDao(), onRequest = { requests++ })
        repository.refresh()

        repository.refresh(force = true)

        assertEquals(2, requests)
    }

    @Test
    fun failedRefresh_doesNotMarkTheCopyAsFresh() = runTest {
        val stamp = FakeStamp()
        try {
            repository(FakeExerciseDao(), status = HttpStatusCode.InternalServerError, body = "{}", stamp = stamp).refresh()
        } catch (expected: Exception) {
        }
        assertEquals(null, stamp.last)
    }

    @Test
    fun observeExercise_findsOneById() = runTest {
        val repository = repository(FakeExerciseDao())
        repository.refresh()

        assertEquals("Kettlebell Swing", repository.observeExercise(9).first()?.name)
        assertEquals(null, repository.observeExercise(404).first())
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

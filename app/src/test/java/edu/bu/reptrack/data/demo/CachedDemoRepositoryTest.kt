package edu.bu.reptrack.data.demo

import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CachedDemoRepositoryTest {

    private val json = """
        [
          {"id": "Barbell_Squat", "name": "Barbell Squat", "equipment": "barbell", "primaryMuscles": ["quadriceps"],
           "instructions": ["Stand under the bar. ", "", "Squat down."], "images": ["Barbell_Squat/0.jpg", "Barbell_Squat/1.jpg"],
           "level": "beginner"},
          {"id": "Plank", "name": "Plank", "equipment": null, "primaryMuscles": ["abdominals"], "instructions": [], "images": []}
        ]
    """.trimIndent()

    private val folder: File = Files.createTempDirectory("demos").toFile()
    private val file = File(folder, "free-exercise-db.json")

    private class CountingApi(var text: String) : DemoApi {
        var downloads = 0
        var fail = false

        override suspend fun download(): String {
            downloads++
            if (fail) throw IOException("no connection")
            return text
        }
    }

    @Test
    fun parse_buildsPhotoLinks_andCleansTheSteps() {
        val squat = FreeExerciseDb.parse(json).first()

        assertEquals(listOf(FreeExerciseDb.IMAGES_URL + "Barbell_Squat/0.jpg", FreeExerciseDb.IMAGES_URL + "Barbell_Squat/1.jpg"), squat.imageUrls)
        assertEquals(listOf("Stand under the bar.", "Squat down."), squat.steps)
        assertEquals("barbell", squat.equipment)
    }

    @Test
    fun downloadsOnce_thenUsesTheSavedCopy() = runTest {
        val api = CountingApi(json)
        val repository = CachedDemoRepository(api, file, StandardTestDispatcher(testScheduler))

        assertEquals(2, repository.demos().size)
        assertEquals(2, repository.demos().size)
        assertEquals(1, api.downloads)
        assertTrue(file.exists())

        // A new start of the app reads the file instead of downloading again.
        val later = CachedDemoRepository(api, file, StandardTestDispatcher(testScheduler))
        assertEquals("Plank", later.demos()[1].id)
        assertEquals(1, api.downloads)
    }

    @Test
    fun damagedSavedCopy_isDownloadedAgain() = runTest {
        file.writeText("{ not the list")
        val api = CountingApi(json)

        val demos = CachedDemoRepository(api, file, StandardTestDispatcher(testScheduler)).demos()

        assertEquals(2, demos.size)
        assertEquals(1, api.downloads)
    }

    @Test
    fun failedDownload_throws_andSavesNothing_soTheNextTryDownloadsAgain() = runTest {
        val api = CountingApi(json).apply { fail = true }
        val repository = CachedDemoRepository(api, file, StandardTestDispatcher(testScheduler))

        try {
            repository.demos()
            fail("expected the download to fail")
        } catch (expected: IOException) {
        }
        assertTrue(!file.exists())

        api.fail = false
        assertEquals(2, repository.demos().size)
    }

    @Test
    fun badDownload_isNotSaved() = runTest {
        val api = CountingApi("<html>rate limited</html>")
        val repository = CachedDemoRepository(api, file, StandardTestDispatcher(testScheduler))

        try {
            repository.demos()
            fail("expected bad text to fail")
        } catch (expected: Exception) {
        }
        assertTrue(!file.exists())
    }
}

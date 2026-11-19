package edu.bu.reptrack.data.run

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.data.db.RepTrackDatabase
import edu.bu.reptrack.domain.run.RunSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real Room code for runs against an in-memory database. */
@RunWith(AndroidJUnit4::class)
class RoomRunRepositoryTest {

    private lateinit var database: RepTrackDatabase
    private lateinit var repository: RoomRunRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RepTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomRunRepository(database.runDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun run(finishedAt: Long, distanceM: Double) = RunSummary(
        startedAt = finishedAt - 600_000,
        finishedAt = finishedAt,
        distanceM = distanceM,
        durationMs = 600_000,
        calories = 90,
    )

    @Test
    fun noRuns_isAnEmptyList() = runTest {
        assertTrue(repository.observeRuns().first().isEmpty())
    }

    @Test
    fun savedRun_comesBackWithItsTotals() = runTest {
        val id = repository.saveRun(run(finishedAt = 1_000_000, distanceM = 1_850.5))

        val saved = repository.observeRuns().first().single()
        assertEquals(id, saved.id)
        assertEquals(1_850.5, saved.distanceM, 0.0)
        assertEquals(600_000L, saved.durationMs)
        assertEquals(90, saved.calories)
    }

    @Test
    fun runs_areNewestFirst() = runTest {
        repository.saveRun(run(finishedAt = 1_000_000, distanceM = 1_000.0))
        repository.saveRun(run(finishedAt = 3_000_000, distanceM = 3_000.0))
        repository.saveRun(run(finishedAt = 2_000_000, distanceM = 2_000.0))

        val distances = repository.observeRuns().first().map { it.distanceM }
        assertEquals(listOf(3_000.0, 2_000.0, 1_000.0), distances)
    }
}

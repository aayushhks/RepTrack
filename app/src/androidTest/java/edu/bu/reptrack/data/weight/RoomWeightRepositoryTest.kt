package edu.bu.reptrack.data.weight

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.data.db.RepTrackDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real Room code for weigh-ins against an in-memory database. */
@RunWith(AndroidJUnit4::class)
class RoomWeightRepositoryTest {

    private lateinit var database: RepTrackDatabase
    private lateinit var repository: RoomWeightRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RepTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomWeightRepository(database.weightDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun noWeighIns_isAnEmptyList_andNoLatest() = runTest {
        assertTrue(repository.observeWeights().first().isEmpty())
        assertNull(repository.latestWeight())
    }

    @Test
    fun weighIns_comeBackOldestFirst_whateverOrderTheyWereSaved() = runTest {
        repository.addWeight(82f, loggedAt = 3_000)
        repository.addWeight(84f, loggedAt = 1_000)
        repository.addWeight(83f, loggedAt = 2_000)

        assertEquals(listOf(84f, 83f, 82f), repository.observeWeights().first().map { it.weightKg })
        assertEquals(82f, repository.latestWeight()!!.weightKg)
    }

    @Test
    fun delete_removesOnlyThatWeighIn() = runTest {
        val keep = repository.addWeight(84f, loggedAt = 1_000)
        val typo = repository.addWeight(48f, loggedAt = 2_000)

        repository.deleteWeight(typo)

        assertEquals(listOf(keep), repository.observeWeights().first().map { it.id })
        assertEquals(84f, repository.latestWeight()!!.weightKg)
    }
}

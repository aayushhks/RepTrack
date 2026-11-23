package edu.bu.reptrack.data.plan

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.data.db.RepTrackDatabase
import java.time.DayOfWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real Room code for the plan against an in-memory database. */
@RunWith(AndroidJUnit4::class)
class RoomPlanRepositoryTest {

    private lateinit var database: RepTrackDatabase
    private lateinit var repository: RoomPlanRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RepTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomPlanRepository(database.planDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun editedRoutine_comesBackInOrder_andSavingAgainReplacesIt() = runTest {
        repository.setRoutineExercises("upper", listOf("Pull-up", "Bench press", "Dips"))
        repository.setRoutineExercises("lower", listOf("Squat"))
        repository.setRoutineExercises("upper", listOf("Dips", "Pull-up"))

        val edits = repository.observeRoutineEdits().first()
        assertEquals(listOf("Dips", "Pull-up"), edits["upper"])
        assertEquals(listOf("Squat"), edits["lower"])
    }

    @Test
    fun reset_removesOnlyThatRoutine() = runTest {
        repository.setRoutineExercises("upper", listOf("Pull-up"))
        repository.setRoutineExercises("lower", listOf("Squat"))

        repository.resetRoutine("upper")

        assertEquals(setOf("lower"), repository.observeRoutineEdits().first().keys)
    }

    @Test
    fun trainingDays_roundTrip_andEmptyMeansDefault() = runTest {
        assertTrue(repository.observeTrainingDays().first().isEmpty())

        repository.setTrainingDays(setOf(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY))
        assertEquals(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY), repository.observeTrainingDays().first())

        repository.setTrainingDays(emptySet())
        assertTrue(repository.observeTrainingDays().first().isEmpty())
    }
}

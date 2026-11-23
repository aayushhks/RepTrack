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

    // ---- The user's own plan ----

    @Test
    fun ownPlan_startsWithItsRoutinesExercisesAndDays() = runTest {
        val ids = repository.startCustomPlan(
            routines = listOf(NewRoutine("Push", listOf("Bench press", "Dips")), NewRoutine("Legs")),
            schedule = mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.WEDNESDAY to 1, DayOfWeek.FRIDAY to 0),
        )

        val plan = repository.observeCustomPlan().first()
        assertEquals(listOf("Push", "Legs"), plan.routines.map { it.name })
        assertEquals(listOf("Bench press", "Dips"), plan.routines[0].exercises)
        assertTrue(plan.routines[1].exercises.isEmpty())
        assertEquals(mapOf(DayOfWeek.MONDAY to ids[0], DayOfWeek.WEDNESDAY to ids[1], DayOfWeek.FRIDAY to ids[0]), plan.schedule)
    }

    @Test
    fun ownPlan_addRenameAndDelete() = runTest {
        val (push) = repository.startCustomPlan(listOf(NewRoutine("Push", listOf("Bench press"))), mapOf(DayOfWeek.MONDAY to 0))
        val pull = repository.addRoutine(NewRoutine("Pull", listOf("Pull-up")))
        repository.renameRoutine(pull, "Pull and core")

        var plan = repository.observeCustomPlan().first()
        assertEquals(listOf("Push", "Pull and core"), plan.routines.map { it.name })
        assertEquals(listOf("Pull-up"), plan.routines[1].exercises)

        repository.deleteRoutine(push, mapOf(DayOfWeek.MONDAY to pull))

        plan = repository.observeCustomPlan().first()
        assertEquals(listOf(pull), plan.routines.map { it.id })
        assertEquals(mapOf(DayOfWeek.MONDAY to pull), plan.schedule)
        assertTrue(push !in repository.observeRoutineEdits().first())
    }

    @Test
    fun ownPlan_clearing_keepsTheEditsToBuiltInRoutines() = runTest {
        repository.setRoutineExercises("upper", listOf("Pull-up"))
        repository.startCustomPlan(listOf(NewRoutine("Push", listOf("Bench press"))), mapOf(DayOfWeek.MONDAY to 0))

        repository.clearCustomPlan()

        val plan = repository.observeCustomPlan().first()
        assertTrue(plan.routines.isEmpty())
        assertTrue(plan.schedule.isEmpty())
        assertEquals(setOf("upper"), repository.observeRoutineEdits().first().keys)
    }

    @Test
    fun ownPlan_startingAgain_replacesTheOldOne() = runTest {
        repository.startCustomPlan(listOf(NewRoutine("Old", listOf("Squat"))), mapOf(DayOfWeek.MONDAY to 0))
        val (fresh) = repository.startCustomPlan(listOf(NewRoutine("New")), mapOf(DayOfWeek.TUESDAY to 0))

        val plan = repository.observeCustomPlan().first()
        assertEquals(listOf(fresh), plan.routines.map { it.id })
        assertEquals(mapOf(DayOfWeek.TUESDAY to fresh), plan.schedule)
        assertTrue(repository.observeRoutineEdits().first().isEmpty())
    }

    @Test
    fun ownPlan_schedule_canBeChanged() = runTest {
        val (push) = repository.startCustomPlan(listOf(NewRoutine("Push")), mapOf(DayOfWeek.MONDAY to 0))

        repository.setSchedule(mapOf(DayOfWeek.SATURDAY to push, DayOfWeek.SUNDAY to push))

        assertEquals(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), repository.observeCustomPlan().first().schedule.keys)
    }
}

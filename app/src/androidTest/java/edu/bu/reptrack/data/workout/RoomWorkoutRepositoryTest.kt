package edu.bu.reptrack.data.workout

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.data.db.RepTrackDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real Room code against an in-memory database, so nothing is written to the phone. */
@RunWith(AndroidJUnit4::class)
class RoomWorkoutRepositoryTest {

    private lateinit var database: RepTrackDatabase
    private lateinit var repository: RoomWorkoutRepository

    /** The test clock. Tests move it forward by hand. */
    private var time = 1_000L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RepTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomWorkoutRepository(database.workoutDao()) { time }
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** Adds a set one millisecond after the last thing, so "oldest first" is well defined. */
    private suspend fun addSet(sessionId: Long, exercise: String, weight: Float, reps: Int): Long {
        time += 1
        return repository.addSet(sessionId, exercise, weight, reps)
    }

    @Test
    fun addingThreeSets_numbersThemOneTwoThree_inOrder() = runTest {
        val session = repository.startSession("full_body_a", "Full body A")
        addSet(session, "Squat", 60f, 10)
        addSet(session, "Squat", 70f, 8)
        addSet(session, "Squat", 80f, 5)

        val sets = repository.observeSets(session).first()

        assertEquals(listOf(1, 2, 3), sets.map { it.setNumber })
        assertEquals(listOf(60f, 70f, 80f), sets.map { it.weightKg })
        assertEquals(listOf(10, 8, 5), sets.map { it.reps })
    }

    @Test
    fun setsOfDifferentExercises_areNumberedSeparately() = runTest {
        val session = repository.startSession(null, "Mixed")
        addSet(session, "Squat", 60f, 10)
        addSet(session, "Bench press", 40f, 10)
        addSet(session, "Squat", 70f, 8)
        addSet(session, "Bench press", 45f, 8)

        val sets = repository.observeSets(session).first()

        assertEquals(
            listOf("Squat" to 1, "Bench press" to 1, "Squat" to 2, "Bench press" to 2),
            sets.map { it.exerciseName to it.setNumber },
        )
    }

    @Test
    fun deletingTheMiddleSet_renumbersTheRest() = runTest {
        val session = repository.startSession(null, "Legs")
        addSet(session, "Squat", 60f, 10)
        val middle = addSet(session, "Squat", 70f, 8)
        addSet(session, "Squat", 80f, 5)

        repository.deleteSet(middle)
        val sets = repository.observeSets(session).first()

        assertEquals(listOf(1, 2), sets.map { it.setNumber })
        assertEquals(listOf(60f, 80f), sets.map { it.weightKg })
    }

    @Test
    fun deletingASet_doesNotRenumberOtherExercises() = runTest {
        val session = repository.startSession(null, "Mixed")
        val firstSquat = addSet(session, "Squat", 60f, 10)
        addSet(session, "Squat", 70f, 8)
        addSet(session, "Bench press", 40f, 10)
        addSet(session, "Bench press", 45f, 8)

        repository.deleteSet(firstSquat)
        val sets = repository.observeSets(session).first()

        assertEquals(listOf(1), sets.filter { it.exerciseName == "Squat" }.map { it.setNumber })
        assertEquals(listOf(1, 2), sets.filter { it.exerciseName == "Bench press" }.map { it.setNumber })
    }

    @Test
    fun updatingASet_changesWeightAndReps() = runTest {
        val session = repository.startSession(null, "Legs")
        val set = addSet(session, "Squat", 60f, 10)

        repository.updateSet(set, weightKg = 62.5f, reps = 9)
        val updated = repository.observeSets(session).first().single()

        assertEquals(62.5f, updated.weightKg)
        assertEquals(9, updated.reps)
        assertEquals(1, updated.setNumber)
    }

    @Test
    fun finishingASessionWithNoSets_deletesIt() = runTest {
        val session = repository.startSession(null, "Empty")

        repository.finishSession(session)

        assertNull(repository.observeSession(session).first())
        assertTrue(repository.observeHistory().first().isEmpty())
    }

    @Test
    fun finishingASession_stampsTheFinishTime() = runTest {
        val session = repository.startSession(null, "Legs")
        addSet(session, "Squat", 60f, 10)
        time = 5_000L

        repository.finishSession(session)
        val finished = repository.observeSession(session).first()

        assertNotNull(finished)
        assertEquals(1_000L, finished!!.startedAt)
        assertEquals(5_000L, finished.finishedAt)
    }

    @Test
    fun history_showsOnlyFinishedSessions_newestFirst_withTotals() = runTest {
        // Older session: 80 kg x 5 twice, one exercise. Volume 800.
        val older = repository.startSession("a", "Older")
        addSet(older, "Squat", 80f, 5)
        addSet(older, "Squat", 80f, 5)
        time = 5_000L
        repository.finishSession(older)

        // Newer session: two exercises. Volume 60 x 10 + 100 x 3 = 900.
        val newer = repository.startSession("b", "Newer")
        addSet(newer, "Bench press", 60f, 10)
        addSet(newer, "Squat", 100f, 3)
        time = 9_000L
        repository.finishSession(newer)

        // Still in progress, so it must not show up.
        val inProgress = repository.startSession("c", "In progress")
        addSet(inProgress, "Deadlift", 100f, 5)

        val history = repository.observeHistory().first()

        assertEquals(listOf("Newer", "Older"), history.map { it.session.name })

        val newerSummary = history[0]
        assertEquals(2, newerSummary.exerciseCount)
        assertEquals(2, newerSummary.setCount)
        assertEquals(900f, newerSummary.totalVolumeKg)

        val olderSummary = history[1]
        assertEquals(1, olderSummary.exerciseCount)
        assertEquals(2, olderSummary.setCount)
        assertEquals(800f, olderSummary.totalVolumeKg)
    }

    @Test
    fun deletingASession_alsoDeletesItsSets() = runTest {
        val session = repository.startSession(null, "Legs")
        addSet(session, "Squat", 60f, 10)
        addSet(session, "Squat", 70f, 8)

        repository.deleteSession(session)

        assertNull(repository.observeSession(session).first())
        assertTrue(repository.observeSets(session).first().isEmpty())
    }

    @Test
    fun observingAMissingSession_givesNull() = runTest {
        assertNull(repository.observeSession(sessionId = 12345L).first())
    }
}

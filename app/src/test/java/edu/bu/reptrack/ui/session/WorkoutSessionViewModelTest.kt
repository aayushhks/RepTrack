package edu.bu.reptrack.ui.session

import androidx.lifecycle.SavedStateHandle
import edu.bu.reptrack.testing.FakeWorkoutRepository
import edu.bu.reptrack.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var clock = 1_000L
    private val repository = FakeWorkoutRepository(now = { clock })

    private fun newViewModel(extra: Map<String, Any?> = emptyMap()) =
        WorkoutSessionViewModel(repository, SavedStateHandle(mapOf("routineId" to "full_body_a") + extra))

    private fun WorkoutSessionViewModel.log(exercise: String, weight: String, reps: String) {
        onWeightChange(exercise, weight)
        onRepsChange(exercise, reps)
        addSet(exercise)
    }

    private fun WorkoutSessionViewModel.squat() = uiState.value.exercises.first { it.name == "Squat" }

    @Test
    fun opensWithTheRoutinesExercises_andNothingSaved() = runTest {
        val state = newViewModel().uiState.value

        assertEquals("Full body A", state.routineName)
        assertEquals(listOf("Squat", "Bench press", "Barbell row", "Plank"), state.exercises.map { it.name })
        assertFalse(state.canFinish)
        // Opening a routine alone must not create a session.
        assertTrue(repository.allSessions.isEmpty())
    }

    @Test
    fun loggingSets_createsOneSession_andKeepsTheTypedValues() = runTest {
        val viewModel = newViewModel()

        viewModel.log("Squat", "60", "10")
        viewModel.addSet("Squat")
        advanceUntilIdle()

        assertEquals(1, repository.allSessions.size)
        assertEquals("full_body_a", repository.allSessions.single().routineId)
        assertEquals(listOf(1, 2), viewModel.squat().sets.map { it.setNumber })
        assertEquals("60", viewModel.squat().weightInput)
        assertEquals("10", viewModel.squat().repsInput)
        assertEquals(2, viewModel.uiState.value.setCount)
        assertEquals(1200f, viewModel.uiState.value.totalVolumeKg)
    }

    @Test
    fun missingReps_showsAnError_andSavesNothing() = runTest {
        val viewModel = newViewModel()

        viewModel.onWeightChange("Squat", "60")
        viewModel.addSet("Squat")
        advanceUntilIdle()

        assertTrue(viewModel.squat().repsError)
        assertTrue(repository.allSessions.isEmpty())
    }

    @Test
    fun typingAgain_clearsTheError() = runTest {
        val viewModel = newViewModel()
        viewModel.addSet("Squat")

        viewModel.onRepsChange("Squat", "8")

        assertFalse(viewModel.squat().repsError)
    }

    @Test
    fun emptyWeight_isSavedAsBodyweight() = runTest {
        val viewModel = newViewModel()

        viewModel.log("Plank", "", "1")
        advanceUntilIdle()

        assertEquals(0f, repository.allSets.single().weightKg)
    }

    @Test
    fun editingASet_savesTheNewValues() = runTest {
        val viewModel = newViewModel()
        viewModel.log("Squat", "60", "10")
        advanceUntilIdle()

        viewModel.onEditSet(viewModel.squat().sets.single())
        assertEquals("60", viewModel.uiState.value.editing?.weightInput)
        viewModel.onEditRepsChange("12")
        viewModel.saveEdit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editing)
        assertEquals(12, repository.allSets.single().reps)
    }

    @Test
    fun badEdit_keepsTheDialogOpen_withAnError() = runTest {
        val viewModel = newViewModel()
        viewModel.log("Squat", "60", "10")
        advanceUntilIdle()

        viewModel.onEditSet(viewModel.squat().sets.single())
        viewModel.onEditRepsChange("")
        viewModel.saveEdit()

        assertTrue(viewModel.uiState.value.editing?.repsError == true)
        assertEquals(10, repository.allSets.single().reps)
    }

    @Test
    fun deletingFromTheDialog_removesTheSet_andRenumbers() = runTest {
        val viewModel = newViewModel()
        viewModel.log("Squat", "60", "10")
        viewModel.log("Squat", "70", "8")
        advanceUntilIdle()

        viewModel.onEditSet(viewModel.squat().sets.first())
        viewModel.deleteEditingSet()
        advanceUntilIdle()

        val remaining = viewModel.squat().sets.single()
        assertEquals(70f, remaining.weightKg)
        assertEquals(1, remaining.setNumber)
    }

    @Test
    fun finish_savesTheWorkout() = runTest {
        val viewModel = newViewModel()
        viewModel.log("Squat", "60", "10")
        advanceUntilIdle()
        clock = 5_000L

        viewModel.finish()
        advanceUntilIdle()

        assertEquals(SessionOutcome.FINISHED, viewModel.uiState.value.outcome)
        assertEquals(5_000L, repository.allSessions.single().finishedAt)
    }

    @Test
    fun finish_withNoSets_doesNothing() = runTest {
        val viewModel = newViewModel()

        viewModel.finish()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.outcome)
    }

    @Test
    fun leaving_withNoSets_doesNotAsk() = runTest {
        val viewModel = newViewModel()

        viewModel.requestLeave()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showLeaveDialog)
        assertEquals(SessionOutcome.DISCARDED, viewModel.uiState.value.outcome)
    }

    @Test
    fun leaving_withSets_asksFirst_andDiscardDeletesEverything() = runTest {
        val viewModel = newViewModel()
        viewModel.log("Squat", "60", "10")
        advanceUntilIdle()

        viewModel.requestLeave()
        assertTrue(viewModel.uiState.value.showLeaveDialog)
        assertNull(viewModel.uiState.value.outcome)

        viewModel.discard()
        advanceUntilIdle()

        assertEquals(SessionOutcome.DISCARDED, viewModel.uiState.value.outcome)
        assertTrue(repository.allSessions.isEmpty())
        assertTrue(repository.allSets.isEmpty())
    }

    @Test
    fun keepLogging_closesTheDialog_andKeepsTheSets() = runTest {
        val viewModel = newViewModel()
        viewModel.log("Squat", "60", "10")
        advanceUntilIdle()

        viewModel.requestLeave()
        viewModel.dismissLeave()

        assertFalse(viewModel.uiState.value.showLeaveDialog)
        assertEquals(1, repository.allSets.size)
    }

    @Test
    fun session_comesBackFromSavedState() = runTest {
        // As if Android killed the app mid-workout and restored the saved session id.
        val id = repository.startSession("full_body_a", "Full body A")
        repository.addSet(id, "Squat", 60f, 10)

        val viewModel = newViewModel(mapOf("session_id" to id))
        advanceUntilIdle()

        assertNotNull(viewModel.squat().sets.singleOrNull())
        assertTrue(viewModel.uiState.value.canFinish)
    }
}

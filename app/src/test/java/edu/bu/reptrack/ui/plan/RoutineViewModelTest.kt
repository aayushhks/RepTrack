package edu.bu.reptrack.ui.plan

import androidx.lifecycle.SavedStateHandle
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.testing.FakeExerciseRepository
import edu.bu.reptrack.testing.FakePlanRepository
import edu.bu.reptrack.testing.MainDispatcherRule
import edu.bu.reptrack.ui.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val plan = FakePlanRepository()
    private val bench = Exercise(73, "Bench Press", "", "Chest", listOf("Chest", "Triceps"), listOf("Barbell"), null)
    private val catalog = FakeExerciseRepository(saved = listOf(bench))

    // Upper: Bench press, Barbell row, Overhead press, Lat pulldown, Biceps curl.
    private fun TestScope.open(routineId: String = "upper"): RoutineViewModel {
        val viewModel = RoutineViewModel(plan, catalog, SavedStateHandle(mapOf(Routes.ARG_ROUTINE_ID to routineId)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun builtInRoutine_withCatalogMatches() = runTest {
        val state = open().uiState.value

        assertEquals("Upper body", state.name)
        assertEquals("Bench press", state.exercises.first().name)
        assertEquals(73, state.exercises.first().match?.id)
        assertNull(state.exercises[1].match)
        assertFalse(state.isEdited)
        assertFalse(state.catalogEmpty)
    }

    @Test
    fun remove_saves_andKeepsTheOthersInOrder() = runTest {
        val viewModel = open()

        viewModel.remove(1)

        assertEquals(listOf("Bench press", "Overhead press", "Lat pulldown", "Biceps curl"), plan.edits.value["upper"])
        assertTrue(viewModel.uiState.value.isEdited)
    }

    @Test
    fun moveUpAndDown() = runTest {
        val viewModel = open()

        viewModel.moveDown(0)
        assertEquals("Barbell row", viewModel.uiState.value.exercises[0].name)
        viewModel.moveUp(1)
        assertEquals("Bench press", viewModel.uiState.value.exercises[0].name)
        // Back in the built-in order: not edited any more.
        assertFalse(viewModel.uiState.value.isEdited)
    }

    @Test
    fun reset_bringsBackTheBuiltInList() = runTest {
        plan.edits.value = mapOf("upper" to listOf("Pull-up"))
        val viewModel = open()
        assertTrue(viewModel.uiState.value.isEdited)

        viewModel.reset()

        assertEquals(5, viewModel.uiState.value.exercises.size)
        assertFalse(viewModel.uiState.value.isEdited)
    }

    @Test
    fun theLastExercise_cannotBeRemoved() = runTest {
        plan.edits.value = mapOf("upper" to listOf("Pull-up"))
        val viewModel = open()

        assertFalse(viewModel.uiState.value.canRemove)
        viewModel.remove(0)
        assertEquals(listOf("Pull-up"), plan.edits.value["upper"])
    }

    @Test
    fun saveFailure_isShown() = runTest {
        val viewModel = open()
        plan.failWrites = true

        viewModel.remove(0)

        assertTrue(viewModel.uiState.value.saveFailed)
    }

    @Test
    fun unknownRoutine_saysSo() = runTest {
        assertTrue(open(routineId = "nope").uiState.value.notFound)
    }

    @Test
    fun ownRoutine_showsItsExercises_andSavesChanges() = runTest {
        plan.ownRoutines.value = listOf("custom_1" to "Push day")
        plan.edits.value = mapOf("custom_1" to listOf("Bench press", "Dips"))
        val viewModel = open("custom_1")

        assertEquals("Push day", viewModel.uiState.value.name)
        assertEquals(73, viewModel.uiState.value.exercises.first().match?.id)
        assertFalse(viewModel.uiState.value.isEdited)

        viewModel.moveDown(0)
        assertEquals(listOf("Dips", "Bench press"), plan.edits.value["custom_1"])
    }

    @Test
    fun newOwnRoutine_isEmpty_andCannotStartYet() = runTest {
        plan.ownRoutines.value = listOf("custom_1" to "Legs")
        val state = open("custom_1").uiState.value

        assertFalse(state.notFound)
        assertTrue(state.exercises.isEmpty())
        assertFalse(state.canStart)
    }
}

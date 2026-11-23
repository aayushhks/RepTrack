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
class ExercisePickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun exercise(id: Int, name: String, vararg muscles: String) =
        Exercise(id, name, "", "Chest", muscles.toList(), listOf("Barbell"), null)

    private val plan = FakePlanRepository()
    private val catalog = FakeExerciseRepository(
        saved = listOf(
            exercise(1, "Bench Press", "Chest", "Triceps"),
            exercise(2, "Incline Bench Press", "Chest"),
            exercise(3, "Dips", "Chest", "Triceps"),
            exercise(4, "Pull-Ups", "Lats"),
        ),
    )

    // Upper: Bench press, Barbell row, Overhead press, Lat pulldown, Biceps curl.
    private fun TestScope.open(replaceIndex: Int = -1): ExercisePickerViewModel {
        val savedState = SavedStateHandle(mapOf(Routes.ARG_ROUTINE_ID to "upper", Routes.ARG_REPLACE_INDEX to replaceIndex))
        val viewModel = ExercisePickerViewModel(plan, catalog, savedState)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun adding_listsTheCatalog_withoutWhatIsAlreadyInTheRoutine() = runTest {
        val state = open().uiState.value

        assertNull(state.replacing)
        assertEquals(listOf("Dips", "Incline Bench Press", "Pull-Ups"), state.results.map { it.name }.sorted())
    }

    @Test
    fun pick_addsAtTheEnd_andCloses() = runTest {
        val viewModel = open()

        viewModel.pick("Pull-Ups")

        assertEquals("Pull-Ups", plan.edits.value.getValue("upper").last())
        assertEquals(6, plan.edits.value.getValue("upper").size)
        assertTrue(viewModel.uiState.value.done)
    }

    @Test
    fun swapping_suggestsTheSameMusclesFirst_andKeepsThePlace() = runTest {
        val viewModel = open(replaceIndex = 0)

        val state = viewModel.uiState.value
        assertEquals("Bench press", state.replacing)
        assertEquals(listOf("Dips", "Incline Bench Press"), state.sameMuscles.map { it.name })
        assertFalse(state.results.any { it.name == "Dips" })

        viewModel.pick("Dips")

        val saved = plan.edits.value.getValue("upper")
        assertEquals("Dips", saved[0])
        assertEquals(listOf("Barbell row", "Overhead press", "Lat pulldown", "Biceps curl"), saved.drop(1))
    }

    @Test
    fun search_filters_andOffersTheTypedName() = runTest {
        val viewModel = open()

        viewModel.onQueryChange("incl")
        assertEquals(listOf("Incline Bench Press"), viewModel.uiState.value.results.map { it.name })
        assertEquals("incl", viewModel.uiState.value.customName)

        viewModel.onQueryChange("pull-ups")
        assertNull(viewModel.uiState.value.customName)
    }

    @Test
    fun ownExercise_canBeAdded_evenWithoutTheCatalog() = runTest {
        val viewModel = ExercisePickerViewModel(
            plan,
            FakeExerciseRepository(),
            SavedStateHandle(mapOf(Routes.ARG_ROUTINE_ID to "upper", Routes.ARG_REPLACE_INDEX to -1)),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertTrue(viewModel.uiState.value.catalogEmpty)

        viewModel.onQueryChange("Face pull")
        viewModel.pick(viewModel.uiState.value.customName!!)

        assertEquals("Face pull", plan.edits.value.getValue("upper").last())
    }

    @Test
    fun saveFailure_staysOpen_andSaysSo() = runTest {
        val viewModel = open()
        plan.failWrites = true

        viewModel.pick("Dips")

        assertTrue(viewModel.uiState.value.saveFailed)
        assertFalse(viewModel.uiState.value.done)
    }

    @Test
    fun addingToAnEmptyOwnRoutine() = runTest {
        plan.ownRoutines.value = listOf("custom_1" to "Push day")
        val savedState = SavedStateHandle(mapOf(Routes.ARG_ROUTINE_ID to "custom_1", Routes.ARG_REPLACE_INDEX to -1))
        val viewModel = ExercisePickerViewModel(plan, catalog, savedState)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals("Push day", viewModel.uiState.value.routineName)
        assertEquals(4, viewModel.uiState.value.results.size)

        viewModel.pick("Dips")

        assertEquals(listOf("Dips"), plan.edits.value["custom_1"])
        assertTrue(viewModel.uiState.value.done)
    }
}

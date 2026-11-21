package edu.bu.reptrack.ui.exercises

import androidx.lifecycle.SavedStateHandle
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.testing.FakeExerciseRepository
import edu.bu.reptrack.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
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
class ExercisesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun exercise(id: Int, name: String, category: String) =
        Exercise(id, name, "", category, emptyList(), emptyList(), null)

    private val catalog = listOf(
        exercise(1, "Bench Press", "Chest"),
        exercise(2, "Squat", "Legs"),
        exercise(3, "Leg Press", "Legs"),
    )

    // uiState only updates while someone collects it, like the screen does.
    private fun TestScope.startCollecting(viewModel: ExercisesViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun showsLoading_untilTheFirstDownloadEnds() = runTest {
        val repository = FakeExerciseRepository().apply {
            online = catalog
            gate = CompletableDeferred()
        }
        val viewModel = ExercisesViewModel(repository, SavedStateHandle())
        startCollecting(viewModel)
        assertTrue(viewModel.uiState.value.isLoading)

        repository.gate!!.complete(Unit)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(3, viewModel.uiState.value.exercises.size)
    }

    @Test
    fun offlineWithSavedExercises_keepsShowingThem() = runTest {
        val repository = FakeExerciseRepository(saved = catalog).apply { failRefresh = true }
        val viewModel = ExercisesViewModel(repository, SavedStateHandle())
        startCollecting(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.refreshFailed)
        assertTrue(state.hasSaved)
        assertEquals(3, state.exercises.size)
    }

    @Test
    fun offlineWithNothingSaved_failsUntilRetryWorks() = runTest {
        val repository = FakeExerciseRepository().apply {
            failRefresh = true
            online = catalog
        }
        val viewModel = ExercisesViewModel(repository, SavedStateHandle())
        startCollecting(viewModel)
        assertTrue(viewModel.uiState.value.refreshFailed)
        assertFalse(viewModel.uiState.value.hasSaved)

        repository.failRefresh = false
        viewModel.retry()

        assertFalse(viewModel.uiState.value.refreshFailed)
        assertEquals(3, viewModel.uiState.value.exercises.size)
        assertEquals(2, repository.refreshCount)
    }

    @Test
    fun searchAndCategory_filterTheList() = runTest {
        val viewModel = ExercisesViewModel(FakeExerciseRepository(catalog), SavedStateHandle())
        startCollecting(viewModel)

        viewModel.onCategoryClick("Legs")
        viewModel.onQueryChange("press")

        assertEquals(listOf(3), viewModel.uiState.value.exercises.map { it.id })
        assertEquals(listOf("Chest", "Legs"), viewModel.uiState.value.categories)
    }

    @Test
    fun tappingTheChosenCategoryAgain_showsAll() = runTest {
        val viewModel = ExercisesViewModel(FakeExerciseRepository(catalog), SavedStateHandle())
        startCollecting(viewModel)

        viewModel.onCategoryClick("Legs")
        viewModel.onCategoryClick("Legs")

        assertNull(viewModel.uiState.value.category)
        assertEquals(3, viewModel.uiState.value.exercises.size)
    }

    @Test
    fun searchText_isKeptInSavedState() = runTest {
        val savedState = SavedStateHandle()
        val viewModel = ExercisesViewModel(FakeExerciseRepository(catalog), savedState)

        viewModel.onQueryChange("squat")

        assertEquals("squat", savedState.get<String>("exercises_query"))
    }
}

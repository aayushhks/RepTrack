package edu.bu.reptrack.ui.exercises

import androidx.lifecycle.SavedStateHandle
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.testing.FakeExerciseRepository
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
class ExerciseDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val squat = Exercise(2, "Squat", "Stand tall.", "Legs", listOf("Quads"), emptyList(), null)

    private fun TestScope.startCollecting(viewModel: ExerciseDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun viewModel(id: Int) = ExerciseDetailViewModel(
        FakeExerciseRepository(saved = listOf(squat)),
        SavedStateHandle(mapOf(Routes.ARG_EXERCISE_ID to id)),
    )

    @Test
    fun startsLoading() {
        assertTrue(viewModel(2).uiState.value.isLoading)
    }

    @Test
    fun savedExercise_isShown() = runTest {
        val viewModel = viewModel(2)
        startCollecting(viewModel)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(squat, viewModel.uiState.value.exercise)
    }

    @Test
    fun unknownId_endsLoadingWithNoExercise() = runTest {
        val viewModel = viewModel(99)
        startCollecting(viewModel)

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.exercise)
    }
}

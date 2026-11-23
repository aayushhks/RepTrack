package edu.bu.reptrack.ui.howto

import androidx.lifecycle.SavedStateHandle
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.testing.FakeDemoRepository
import edu.bu.reptrack.testing.FakeExerciseRepository
import edu.bu.reptrack.testing.MainDispatcherRule
import edu.bu.reptrack.testing.demo
import edu.bu.reptrack.ui.navigation.Routes
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
class HowToViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val demos = FakeDemoRepository(listOf(demo("Barbell_Squat", "Barbell Squat", "barbell"), demo("Plank")))
    private val catalog = FakeExerciseRepository(saved = listOf(Exercise(2, "Squat", "", "Legs", listOf("Quads"), listOf("Barbell"), null)))

    private fun TestScope.open(name: String): HowToViewModel {
        val viewModel = HowToViewModel(demos, catalog, SavedStateHandle(mapOf(Routes.ARG_EXERCISE_NAME to name)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun loadsThePhotos_andTheCatalogDetails() = runTest {
        val state = open("Squat").uiState.value

        assertFalse(state.isLoading)
        assertEquals("Squat", state.name)
        assertEquals("Barbell_Squat", state.demo?.id)
        assertEquals(2, state.catalog?.id)
    }

    @Test
    fun whileLoading_saysSo() = runTest {
        demos.gate = CompletableDeferred()
        val viewModel = open("Plank")
        assertTrue(viewModel.uiState.value.isLoading)

        demos.gate?.complete(Unit)
        assertEquals("Plank", viewModel.uiState.value.demo?.id)
    }

    @Test
    fun noPhotos_forThisName() = runTest {
        val state = open("Hollow hold").uiState.value
        assertFalse(state.isLoading)
        assertNull(state.demo)
        assertFalse(state.loadFailed)
    }

    @Test
    fun failedDownload_offersTryAgain() = runTest {
        demos.fail = true
        val viewModel = open("Plank")
        assertTrue(viewModel.uiState.value.loadFailed)

        demos.fail = false
        viewModel.retry()

        assertFalse(viewModel.uiState.value.loadFailed)
        assertEquals("Plank", viewModel.uiState.value.demo?.id)
    }
}

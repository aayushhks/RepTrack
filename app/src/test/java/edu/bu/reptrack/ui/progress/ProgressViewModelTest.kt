package edu.bu.reptrack.ui.progress

import edu.bu.reptrack.testing.FakeWorkoutRepository
import edu.bu.reptrack.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var clock = 1_000L
    private val repository = FakeWorkoutRepository(now = { clock })

    // uiState only updates while someone collects it, like the screen does.
    private fun TestScope.startCollecting(viewModel: ProgressViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun startsLoading() {
        assertTrue(ProgressViewModel(repository).uiState.value.isLoading)
    }

    @Test
    fun noWorkouts_showsAnEmptyHistory() = runTest {
        val viewModel = ProgressViewModel(repository)
        startCollecting(viewModel)

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.history.isEmpty())
    }

    @Test
    fun finishedWorkout_showsUpByItself_withTotals() = runTest {
        val viewModel = ProgressViewModel(repository)
        startCollecting(viewModel)

        val id = repository.startSession("upper", "Upper body")
        repository.addSet(id, "Bench press", 60f, 10)
        repository.addSet(id, "Barbell row", 50f, 10)
        repository.finishSession(id)

        val summary = viewModel.uiState.value.history.single()
        assertEquals("Upper body", summary.session.name)
        assertEquals(2, summary.exerciseCount)
        assertEquals(1100f, summary.totalVolumeKg)
    }

    @Test
    fun unfinishedWorkout_isNotInHistory() = runTest {
        val viewModel = ProgressViewModel(repository)
        startCollecting(viewModel)

        val id = repository.startSession("upper", "Upper body")
        repository.addSet(id, "Bench press", 60f, 10)

        assertTrue(viewModel.uiState.value.history.isEmpty())
    }
}

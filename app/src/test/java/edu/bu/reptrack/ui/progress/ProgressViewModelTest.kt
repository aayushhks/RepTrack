package edu.bu.reptrack.ui.progress

import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.testing.FakeRunRepository
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
    private val runs = FakeRunRepository()

    // uiState only updates while someone collects it, like the screen does.
    private fun TestScope.startCollecting(viewModel: ProgressViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun startsLoading() {
        assertTrue(ProgressViewModel(repository, runs).uiState.value.isLoading)
    }

    @Test
    fun noWorkouts_showsAnEmptyHistory() = runTest {
        val viewModel = ProgressViewModel(repository, runs)
        startCollecting(viewModel)

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun finishedWorkout_showsUpByItself_withTotals() = runTest {
        val viewModel = ProgressViewModel(repository, runs)
        startCollecting(viewModel)

        val id = repository.startSession("upper", "Upper body")
        repository.addSet(id, "Bench press", 60f, 10)
        repository.addSet(id, "Barbell row", 50f, 10)
        repository.finishSession(id)

        val summary = (viewModel.uiState.value.items.single() as HistoryItem.Workout).summary
        assertEquals("Upper body", summary.session.name)
        assertEquals(2, summary.exerciseCount)
        assertEquals(1100f, summary.totalVolumeKg)
    }

    @Test
    fun unfinishedWorkout_isNotInHistory() = runTest {
        val viewModel = ProgressViewModel(repository, runs)
        startCollecting(viewModel)

        val id = repository.startSession("upper", "Upper body")
        repository.addSet(id, "Bench press", 60f, 10)

        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun savedRun_showsUpByItself_nextToWorkouts_newestFirst() = runTest {
        val viewModel = ProgressViewModel(repository, runs)
        startCollecting(viewModel)

        clock = 5_000L
        val id = repository.startSession("upper", "Upper body")
        repository.addSet(id, "Bench press", 60f, 10)
        repository.finishSession(id)
        runs.saveRun(RunSummary(startedAt = 6_000L, finishedAt = 9_000L, distanceM = 2_000.0, durationMs = 3_000L, calories = 150))

        val items = viewModel.uiState.value.items
        assertEquals(2, items.size)
        assertTrue(items[0] is HistoryItem.Run)
        assertTrue(items[1] is HistoryItem.Workout)
    }
}

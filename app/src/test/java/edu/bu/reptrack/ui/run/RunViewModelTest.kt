package edu.bu.reptrack.ui.run

import edu.bu.reptrack.data.run.RunRecorder
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.testing.FakeLocationTracker
import edu.bu.reptrack.testing.FakeProfileRepository
import edu.bu.reptrack.testing.FakeRunRepository
import edu.bu.reptrack.testing.FakeRunServiceController
import edu.bu.reptrack.testing.MainDispatcherRule
import edu.bu.reptrack.testing.pointNorth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RunViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var clock = 100_000L
    private val tracker = FakeLocationTracker()
    private val runs = FakeRunRepository()
    private val service = FakeRunServiceController()
    private val profiles = FakeProfileRepository()

    private lateinit var recorder: RunRecorder

    private fun TestScope.newViewModel(): RunViewModel {
        recorder = RunRecorder(tracker, backgroundScope + UnconfinedTestDispatcher(testScheduler)) { clock }
        val viewModel = RunViewModel(recorder, runs, service, profiles) { clock }
        // uiState only updates while someone collects it, like the screen does.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    /** Sends fixes 10 m apart, 3 seconds apart, starting at the current clock. */
    private fun runMeters(meters: Int) {
        for (i in 0..meters / 10) {
            tracker.send(pointNorth(i * 10.0, clock + i * 3_000L))
        }
    }

    @Test
    fun opens_readyToStart() = runTest {
        val viewModel = newViewModel()
        val state = viewModel.uiState.value
        assertFalse(state.isRunning)
        assertFalse(state.canFinish)
        assertEquals(0, service.starts)
    }

    @Test
    fun start_recordsAndStartsTheService_thenWaitsForGps() = runTest {
        val viewModel = newViewModel()
        viewModel.start()

        val state = viewModel.uiState.value
        assertTrue(state.isRunning)
        assertTrue(state.waitingForGps)
        assertEquals(1, service.starts)
        assertTrue(tracker.isListening)
    }

    @Test
    fun permissionDenied_showsTheCard_andNothingStarts() = runTest {
        val viewModel = newViewModel()
        viewModel.onPermissionResult(granted = false, approximateOnly = false)

        assertTrue(viewModel.uiState.value.permissionDenied)
        assertFalse(viewModel.uiState.value.isRunning)
        assertEquals(0, service.starts)
    }

    @Test
    fun approximateOnly_startsAnyway_andSaysSo() = runTest {
        val viewModel = newViewModel()
        viewModel.onPermissionResult(granted = true, approximateOnly = true)

        assertTrue(viewModel.uiState.value.isRunning)
        assertTrue(viewModel.uiState.value.approximateOnly)
    }

    @Test
    fun fixes_updateDistanceAndCalories() = runTest {
        profiles.saveProfile(UserProfile(178f, 80f, 75f, FitnessGoal.STAY_FIT, TrainingPlace.GYM))
        val viewModel = newViewModel()
        viewModel.start()

        runMeters(100)
        clock += 30_000
        advanceTimeBy(1_001)

        val state = viewModel.uiState.value
        assertEquals(100.0, state.distanceM, 0.5)
        assertFalse(state.waitingForGps)
        assertTrue(state.calories > 0)
        assertTrue(state.weightKnown)
    }

    @Test
    fun clock_movesEverySecond_evenStandingStill() = runTest {
        val viewModel = newViewModel()
        viewModel.start()
        runCurrent()

        clock += 5_000
        advanceTimeBy(1_001)

        assertEquals(5_000L, viewModel.uiState.value.elapsedMs)
    }

    @Test
    fun withoutAWeight_caloriesUseTheDefault_andSaySo() = runTest {
        val viewModel = newViewModel()
        viewModel.start()
        assertFalse(viewModel.uiState.value.weightKnown)
    }

    @Test
    fun finish_isDisabled_untilTenMeters() = runTest {
        val viewModel = newViewModel()
        viewModel.start()

        viewModel.finish()

        assertTrue(viewModel.uiState.value.isRunning)
        assertTrue(runs.saved.isEmpty())
    }

    @Test
    fun finish_savesTheRun_andStopsRecording() = runTest {
        val viewModel = newViewModel()
        viewModel.start()
        runMeters(200)
        clock += 60_000
        advanceTimeBy(1_001)

        viewModel.finish()

        val saved = runs.saved.single()
        assertEquals(200.0, saved.distanceM, 0.5)
        assertEquals(60_000L, saved.durationMs)
        assertEquals(RunOutcome.SAVED, viewModel.uiState.value.outcome)
        assertNull(recorder.active.value)
        assertFalse(tracker.isListening)
    }

    @Test
    fun leaving_withNoRun_doesNotAsk() = runTest {
        val viewModel = newViewModel()
        viewModel.requestLeave()

        assertFalse(viewModel.uiState.value.showLeaveDialog)
        assertEquals(RunOutcome.DISCARDED, viewModel.uiState.value.outcome)
    }

    @Test
    fun leaving_duringARun_asksFirst_andDiscardSavesNothing() = runTest {
        val viewModel = newViewModel()
        viewModel.start()
        runMeters(50)

        viewModel.requestLeave()
        assertTrue(viewModel.uiState.value.showLeaveDialog)

        viewModel.discard()

        assertEquals(RunOutcome.DISCARDED, viewModel.uiState.value.outcome)
        assertTrue(runs.saved.isEmpty())
        assertNull(recorder.active.value)
    }

    @Test
    fun keepGoing_closesTheDialog_andTheRunContinues() = runTest {
        val viewModel = newViewModel()
        viewModel.start()

        viewModel.requestLeave()
        viewModel.dismissLeave()

        assertFalse(viewModel.uiState.value.showLeaveDialog)
        assertTrue(viewModel.uiState.value.isRunning)
    }

    @Test
    fun locationError_isShown() = runTest {
        tracker.failNextWith(SecurityException("permission removed"))
        val viewModel = newViewModel()
        viewModel.start()

        assertTrue(viewModel.uiState.value.locationFailed)
        assertFalse(viewModel.uiState.value.waitingForGps)
    }
}

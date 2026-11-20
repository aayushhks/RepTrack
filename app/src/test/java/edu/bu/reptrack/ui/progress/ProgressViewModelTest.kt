package edu.bu.reptrack.ui.progress

import edu.bu.reptrack.domain.profile.FieldError
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.domain.weight.WeightEntry
import edu.bu.reptrack.testing.FakeProfileRepository
import edu.bu.reptrack.testing.FakeRunRepository
import edu.bu.reptrack.testing.FakeWeightRepository
import edu.bu.reptrack.testing.FakeWorkoutRepository
import edu.bu.reptrack.testing.MainDispatcherRule
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
class ProgressViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var clock = 1_000L
    private val repository = FakeWorkoutRepository(now = { clock })
    private val runs = FakeRunRepository()
    private val weights = FakeWeightRepository()
    private val profiles = FakeProfileRepository()

    private fun newViewModel() = ProgressViewModel(repository, runs, weights, profiles, now = { clock })

    // uiState only updates while someone collects it, like the screen does.
    private fun TestScope.startCollecting(viewModel: ProgressViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private val profile = UserProfile(178f, 84f, 78f, FitnessGoal.LOSE_WEIGHT, TrainingPlace.GYM)

    @Test
    fun startsLoading() {
        assertTrue(newViewModel().uiState.value.isLoading)
    }

    @Test
    fun noWorkouts_showsAnEmptyHistory() = runTest {
        val viewModel = newViewModel()
        startCollecting(viewModel)

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertNull(viewModel.uiState.value.weight)
    }

    @Test
    fun finishedWorkout_showsUpByItself_withTotals() = runTest {
        val viewModel = newViewModel()
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
        val viewModel = newViewModel()
        startCollecting(viewModel)

        val id = repository.startSession("upper", "Upper body")
        repository.addSet(id, "Bench press", 60f, 10)

        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun savedRun_showsUpByItself_nextToWorkouts_newestFirst() = runTest {
        val viewModel = newViewModel()
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

    @Test
    fun weighIns_showCurrentWeight_change_andGoalProgress() = runTest {
        profiles.saveProfile(profile)
        weights.addWeight(84f, loggedAt = 1_000L)
        weights.addWeight(81f, loggedAt = 2_000L)
        val viewModel = newViewModel()
        startCollecting(viewModel)

        val weight = viewModel.uiState.value.weight!!
        assertEquals(81f, weight.latest.weightKg)
        assertEquals(-3f, weight.changeKg!!, 0.001f)
        assertEquals(78f, weight.goalKg)
        assertEquals(0.5f, weight.goalProgress!!, 0.001f) // 84 to 78, now 81
        assertTrue(weight.hasTrend)
        assertEquals(listOf(81f, 84f), weight.recent.map { it.weightKg })
    }

    @Test
    fun withoutAGoal_thereIsNoGoalProgress() = runTest {
        weights.addWeight(84f, loggedAt = 1_000L)
        val viewModel = newViewModel()
        startCollecting(viewModel)

        val weight = viewModel.uiState.value.weight!!
        assertNull(weight.goalKg)
        assertNull(weight.goalProgress)
        assertFalse(weight.hasTrend)
    }

    @Test
    fun openLogWeight_fillsInTheLastWeight() = runTest {
        weights.addWeight(82.5f, loggedAt = 1_000L)
        val viewModel = newViewModel()
        startCollecting(viewModel)

        viewModel.openLogWeight()

        assertEquals("82.5", viewModel.uiState.value.logDialog!!.input)
    }

    @Test
    fun savingAValidWeight_logsIt_closesTheDialog_andUpdatesTheProfile() = runTest {
        profiles.saveProfile(profile)
        val viewModel = newViewModel()
        startCollecting(viewModel)

        clock = 7_000L
        viewModel.openLogWeight()
        viewModel.onWeightInput("82,4") // a comma works as a decimal point
        viewModel.saveWeight()

        assertEquals(listOf(WeightEntry(1, 7_000L, 82.4f)), weights.saved)
        assertNull(viewModel.uiState.value.logDialog)
        assertEquals(82.4f, profiles.savedProfile!!.weightKg)
    }

    @Test
    fun outOfRangeWeight_showsAnError_andSavesNothing() = runTest {
        val viewModel = newViewModel()
        startCollecting(viewModel)

        viewModel.openLogWeight()
        viewModel.onWeightInput("8")
        assertEquals(FieldError.OUT_OF_RANGE, viewModel.uiState.value.logDialog!!.error)

        viewModel.saveWeight()

        assertTrue(weights.saved.isEmpty())
        assertEquals(FieldError.OUT_OF_RANGE, viewModel.uiState.value.logDialog!!.error)
    }

    @Test
    fun savingAnEmptyField_saysWhy() = runTest {
        val viewModel = newViewModel()
        startCollecting(viewModel)

        viewModel.openLogWeight()
        viewModel.saveWeight()

        assertEquals(FieldError.EMPTY, viewModel.uiState.value.logDialog!!.error)
        assertTrue(weights.saved.isEmpty())
    }

    @Test
    fun cancel_closesTheDialog_withoutSaving() = runTest {
        val viewModel = newViewModel()
        startCollecting(viewModel)

        viewModel.openLogWeight()
        viewModel.onWeightInput("80")
        viewModel.dismissLogWeight()

        assertNull(viewModel.uiState.value.logDialog)
        assertTrue(weights.saved.isEmpty())
    }

    @Test
    fun delete_asksFirst_thenRemoves_andTheProfileGoesBackToThePreviousWeight() = runTest {
        profiles.saveProfile(profile.copy(weightKg = 92f))
        weights.addWeight(84f, loggedAt = 1_000L)
        val typo = weights.addWeight(92f, loggedAt = 2_000L)
        val viewModel = newViewModel()
        startCollecting(viewModel)
        val entry = viewModel.uiState.value.weight!!.latest

        viewModel.requestDelete(entry)
        assertEquals(typo, viewModel.uiState.value.pendingDelete!!.id)
        assertEquals(2, weights.saved.size) // nothing deleted yet

        viewModel.confirmDelete()

        assertNull(viewModel.uiState.value.pendingDelete)
        assertEquals(listOf(84f), weights.saved.map { it.weightKg })
        assertEquals(84f, profiles.savedProfile!!.weightKg)
    }

    @Test
    fun keepInTheDeleteDialog_keepsTheWeighIn() = runTest {
        weights.addWeight(84f, loggedAt = 1_000L)
        val viewModel = newViewModel()
        startCollecting(viewModel)

        viewModel.requestDelete(viewModel.uiState.value.weight!!.latest)
        viewModel.dismissDelete()

        assertNull(viewModel.uiState.value.pendingDelete)
        assertEquals(1, weights.saved.size)
    }

    @Test
    fun weekTotals_countOnlyThisWeek() {
        val monday = 1_000_000L
        val items = listOf(
            HistoryItem.Run(RunSummary(id = 1, startedAt = 0, finishedAt = monday + 10, distanceM = 5_000.0, durationMs = 1, calories = 300)),
            HistoryItem.Run(RunSummary(id = 2, startedAt = 0, finishedAt = monday - 10, distanceM = 9_000.0, durationMs = 1, calories = 900)),
        )

        val totals = weekTotals(items, weekStart = monday)

        assertEquals(WeekTotals(workouts = 0, distanceM = 5_000.0, calories = 300), totals)
    }

    @Test
    fun weightSaveFailure_showsAMessage_untilDismissed() = runTest {
        val viewModel = newViewModel()
        startCollecting(viewModel)
        weights.failWrites = true

        viewModel.openLogWeight()
        viewModel.onWeightInput("80")
        viewModel.saveWeight()

        assertEquals(ProgressMessage.WEIGHT_NOT_SAVED, viewModel.uiState.value.message)
        assertTrue(weights.saved.isEmpty())

        viewModel.dismissMessage()
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun loadFailure_offersTryAgain_andTryAgainReloads() = runTest {
        weights.failReads = true
        val viewModel = newViewModel()
        startCollecting(viewModel)
        assertTrue(viewModel.uiState.value.loadFailed)

        weights.failReads = false
        viewModel.retry()

        assertFalse(viewModel.uiState.value.loadFailed)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}

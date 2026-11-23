package edu.bu.reptrack.ui.workouts

import androidx.lifecycle.SavedStateHandle
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.PlanType
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.domain.workout.PlanDayStatus
import edu.bu.reptrack.testing.FakePlanRepository
import edu.bu.reptrack.testing.FakeProfileRepository
import edu.bu.reptrack.testing.FakeRunRepository
import edu.bu.reptrack.testing.FakeWeightRepository
import edu.bu.reptrack.testing.FakeWorkoutRepository
import edu.bu.reptrack.testing.MainDispatcherRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
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
class WorkoutsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProfileRepository()
    private val zone = ZoneOffset.UTC

    // Noon UTC on a date, so the day never shifts. "Today" is Wednesday, November 18, 2026.
    private fun at(date: String) = LocalDate.parse(date).atTime(12, 0).toInstant(zone).toEpochMilli()
    private var clock = at("2026-11-18")
    private val workouts = FakeWorkoutRepository(now = { clock })
    private val runs = FakeRunRepository()
    private val weights = FakeWeightRepository()
    private val plan = FakePlanRepository()

    private fun newViewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        WorkoutsViewModel(repository, savedState, workouts, runs, weights, plan, now = { clock }, zone = zone)

    private val muscleAtGym = UserProfile(178f, 82f, 76f, FitnessGoal.BUILD_MUSCLE, TrainingPlace.GYM)

    // uiState only updates while someone collects it, like the screen does.
    private fun TestScope.startCollecting(viewModel: WorkoutsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun startsLoading_beforeAnythingCollects() {
        val viewModel = newViewModel()

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun noProfile_showsDefaultPlan() = runTest {
        val viewModel = newViewModel()
        startCollecting(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.profile)
        assertEquals(PlanType.FULL_BODY, state.plan.type)
        assertTrue(state.routines.isNotEmpty())
    }

    @Test
    fun savedProfile_drivesThePlanAndRoutines() = runTest {
        repository.saveProfile(muscleAtGym)
        val viewModel = newViewModel()
        startCollecting(viewModel)

        val state = viewModel.uiState.value
        assertEquals(muscleAtGym, state.profile)
        assertEquals(PlanType.UPPER_LOWER, state.plan.type)
        assertEquals(listOf("upper", "lower"), state.routines.map { it.id })
    }

    @Test
    fun switchingToHome_changesPlanToBodyweight() = runTest {
        repository.saveProfile(muscleAtGym)
        val viewModel = newViewModel()
        startCollecting(viewModel)

        viewModel.onPlaceSelected(TrainingPlace.HOME)

        val state = viewModel.uiState.value
        assertEquals(TrainingPlace.HOME, state.place)
        assertEquals(PlanType.BODYWEIGHT_STRENGTH, state.plan.type)
        // The saved profile does not change.
        assertEquals(TrainingPlace.GYM, repository.savedProfile?.place)
    }

    @Test
    fun profileSavedLater_updatesTheScreen() = runTest {
        val viewModel = newViewModel()
        startCollecting(viewModel)

        repository.saveProfile(muscleAtGym)

        assertEquals(muscleAtGym, viewModel.uiState.value.profile)
    }

    @Test
    fun chosenPlace_comesBackFromSavedState() = runTest {
        repository.saveProfile(muscleAtGym)
        val viewModel = newViewModel(SavedStateHandle(mapOf("place" to "HOME")))
        startCollecting(viewModel)

        assertEquals(TrainingPlace.HOME, viewModel.uiState.value.place)
    }

    private suspend fun finishWorkout(routineId: String, name: String, date: String) {
        clock = at(date)
        val id = workouts.startSession(routineId, name)
        workouts.addSet(id, "Squat", 60f, 10)
        workouts.finishSession(id)
        clock = at("2026-11-18")
    }

    @Test
    fun nextRoutine_followsTheLastFinishedOne() = runTest {
        repository.saveProfile(muscleAtGym)
        val viewModel = newViewModel()
        startCollecting(viewModel)
        assertEquals("upper", viewModel.uiState.value.nextRoutine?.id)

        finishWorkout("upper", "Upper body", "2026-11-16")

        val state = viewModel.uiState.value
        assertEquals("lower", state.nextRoutine?.id)
        assertEquals(listOf("upper"), state.otherRoutines.map { it.id })
    }

    @Test
    fun weekStrip_streakAndCount_comeFromWorkoutsAndRuns() = runTest {
        val viewModel = newViewModel()
        startCollecting(viewModel)

        finishWorkout("full_body_a", "Full body A", "2026-11-16")
        runs.saveRun(RunSummary(startedAt = at("2026-11-17"), finishedAt = at("2026-11-17"), distanceM = 3_000.0, durationMs = 900_000, calories = 200))
        finishWorkout("full_body_b", "Full body B", "2026-11-18")

        val state = viewModel.uiState.value
        assertEquals(3, state.streakDays)
        assertEquals(2, state.workoutsThisWeek)
        assertEquals(7, state.week.size)
        assertTrue(state.week[2].isToday)
    }

    @Test
    fun goalProgress_comesFromTheWeightLog() = runTest {
        repository.saveProfile(muscleAtGym.copy(weightKg = 79f)) // goal 76
        weights.addWeight(82f, loggedAt = at("2026-11-01"))
        weights.addWeight(79f, loggedAt = at("2026-11-15"))
        val viewModel = newViewModel()
        startCollecting(viewModel)

        assertEquals(0.5f, viewModel.uiState.value.goalProgress!!, 0.001f)
    }

    @Test
    fun noWeighIns_meansNoGoalProgress() = runTest {
        repository.saveProfile(muscleAtGym)
        val viewModel = newViewModel()
        startCollecting(viewModel)

        assertNull(viewModel.uiState.value.goalProgress)
    }

    @Test
    fun weightLogReadFailure_stillShowsThePlan() = runTest {
        repository.saveProfile(muscleAtGym)
        weights.failReads = true
        val viewModel = newViewModel()
        startCollecting(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(PlanType.UPPER_LOWER, state.plan.type)
        assertNull(state.goalProgress)
    }

    // ---- The week plan and edited routines ----

    @Test
    fun weekPlan_putsTheRoutinesOnTheDefaultDays() = runTest {
        repository.saveProfile(muscleAtGym)
        val viewModel = newViewModel()
        startCollecting(viewModel)

        // Upper/lower, 4 days: Monday, Tuesday, Thursday, Friday. Today is Wednesday, nothing done yet.
        val state = viewModel.uiState.value
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), state.trainingDays)
        val week = state.weekPlan
        assertEquals(7, week.size)
        assertEquals(PlanDayStatus.MISSED, week[0].status)
        assertEquals(PlanDayStatus.REST, week[2].status)
        assertEquals("upper", week[3].routineId)
        assertEquals("lower", week[4].routineId)
    }

    @Test
    fun ownTrainingDays_replaceTheDefaultOnes() = runTest {
        repository.saveProfile(muscleAtGym)
        plan.days.value = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY)
        val viewModel = newViewModel()
        startCollecting(viewModel)

        val week = viewModel.uiState.value.weekPlan
        assertEquals(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY), viewModel.uiState.value.trainingDays)
        assertEquals(PlanDayStatus.TODAY, week[2].status)
        assertEquals(viewModel.uiState.value.nextRoutine?.id, week[2].routineId)
    }

    @Test
    fun editedRoutine_showsItsNewExercises() = runTest {
        repository.saveProfile(muscleAtGym)
        plan.edits.value = mapOf("upper" to listOf("Incline bench press", "Pull-up"))
        val viewModel = newViewModel()
        startCollecting(viewModel)

        val upper = viewModel.uiState.value.routines.first { it.id == "upper" }
        assertEquals(listOf("Incline bench press", "Pull-up"), upper.exercises)
        assertTrue(upper.isEdited)
    }
}

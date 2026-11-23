package edu.bu.reptrack.ui.plan

import androidx.lifecycle.SavedStateHandle
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.PlanType
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.domain.workout.PlanDayStatus
import edu.bu.reptrack.testing.FakePlanRepository
import edu.bu.reptrack.testing.FakeProfileRepository
import edu.bu.reptrack.testing.FakeRunRepository
import edu.bu.reptrack.testing.FakeWorkoutRepository
import edu.bu.reptrack.testing.MainDispatcherRule
import edu.bu.reptrack.ui.navigation.Routes
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneOffset.UTC

    // "Today" is Wednesday, November 18, 2026, at noon.
    private val clock = LocalDate.parse("2026-11-18").atTime(12, 0).toInstant(zone).toEpochMilli()
    private val profiles = FakeProfileRepository()
    private val plan = FakePlanRepository()
    private val workouts = FakeWorkoutRepository(now = { clock })
    private val runs = FakeRunRepository()

    private fun TestScope.open(place: String? = null): PlanViewModel {
        val savedState = SavedStateHandle(if (place == null) emptyMap() else mapOf(Routes.ARG_PLACE to place))
        val viewModel = PlanViewModel(profiles, plan, workouts, runs, savedState, now = { clock }, zone = zone)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun showsThePlansDefaultDays_andTheWeek() = runTest {
        profiles.saveProfile(UserProfile(178f, 82f, 76f, FitnessGoal.LOSE_WEIGHT, TrainingPlace.GYM))
        val state = open().uiState.value

        assertEquals(PlanType.FULL_BODY, state.plan.type)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), state.trainingDays)
        assertFalse(state.customDays)
        assertEquals(PlanDayStatus.TODAY, state.week[2].status)
        assertEquals(2, state.routines.size)
    }

    @Test
    fun placeFromTheWorkoutsTab_winsOverTheProfile() = runTest {
        profiles.saveProfile(UserProfile(178f, 82f, 76f, FitnessGoal.LOSE_WEIGHT, TrainingPlace.GYM))
        assertEquals(PlanType.BODYWEIGHT_FULL_BODY, open(place = "HOME").uiState.value.plan.type)
    }

    @Test
    fun togglingDays_savesThem_butNeverTheLastOne() = runTest {
        val viewModel = open()
        // No profile: stay fit at the gym, 2 days (Monday and Thursday).

        viewModel.toggleDay(DayOfWeek.SATURDAY)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), plan.days.value)
        assertTrue(viewModel.uiState.value.customDays)

        viewModel.toggleDay(DayOfWeek.MONDAY)
        viewModel.toggleDay(DayOfWeek.THURSDAY)
        viewModel.toggleDay(DayOfWeek.SATURDAY)
        assertEquals(setOf(DayOfWeek.SATURDAY), plan.days.value)
    }

    @Test
    fun resetDays_goesBackToTheDefault() = runTest {
        plan.days.value = setOf(DayOfWeek.SUNDAY)
        val viewModel = open()

        viewModel.resetDays()

        assertTrue(plan.days.value.isEmpty())
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), viewModel.uiState.value.trainingDays)
    }

    @Test
    fun saveFailure_isShown() = runTest {
        val viewModel = open()
        plan.failWrites = true

        viewModel.toggleDay(DayOfWeek.SUNDAY)

        assertTrue(viewModel.uiState.value.saveFailed)
    }
}

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
import org.junit.Assert.assertNull
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

    // ---- The user's own plan ----

    private suspend fun TestScope.openWithProfile(): PlanViewModel {
        // Lose weight at the gym: full body A and B on Monday, Wednesday and Friday.
        profiles.saveProfile(UserProfile(178f, 82f, 76f, FitnessGoal.LOSE_WEIGHT, TrainingPlace.GYM))
        return open()
    }

    @Test
    fun startFromSuggested_copiesTheRoutines_andGivesThemTheDaysInTurn() = runTest {
        plan.edits.value = mapOf("full_body_a" to listOf("Squat", "Dips"))
        val viewModel = openWithProfile()

        viewModel.startFromSuggested()

        val state = viewModel.uiState.value
        assertTrue(state.isCustom)
        assertEquals(listOf("Full body A", "Full body B"), state.routines.map { it.name })
        assertEquals(listOf("Squat", "Dips"), state.routines[0].exercises)
        assertEquals(
            mapOf(DayOfWeek.MONDAY to "custom_1", DayOfWeek.WEDNESDAY to "custom_2", DayOfWeek.FRIDAY to "custom_1"),
            state.schedule,
        )
        // Wednesday is today.
        assertEquals("Full body B", state.week[2].routineName)
    }

    @Test
    fun startFromScratch_makesOneEmptyRoutineOnTheTrainingDays_andOpensIt() = runTest {
        val viewModel = openWithProfile()

        viewModel.startFromScratch("  Push day ")

        val state = viewModel.uiState.value
        assertEquals(listOf("Push day"), state.routines.map { it.name })
        assertTrue(state.routines.single().exercises.isEmpty())
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), state.schedule.keys)
        assertEquals("custom_1", state.createdRoutineId)

        viewModel.onCreatedRoutineOpened()
        assertNull(viewModel.uiState.value.createdRoutineId)
    }

    @Test
    fun blankNames_areIgnored() = runTest {
        val viewModel = openWithProfile()
        viewModel.startFromScratch("   ")
        assertFalse(viewModel.uiState.value.isCustom)
    }

    @Test
    fun addRenameAndDelete_routines() = runTest {
        val viewModel = openWithProfile()
        viewModel.startFromScratch("Push")

        viewModel.addRoutine("Pull")
        assertEquals("custom_2", viewModel.uiState.value.createdRoutineId)
        viewModel.renameRoutine("custom_2", "Pull and core")
        assertEquals(listOf("Push", "Pull and core"), viewModel.uiState.value.routines.map { it.name })

        viewModel.deleteRoutine("custom_2")
        assertEquals(listOf("Push"), viewModel.uiState.value.routines.map { it.name })

        // The last routine stays.
        viewModel.deleteRoutine("custom_1")
        assertEquals(1, viewModel.uiState.value.routines.size)
        assertFalse(viewModel.uiState.value.canDeleteRoutine)
    }

    @Test
    fun deletingARoutine_makesItsDaysRestDays() = runTest {
        val viewModel = openWithProfile()
        viewModel.startFromSuggested()

        viewModel.deleteRoutine("custom_1")

        assertEquals(mapOf(DayOfWeek.WEDNESDAY to "custom_2"), viewModel.uiState.value.schedule)
    }

    @Test
    fun pickingARoutineForADay_orARestDay_butNeverTheLastTrainingDay() = runTest {
        val viewModel = openWithProfile()
        viewModel.startFromScratch("Push")
        viewModel.addRoutine("Legs")

        viewModel.setDayRoutine(DayOfWeek.SATURDAY, "custom_2")
        viewModel.setDayRoutine(DayOfWeek.MONDAY, null)
        viewModel.setDayRoutine(DayOfWeek.WEDNESDAY, "custom_2")

        assertEquals(
            mapOf(DayOfWeek.WEDNESDAY to "custom_2", DayOfWeek.FRIDAY to "custom_1", DayOfWeek.SATURDAY to "custom_2"),
            viewModel.uiState.value.schedule,
        )
        assertEquals(3, viewModel.uiState.value.trainingDays.size)

        viewModel.setDayRoutine(DayOfWeek.WEDNESDAY, null)
        viewModel.setDayRoutine(DayOfWeek.FRIDAY, null)
        viewModel.setDayRoutine(DayOfWeek.SATURDAY, null)
        assertEquals(mapOf(DayOfWeek.SATURDAY to "custom_2"), viewModel.uiState.value.schedule)
    }

    @Test
    fun useSuggestedPlan_deletesTheOwnPlan() = runTest {
        val viewModel = openWithProfile()
        viewModel.startFromSuggested()

        viewModel.useSuggestedPlan()

        val state = viewModel.uiState.value
        assertFalse(state.isCustom)
        assertEquals(listOf("full_body_a", "full_body_b"), state.routines.map { it.id })
        assertTrue(plan.ownRoutines.value.isEmpty())
    }

    @Test
    fun onTheOwnPlan_theSuggestedDaysAreLeftAlone() = runTest {
        val viewModel = openWithProfile()
        viewModel.startFromSuggested()

        viewModel.toggleDay(DayOfWeek.SUNDAY)

        assertTrue(plan.days.value.isEmpty())
    }
}

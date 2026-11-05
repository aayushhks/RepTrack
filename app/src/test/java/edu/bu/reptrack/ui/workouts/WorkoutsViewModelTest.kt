package edu.bu.reptrack.ui.workouts

import androidx.lifecycle.SavedStateHandle
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.PlanType
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.testing.FakeProfileRepository
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
class WorkoutsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProfileRepository()

    private val muscleAtGym = UserProfile(178f, 82f, 76f, FitnessGoal.BUILD_MUSCLE, TrainingPlace.GYM)

    // uiState only updates while someone collects it, like the screen does.
    private fun TestScope.startCollecting(viewModel: WorkoutsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun startsLoading_beforeAnythingCollects() {
        val viewModel = WorkoutsViewModel(repository, SavedStateHandle())

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun noProfile_showsDefaultPlan() = runTest {
        val viewModel = WorkoutsViewModel(repository, SavedStateHandle())
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
        val viewModel = WorkoutsViewModel(repository, SavedStateHandle())
        startCollecting(viewModel)

        val state = viewModel.uiState.value
        assertEquals(muscleAtGym, state.profile)
        assertEquals(PlanType.UPPER_LOWER, state.plan.type)
        assertEquals(listOf("upper", "lower"), state.routines.map { it.id })
    }

    @Test
    fun switchingToHome_changesPlanToBodyweight() = runTest {
        repository.saveProfile(muscleAtGym)
        val viewModel = WorkoutsViewModel(repository, SavedStateHandle())
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
        val viewModel = WorkoutsViewModel(repository, SavedStateHandle())
        startCollecting(viewModel)

        repository.saveProfile(muscleAtGym)

        assertEquals(muscleAtGym, viewModel.uiState.value.profile)
    }

    @Test
    fun chosenPlace_comesBackFromSavedState() = runTest {
        repository.saveProfile(muscleAtGym)
        val viewModel = WorkoutsViewModel(repository, SavedStateHandle(mapOf("place" to "HOME")))
        startCollecting(viewModel)

        assertEquals(TrainingPlace.HOME, viewModel.uiState.value.place)
    }
}

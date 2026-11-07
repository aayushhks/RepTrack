package edu.bu.reptrack.ui.setup

import androidx.lifecycle.SavedStateHandle
import edu.bu.reptrack.domain.profile.FieldError
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.PlanType
import edu.bu.reptrack.domain.profile.StarterPlan
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.testing.FakeProfileRepository
import edu.bu.reptrack.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoalSetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProfileRepository()

    private fun newViewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        GoalSetupViewModel(repository, savedState)

    private fun GoalSetupViewModel.fillValidForm() {
        onHeightChange("178")
        onWeightChange("82")
        onGoalWeightChange("76")
    }

    @Test
    fun startsEmpty_withNoErrors_andCannotSave() = runTest {
        val state = newViewModel().uiState.value

        assertEquals("", state.height)
        assertNull(state.heightError)
        assertFalse(state.canSave)
        assertEquals(StarterPlan(PlanType.FULL_BODY, 3), state.plan)
    }

    @Test
    fun validForm_canSave() = runTest {
        val viewModel = newViewModel()

        viewModel.fillValidForm()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun outOfRangeHeight_showsError_andBlocksSave() = runTest {
        val viewModel = newViewModel()

        viewModel.fillValidForm()
        viewModel.onHeightChange("90")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(FieldError.OUT_OF_RANGE, state.heightError)
        assertFalse(state.canSave)
    }

    @Test
    fun typing_cleansTheNumber() = runTest {
        val viewModel = newViewModel()

        viewModel.onWeightChange("72,5 kg")
        advanceUntilIdle()

        assertEquals("72.5", viewModel.uiState.value.weight)
    }

    @Test
    fun changingGoalAndPlace_updatesSuggestedPlan() = runTest {
        val viewModel = newViewModel()

        viewModel.onGoalSelected(FitnessGoal.BUILD_MUSCLE)
        viewModel.onPlaceSelected(TrainingPlace.HOME)
        advanceUntilIdle()

        assertEquals(StarterPlan(PlanType.BODYWEIGHT_STRENGTH, 3), viewModel.uiState.value.plan)
    }

    @Test
    fun save_storesProfile_andFinishes() = runTest {
        val viewModel = newViewModel()
        viewModel.fillValidForm()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            UserProfile(178f, 82f, 76f, FitnessGoal.LOSE_WEIGHT, TrainingPlace.GYM),
            repository.savedProfile,
        )
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun save_withInvalidForm_doesNothing() = runTest {
        val viewModel = newViewModel()

        viewModel.save()
        advanceUntilIdle()

        assertNull(repository.savedProfile)
        assertFalse(repository.setupDone)
        assertFalse(viewModel.uiState.value.isDone)
    }

    @Test
    fun skip_finishesWithoutAProfile() = runTest {
        val viewModel = newViewModel()

        viewModel.skip()
        advanceUntilIdle()

        assertNull(repository.savedProfile)
        assertTrue(repository.setupDone)
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun typedValues_comeBackFromSavedState() = runTest {
        // Same keys the ViewModel writes, as if Android restored them after the app was killed.
        val restored = SavedStateHandle(mapOf("height" to "180", "goal" to "STAY_FIT"))

        val state = newViewModel(restored).uiState.value

        assertEquals("180", state.height)
        assertEquals(FitnessGoal.STAY_FIT, state.goal)
    }

    @Test
    fun savedProfile_opensTheFormFilledIn_forEditing() = runTest {
        repository.saveProfile(UserProfile(178f, 82.5f, 76f, FitnessGoal.BUILD_MUSCLE, TrainingPlace.HOME))

        val state = newViewModel().uiState.value

        assertEquals("178", state.height)
        assertEquals("82.5", state.weight)
        assertEquals("76", state.goalWeight)
        assertEquals(FitnessGoal.BUILD_MUSCLE, state.goal)
        assertEquals(TrainingPlace.HOME, state.place)
        assertTrue(state.canSave)
    }

    @Test
    fun valuesTypedBeforeRotation_areNotReplacedByTheSavedProfile() = runTest {
        repository.saveProfile(UserProfile(178f, 82f, 76f, FitnessGoal.BUILD_MUSCLE, TrainingPlace.GYM))

        val state = newViewModel(SavedStateHandle(mapOf("height" to "190"))).uiState.value

        assertEquals("190", state.height)
    }
}

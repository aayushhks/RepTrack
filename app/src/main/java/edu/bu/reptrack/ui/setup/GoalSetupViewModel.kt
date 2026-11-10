package edu.bu.reptrack.ui.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.profile.ProfileRepository
import edu.bu.reptrack.domain.profile.FieldError
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.ProfileValidator
import edu.bu.reptrack.domain.profile.StarterPlan
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.formatKg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the setup screen needs to draw itself. */
data class GoalSetupUiState(
    val height: String = "",
    val weight: String = "",
    val goalWeight: String = "",
    val goal: FitnessGoal = FitnessGoal.LOSE_WEIGHT,
    val place: TrainingPlace = TrainingPlace.GYM,
    val heightError: FieldError? = null,
    val weightError: FieldError? = null,
    val goalWeightError: FieldError? = null,
    val plan: StarterPlan = StarterPlan.suggest(FitnessGoal.LOSE_WEIGHT, TrainingPlace.GYM),
    val canSave: Boolean = false,
    val isDone: Boolean = false,
)

/**
 * Holds the setup form. Typed values live in [SavedStateHandle], so they survive
 * rotation and the app being killed in the background. If a profile is already saved,
 * the form opens filled in, so the same screen works for editing stats from Profile.
 */
class GoalSetupViewModel(
    private val repository: ProfileRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val height = savedState.getStateFlow(KEY_HEIGHT, "")
    private val weight = savedState.getStateFlow(KEY_WEIGHT, "")
    private val goalWeight = savedState.getStateFlow(KEY_GOAL_WEIGHT, "")
    private val goal = savedState.getStateFlow(KEY_GOAL, FitnessGoal.LOSE_WEIGHT.name)
    private val place = savedState.getStateFlow(KEY_PLACE, TrainingPlace.GYM.name)
    private val done = MutableStateFlow(false)

    init {
        // Only on a fresh open. After rotation or the app being killed, keep what was typed.
        if (formIsEmpty()) {
            viewModelScope.launch {
                val saved = repository.profile.first() ?: return@launch
                // The user may have started typing while the profile loaded. Do not overwrite that.
                if (!formIsEmpty()) return@launch
                savedState[KEY_HEIGHT] = formatKg(saved.heightCm)
                savedState[KEY_WEIGHT] = formatKg(saved.weightKg)
                savedState[KEY_GOAL_WEIGHT] = formatKg(saved.goalWeightKg)
                savedState[KEY_GOAL] = saved.goal.name
                savedState[KEY_PLACE] = saved.place.name
            }
        }
    }

    private fun formIsEmpty() = height.value.isEmpty() && weight.value.isEmpty() && goalWeight.value.isEmpty()

    private val form = combine(height, weight, goalWeight, goal, place) { h, w, g, goalName, placeName ->
        FormValues(h, w, g, FitnessGoal.valueOf(goalName), TrainingPlace.valueOf(placeName))
    }

    val uiState: StateFlow<GoalSetupUiState> = combine(form, done) { values, isDone ->
        values.toUiState(isDone)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = currentValues().toUiState(isDone = false),
    )

    fun onHeightChange(text: String) {
        savedState[KEY_HEIGHT] = ProfileValidator.cleanNumber(text)
    }

    fun onWeightChange(text: String) {
        savedState[KEY_WEIGHT] = ProfileValidator.cleanNumber(text)
    }

    fun onGoalWeightChange(text: String) {
        savedState[KEY_GOAL_WEIGHT] = ProfileValidator.cleanNumber(text)
    }

    fun onGoalSelected(goal: FitnessGoal) {
        savedState[KEY_GOAL] = goal.name
    }

    fun onPlaceSelected(place: TrainingPlace) {
        savedState[KEY_PLACE] = place.name
    }

    /** Saves the profile if the form is valid, then marks setup as done. */
    fun save() {
        val values = currentValues()
        val profile = ProfileValidator.toProfile(
            values.height, values.weight, values.goalWeight, values.goal, values.place,
        ) ?: return
        viewModelScope.launch {
            repository.saveProfile(profile)
            done.value = true
        }
    }

    /** Skips setup for now. The user can still use the app. */
    fun skip() {
        viewModelScope.launch {
            repository.skipSetup()
            done.value = true
        }
    }

    private fun currentValues() = FormValues(
        height.value,
        weight.value,
        goalWeight.value,
        FitnessGoal.valueOf(goal.value),
        TrainingPlace.valueOf(place.value),
    )

    private data class FormValues(
        val height: String,
        val weight: String,
        val goalWeight: String,
        val goal: FitnessGoal,
        val place: TrainingPlace,
    ) {
        fun toUiState(isDone: Boolean): GoalSetupUiState {
            // Only show an error once the user has typed something in that field.
            fun errorFor(text: String, range: ClosedFloatingPointRange<Float>) =
                if (text.isEmpty()) null else ProfileValidator.check(text, range)

            return GoalSetupUiState(
                height = height,
                weight = weight,
                goalWeight = goalWeight,
                goal = goal,
                place = place,
                heightError = errorFor(height, ProfileValidator.HEIGHT_CM_RANGE),
                weightError = errorFor(weight, ProfileValidator.WEIGHT_KG_RANGE),
                goalWeightError = errorFor(goalWeight, ProfileValidator.WEIGHT_KG_RANGE),
                plan = StarterPlan.suggest(goal, place),
                canSave = ProfileValidator.toProfile(height, weight, goalWeight, goal, place) != null,
                isDone = isDone,
            )
        }
    }

    companion object {
        private const val KEY_HEIGHT = "height"
        private const val KEY_WEIGHT = "weight"
        private const val KEY_GOAL_WEIGHT = "goal_weight"
        private const val KEY_GOAL = "goal"
        private const val KEY_PLACE = "place"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RepTrackApplication
                GoalSetupViewModel(app.container.profileRepository, createSavedStateHandle())
            }
        }
    }
}

package edu.bu.reptrack.ui.workouts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.profile.ProfileRepository
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.StarterPlan
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.domain.workout.WorkoutTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Everything the Workouts tab needs to draw itself. */
data class WorkoutsUiState(
    val isLoading: Boolean = true,
    /** Null when the user skipped goal setup. */
    val profile: UserProfile? = null,
    val place: TrainingPlace = TrainingPlace.GYM,
    val plan: StarterPlan = StarterPlan.suggest(WorkoutsViewModel.DEFAULT_GOAL, place),
    val routines: List<WorkoutTemplate> = StarterRoutines.forPlan(plan.type),
)

/**
 * Builds the Workouts tab from the saved profile. The user can switch between gym and home
 * here without changing their saved profile. That choice lives in [SavedStateHandle], so it
 * survives rotation.
 */
class WorkoutsViewModel(
    profileRepository: ProfileRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val chosenPlace = savedState.getStateFlow<String?>(KEY_PLACE, null)

    val uiState: StateFlow<WorkoutsUiState> =
        combine(profileRepository.profile, chosenPlace) { profile, placeName ->
            // A place picked on this screen wins over the one saved in the profile.
            val place = placeName?.let { TrainingPlace.valueOf(it) } ?: profile?.place ?: TrainingPlace.GYM
            val plan = StarterPlan.suggest(profile?.goal ?: DEFAULT_GOAL, place)
            WorkoutsUiState(
                isLoading = false,
                profile = profile,
                place = place,
                plan = plan,
                routines = StarterRoutines.forPlan(plan.type),
            )
        }.stateIn(
            scope = viewModelScope,
            // Keep listening for 5 seconds after the screen goes away, so a rotation does not restart it.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WorkoutsUiState(),
        )

    fun onPlaceSelected(place: TrainingPlace) {
        savedState[KEY_PLACE] = place.name
    }

    companion object {
        /** Used when the user skipped goal setup. */
        val DEFAULT_GOAL = FitnessGoal.STAY_FIT

        private const val KEY_PLACE = "place"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RepTrackApplication
                WorkoutsViewModel(app.container.profileRepository, createSavedStateHandle())
            }
        }
    }
}

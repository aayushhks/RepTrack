package edu.bu.reptrack.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.exercise.ExerciseRepository
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** What the exercise detail screen shows. */
data class ExerciseDetailUiState(
    val isLoading: Boolean = true,
    /** Null once loading is over means the exercise is not saved. */
    val exercise: Exercise? = null,
)

/** One exercise from the saved catalog. The id comes from the route, through SavedStateHandle. */
class ExerciseDetailViewModel(
    repository: ExerciseRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val exerciseId: Int = checkNotNull(savedState[Routes.ARG_EXERCISE_ID])

    val uiState: StateFlow<ExerciseDetailUiState> = repository.observeExercise(exerciseId)
        .map { ExerciseDetailUiState(isLoading = false, exercise = it) }
        .catch { emit(ExerciseDetailUiState(isLoading = false)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExerciseDetailUiState(),
        )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RepTrackApplication).container
                ExerciseDetailViewModel(
                    repository = container.exerciseRepository,
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }
}

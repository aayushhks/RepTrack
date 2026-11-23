package edu.bu.reptrack.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.demo.DemoRepository
import edu.bu.reptrack.data.exercise.ExerciseRepository
import edu.bu.reptrack.domain.exercise.DemoMatch
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.domain.exercise.ExerciseDemo
import edu.bu.reptrack.ui.navigation.Routes
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** What the exercise detail screen shows. */
data class ExerciseDetailUiState(
    val isLoading: Boolean = true,
    /** Null once loading is over means the exercise is not saved. */
    val exercise: Exercise? = null,
    /** The two how-to photos and the steps from Free Exercise DB, when it has this exercise. */
    val demo: ExerciseDemo? = null,
)

/** One exercise from the saved catalog. The id comes from the route, through SavedStateHandle. */
class ExerciseDetailViewModel(
    repository: ExerciseRepository,
    savedState: SavedStateHandle,
    demos: DemoRepository,
) : ViewModel() {

    private val exerciseId: Int = checkNotNull(savedState[Routes.ARG_EXERCISE_ID])

    /** Empty until the photo set is loaded; it stays empty if it can not be, and the page shows without photos. */
    private val allDemos = flow {
        emit(emptyList())
        val loaded = try {
            demos.demos()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            emptyList()
        }
        emit(loaded)
    }

    val uiState: StateFlow<ExerciseDetailUiState> = combine(repository.observeExercise(exerciseId), allDemos) { exercise, all ->
        ExerciseDetailUiState(
            isLoading = false,
            exercise = exercise,
            demo = exercise?.let { DemoMatch.find(it.name, all, it.equipment) },
        )
    }
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
                    demos = container.demoRepository,
                )
            }
        }
    }
}

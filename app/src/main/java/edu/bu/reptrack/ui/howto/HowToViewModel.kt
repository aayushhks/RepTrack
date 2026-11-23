package edu.bu.reptrack.ui.howto

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
import edu.bu.reptrack.domain.exercise.CatalogMatch
import edu.bu.reptrack.domain.exercise.DemoMatch
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.domain.exercise.ExerciseDemo
import edu.bu.reptrack.ui.navigation.Routes
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Everything the how-to screen needs to draw itself. */
data class HowToUiState(
    val name: String = "",
    /** The photos and steps are still loading (the first time, they download). */
    val isLoading: Boolean = true,
    /** Photos and steps, or null if the photo set has no such exercise. */
    val demo: ExerciseDemo? = null,
    /** Muscles and equipment from the wger catalog, when it has the exercise. */
    val catalog: Exercise? = null,
    /** The photo set could not be downloaded. */
    val loadFailed: Boolean = false,
)

/** Loads how to do one exercise, by its name: photos and steps, plus the wger catalog's details. */
@OptIn(ExperimentalCoroutinesApi::class)
class HowToViewModel(
    demos: DemoRepository,
    catalog: ExerciseRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    val name: String = savedState.get<String>(Routes.ARG_EXERCISE_NAME).orEmpty()

    private sealed interface Load {
        data object Loading : Load
        data object Failed : Load
        data class Ready(val all: List<ExerciseDemo>) : Load
    }

    /** Bumped by Try again. */
    private val attempt = MutableStateFlow(0)

    private val load: Flow<Load> = attempt.flatMapLatest {
        flow {
            emit(Load.Loading)
            val result = try {
                Load.Ready(demos.demos())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Load.Failed
            }
            emit(result)
        }
    }

    val uiState: StateFlow<HowToUiState> = combine(
        load,
        catalog.observeExercises().catch { emit(emptyList()) },
    ) { load, exercises ->
        val match = CatalogMatch.find(name, exercises)
        when (load) {
            Load.Loading -> HowToUiState(name = name, catalog = match)
            Load.Failed -> HowToUiState(name = name, isLoading = false, catalog = match, loadFailed = true)
            is Load.Ready -> HowToUiState(
                name = name,
                isLoading = false,
                demo = DemoMatch.find(name, load.all, match?.equipment.orEmpty()),
                catalog = match,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HowToUiState(name = name),
    )

    fun retry() {
        attempt.update { it + 1 }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RepTrackApplication).container
                HowToViewModel(
                    demos = container.demoRepository,
                    catalog = container.exerciseRepository,
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }
}

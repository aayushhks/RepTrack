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
import edu.bu.reptrack.domain.exercise.categoriesOf
import edu.bu.reptrack.domain.exercise.filterExercises
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the Exercises tab needs to draw itself. */
data class ExercisesUiState(
    /** Downloading for the first time, with nothing saved to show yet. */
    val isLoading: Boolean = true,
    /** The saved exercises that match the search and the category. */
    val exercises: List<Exercise> = emptyList(),
    val categories: List<String> = emptyList(),
    val query: String = "",
    /** Null means all categories. */
    val category: String? = null,
    /** At least one exercise is saved, ignoring the search. */
    val hasSaved: Boolean = false,
    /** The last download failed. With saved exercises the screen still shows them. */
    val refreshFailed: Boolean = false,
)

private enum class RefreshState { LOADING, DONE, FAILED }

/**
 * Exercises tab: the wger catalog. The list always comes from the saved copy in Room; a
 * download runs when the tab opens and again on Try again.
 */
class ExercisesViewModel(
    private val repository: ExerciseRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val refreshState = MutableStateFlow(RefreshState.LOADING)
    private var refreshJob: Job? = null

    private val query = savedState.getStateFlow(KEY_QUERY, "")
    private val category = savedState.getStateFlow<String?>(KEY_CATEGORY, null)

    val uiState: StateFlow<ExercisesUiState> = combine(
        repository.observeExercises().catch { emit(emptyList()) },
        query,
        category,
        refreshState,
    ) { saved, query, category, refresh ->
        ExercisesUiState(
            isLoading = refresh == RefreshState.LOADING && saved.isEmpty(),
            exercises = filterExercises(saved, query, category),
            categories = categoriesOf(saved),
            query = query,
            category = category,
            hasSaved = saved.isNotEmpty(),
            refreshFailed = refresh == RefreshState.FAILED,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExercisesUiState(),
    )

    init {
        refresh(force = false)
    }

    fun onQueryChange(text: String) {
        savedState[KEY_QUERY] = text
    }

    /** Tapping the chosen category again, or All, shows every category. */
    fun onCategoryClick(selected: String?) {
        savedState[KEY_CATEGORY] = if (selected == category.value) null else selected
    }

    /** Clears the search text and the category, so the whole list is back. */
    fun clearSearch() {
        savedState[KEY_QUERY] = ""
        savedState[KEY_CATEGORY] = null
    }

    fun retry() = refresh(force = true)

    private fun refresh(force: Boolean) {
        if (refreshJob?.isActive == true) return
        refreshState.value = RefreshState.LOADING
        refreshJob = viewModelScope.launch {
            refreshState.value = try {
                repository.refresh(force)
                RefreshState.DONE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                RefreshState.FAILED
            }
        }
    }

    companion object {
        private const val KEY_QUERY = "exercises_query"
        private const val KEY_CATEGORY = "exercises_category"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RepTrackApplication).container
                ExercisesViewModel(
                    repository = container.exerciseRepository,
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }
}

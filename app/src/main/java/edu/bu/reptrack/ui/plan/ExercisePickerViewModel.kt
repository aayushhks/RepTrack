package edu.bu.reptrack.ui.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.exercise.ExerciseRepository
import edu.bu.reptrack.data.plan.PlanRepository
import edu.bu.reptrack.domain.exercise.CatalogMatch
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.domain.exercise.filterExercises
import edu.bu.reptrack.domain.workout.RoutineEdits
import edu.bu.reptrack.domain.workout.CustomPlan
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.domain.workout.findRoutine
import edu.bu.reptrack.ui.navigation.Routes
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the exercise picker needs to draw itself. */
data class ExercisePickerUiState(
    val isLoading: Boolean = true,
    val routineName: String = "",
    /** The exercise being swapped out. Null when adding a new one. */
    val replacing: String? = null,
    val query: String = "",
    /** When swapping: catalog exercises for the same muscles, shown before the rest. */
    val sameMuscles: List<Exercise> = emptyList(),
    /** Catalog exercises that match the search and are not in the routine yet. */
    val results: List<Exercise> = emptyList(),
    /** The search text, offered as the user's own exercise when the catalog has nothing by that name. */
    val customName: String? = null,
    val catalogEmpty: Boolean = false,
    val saveFailed: Boolean = false,
    /** The change is saved: the screen closes. */
    val done: Boolean = false,
)

/**
 * Picks an exercise from the wger catalog to add to a routine, or to swap one out. A swap puts
 * the new exercise in the old one's place and leaves the rest of the routine as it was. The
 * user can also type the name of an exercise the catalog does not have.
 */
class ExercisePickerViewModel(
    private val planRepository: PlanRepository,
    catalog: ExerciseRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val routineId: String = savedState.get<String>(Routes.ARG_ROUTINE_ID).orEmpty()

    /** The exercise to swap, by its place in the routine. -1: add a new one. */
    private val replaceIndex: Int = savedState.get<Int>(Routes.ARG_REPLACE_INDEX) ?: -1

    private val query = savedState.getStateFlow(KEY_QUERY, "")

    private data class Local(val saveFailed: Boolean = false, val done: Boolean = false)

    private val local = MutableStateFlow(Local())

    /** The routine's exercises right now, for saving. */
    private var current: List<String> = StarterRoutines.find(routineId)?.exercises.orEmpty()

    private val routine = combine(
        planRepository.observeRoutineEdits().catch { emit(emptyMap()) },
        planRepository.observeCustomPlan().catch { emit(CustomPlan()) },
    ) { edits, custom -> findRoutine(routineId, edits, custom) }

    val uiState: StateFlow<ExercisePickerUiState> = combine(
        routine,
        catalog.observeExercises().catch { emit(emptyList()) },
        query,
        local,
    ) { found, all, text, picked ->
        val exercises = found?.exercises.orEmpty()
        current = exercises
        val replacing = exercises.getOrNull(replaceIndex)
        val inRoutine = exercises.map(CatalogMatch::squash).toSet()
        val available = all.filter { CatalogMatch.squash(it.name) !in inRoutine }
        val sameMuscles = if (replacing != null && text.isBlank()) {
            CatalogMatch.find(replacing, all)?.let { CatalogMatch.sameMuscles(it, all, exclude = exercises) }.orEmpty()
        } else {
            emptyList()
        }
        val typed = text.trim()
        val custom = typed.takeIf { name ->
            name.isNotEmpty() &&
                all.none { CatalogMatch.squash(it.name) == CatalogMatch.squash(name) } &&
                CatalogMatch.squash(name) !in inRoutine
        }
        ExercisePickerUiState(
            isLoading = false,
            routineName = found?.name.orEmpty(),
            replacing = replacing,
            query = text,
            sameMuscles = sameMuscles,
            results = filterExercises(available, text, category = null).filter { it !in sameMuscles },
            customName = custom,
            catalogEmpty = all.isEmpty(),
            saveFailed = picked.saveFailed,
            done = picked.done,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExercisePickerUiState(),
    )

    fun onQueryChange(text: String) {
        savedState[KEY_QUERY] = text
    }

    /** Adds [name], or puts it in place of the exercise being swapped, then closes. */
    fun pick(name: String) {
        val changed = if (replaceIndex >= 0) RoutineEdits.replace(current, replaceIndex, name) else RoutineEdits.add(current, name)
        if (changed == current) {
            local.update { it.copy(done = true) }
            return
        }
        local.update { it.copy(saveFailed = false) }
        viewModelScope.launch {
            try {
                planRepository.setRoutineExercises(routineId, changed)
                local.update { it.copy(done = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                local.update { it.copy(saveFailed = true) }
            }
        }
    }

    companion object {
        private const val KEY_QUERY = "picker_query"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RepTrackApplication).container
                ExercisePickerViewModel(
                    planRepository = container.planRepository,
                    catalog = container.exerciseRepository,
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }
}

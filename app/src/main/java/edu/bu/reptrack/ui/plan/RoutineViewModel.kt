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
import edu.bu.reptrack.domain.workout.RoutineEdits
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.domain.workout.withEdits
import edu.bu.reptrack.ui.navigation.Routes
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One exercise of the routine, with its catalog entry when the catalog has it. */
data class RoutineExerciseUi(
    val name: String,
    /** From the wger catalog: muscles, equipment and a page to open. Null if it is not in the catalog. */
    val match: Exercise?,
)

/** Everything the routine screen needs to draw itself. */
data class RoutineUiState(
    val isLoading: Boolean = true,
    /** No built-in routine has this id. */
    val notFound: Boolean = false,
    val routineId: String = "",
    val name: String = "",
    val exercises: List<RoutineExerciseUi> = emptyList(),
    /** The user changed the exercises, so Reset is offered. */
    val isEdited: Boolean = false,
    /** The catalog is not saved on the phone yet, so nothing can be looked up. */
    val catalogEmpty: Boolean = false,
    /** The last change was not saved. */
    val saveFailed: Boolean = false,
) {
    /** A routine keeps at least one exercise. */
    val canRemove: Boolean get() = exercises.size > 1
}

/**
 * One routine: its exercises, each looked up in the wger catalog, and the changes the user can
 * make to it (remove, move, reset; adding and swapping happen on the catalog picker). Every
 * change saves the whole list, and the rest of the routine stays as it was.
 */
class RoutineViewModel(
    private val planRepository: PlanRepository,
    catalog: ExerciseRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    val routineId: String = savedState.get<String>(Routes.ARG_ROUTINE_ID).orEmpty()
    private val builtIn = StarterRoutines.find(routineId)

    private val saveFailed = MutableStateFlow(false)

    val uiState: StateFlow<RoutineUiState> = combine(
        planRepository.observeRoutineEdits().catch { emit(emptyMap()) },
        // Without the catalog the routine still shows, just without muscles and pages.
        catalog.observeExercises().catch { emit(emptyList()) },
        saveFailed,
    ) { edits, exercises, failed ->
        val routine = builtIn?.withEdits(edits) ?: return@combine RoutineUiState(isLoading = false, notFound = true)
        RoutineUiState(
            isLoading = false,
            routineId = routine.id,
            name = routine.name,
            exercises = routine.exercises.map { RoutineExerciseUi(it, CatalogMatch.find(it, exercises)) },
            isEdited = routine.isEdited,
            catalogEmpty = exercises.isEmpty(),
            saveFailed = failed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RoutineUiState(routineId = routineId),
    )

    fun remove(index: Int) = change { RoutineEdits.remove(it, index) }

    fun moveUp(index: Int) = change { RoutineEdits.move(it, index, -1) }

    fun moveDown(index: Int) = change { RoutineEdits.move(it, index, 1) }

    /** Back to the built-in exercises. */
    fun reset() = save { planRepository.resetRoutine(routineId) }

    private fun change(edit: (List<String>) -> List<String>) {
        val current = uiState.value.exercises.map { it.name }
        val changed = edit(current)
        if (changed == current) return
        save { planRepository.setRoutineExercises(routineId, changed) }
    }

    private fun save(block: suspend () -> Unit) {
        saveFailed.value = false
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                saveFailed.value = true
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RepTrackApplication).container
                RoutineViewModel(
                    planRepository = container.planRepository,
                    catalog = container.exerciseRepository,
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }
}

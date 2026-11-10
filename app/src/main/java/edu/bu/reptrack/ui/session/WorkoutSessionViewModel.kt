package edu.bu.reptrack.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.workout.WorkoutRepository
import edu.bu.reptrack.domain.profile.formatKg
import edu.bu.reptrack.domain.workout.LoggedSet
import edu.bu.reptrack.domain.workout.SetInput
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.domain.workout.exercisesToShow
import edu.bu.reptrack.domain.workout.totalVolumeKg
import edu.bu.reptrack.ui.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** How the session ended, so the screen knows where to go next. */
enum class SessionOutcome { FINISHED, DISCARDED }

/** One exercise card: its logged sets and what is typed in its boxes right now. */
data class ExerciseUi(
    val name: String,
    val sets: List<LoggedSet> = emptyList(),
    val weightInput: String = "",
    val repsInput: String = "",
    val weightError: Boolean = false,
    val repsError: Boolean = false,
)

/** The edit dialog for one set. */
data class EditSetUi(
    val set: LoggedSet,
    val weightInput: String,
    val repsInput: String,
    val weightError: Boolean = false,
    val repsError: Boolean = false,
)

/** Everything the workout screen needs to draw itself. */
data class WorkoutSessionUiState(
    val routineName: String = "",
    val exercises: List<ExerciseUi> = emptyList(),
    val setCount: Int = 0,
    val totalVolumeKg: Float = 0f,
    val editing: EditSetUi? = null,
    val showLeaveDialog: Boolean = false,
    val outcome: SessionOutcome? = null,
) {
    val canFinish: Boolean get() = setCount > 0
}

/**
 * Runs one workout. The session row is only created in Room when the first set is logged,
 * so opening a routine and backing out leaves nothing behind. The session id is kept in
 * [SavedStateHandle], so the workout comes back after rotation or the app being killed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionViewModel(
    private val repository: WorkoutRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val routineId: String = savedState.get<String>(Routes.ARG_ROUTINE_ID).orEmpty()
    private val routine = StarterRoutines.find(routineId)
    private val routineName: String = routine?.name.orEmpty()

    private val sessionId: StateFlow<Long?> = savedState.getStateFlow(KEY_SESSION_ID, null)

    /** Stops two quick taps from creating two sessions. */
    private val sessionLock = Mutex()

    /** Typed values and dialogs. Not saved to disk; losing a half-typed number is fine. */
    private data class Draft(
        val weight: String = "",
        val reps: String = "",
        val weightError: Boolean = false,
        val repsError: Boolean = false,
    )

    private data class LocalState(
        val drafts: Map<String, Draft> = emptyMap(),
        val editing: EditSetUi? = null,
        val showLeaveDialog: Boolean = false,
        val outcome: SessionOutcome? = null,
    )

    private val local = MutableStateFlow(LocalState())

    /** Sets of this session, live from the repository. Empty until the first set is logged. */
    private val sets: StateFlow<List<LoggedSet>> = sessionId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeSets(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<WorkoutSessionUiState> = combine(sets, local) { sets, local ->
        buildState(sets, local)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = buildState(emptyList(), LocalState()),
    )

    // ---- Typing in an exercise card ----

    fun onWeightChange(exercise: String, text: String) = updateDraft(exercise) {
        it.copy(weight = SetInput.cleanWeight(text), weightError = false)
    }

    fun onRepsChange(exercise: String, text: String) = updateDraft(exercise) {
        it.copy(reps = SetInput.cleanReps(text), repsError = false)
    }

    /** Logs a set. The typed values stay in the boxes, since the next set is often the same. */
    fun addSet(exercise: String) {
        val draft = local.value.drafts[exercise] ?: Draft()
        val parsed = SetInput.parse(draft.weight, draft.reps)
        if (parsed == null) {
            updateDraft(exercise) {
                it.copy(weightError = SetInput.isWeightBad(it.weight), repsError = SetInput.isRepsBad(it.reps))
            }
            return
        }
        viewModelScope.launch {
            val id = ensureSession()
            repository.addSet(id, exercise, parsed.weightKg, parsed.reps)
        }
    }

    // ---- Edit dialog ----

    fun onEditSet(set: LoggedSet) {
        local.update {
            it.copy(editing = EditSetUi(set, weightInput = formatKg(set.weightKg), repsInput = set.reps.toString()))
        }
    }

    fun onEditWeightChange(text: String) = updateEditing {
        it.copy(weightInput = SetInput.cleanWeight(text), weightError = false)
    }

    fun onEditRepsChange(text: String) = updateEditing {
        it.copy(repsInput = SetInput.cleanReps(text), repsError = false)
    }

    fun saveEdit() {
        val editing = local.value.editing ?: return
        val parsed = SetInput.parse(editing.weightInput, editing.repsInput)
        if (parsed == null) {
            updateEditing {
                it.copy(
                    weightError = SetInput.isWeightBad(it.weightInput),
                    repsError = SetInput.isRepsBad(it.repsInput),
                )
            }
            return
        }
        // Close the dialog right away so a second tap cannot save twice.
        local.update { it.copy(editing = null) }
        viewModelScope.launch { repository.updateSet(editing.set.id, parsed.weightKg, parsed.reps) }
    }

    fun deleteEditingSet() {
        val editing = local.value.editing ?: return
        local.update { it.copy(editing = null) }
        viewModelScope.launch { repository.deleteSet(editing.set.id) }
    }

    fun dismissEdit() {
        local.update { it.copy(editing = null) }
    }

    // ---- Finishing and leaving ----

    /** Saves the workout to history. Does nothing until at least one set is logged. */
    fun finish() {
        val id = sessionId.value ?: return
        if (sets.value.isEmpty()) return
        viewModelScope.launch {
            repository.finishSession(id)
            local.update { it.copy(outcome = SessionOutcome.FINISHED) }
        }
    }

    /** Back or close. With no sets there is nothing to lose, so it leaves straight away. */
    fun requestLeave() {
        if (sets.value.isEmpty()) discard() else local.update { it.copy(showLeaveDialog = true) }
    }

    fun dismissLeave() {
        local.update { it.copy(showLeaveDialog = false) }
    }

    /** Deletes the session and its sets, then leaves. */
    fun discard() {
        viewModelScope.launch {
            sessionId.value?.let { repository.deleteSession(it) }
            local.update { it.copy(showLeaveDialog = false, outcome = SessionOutcome.DISCARDED) }
        }
    }

    // ---- Helpers ----

    private suspend fun ensureSession(): Long = sessionLock.withLock {
        sessionId.value ?: repository.startSession(routineId.ifEmpty { null }, routineName.ifEmpty { FALLBACK_NAME })
            .also { savedState[KEY_SESSION_ID] = it }
    }

    private fun updateDraft(exercise: String, change: (Draft) -> Draft) {
        local.update { state ->
            val current = state.drafts[exercise] ?: Draft()
            state.copy(drafts = state.drafts + (exercise to change(current)))
        }
    }

    private fun updateEditing(change: (EditSetUi) -> EditSetUi) {
        local.update { state -> state.copy(editing = state.editing?.let(change)) }
    }

    private fun buildState(sets: List<LoggedSet>, local: LocalState): WorkoutSessionUiState {
        val names = exercisesToShow(routine?.exercises.orEmpty(), sets)
        val exercises = names.map { name ->
            val draft = local.drafts[name] ?: Draft()
            ExerciseUi(
                name = name,
                sets = sets.filter { it.exerciseName == name },
                weightInput = draft.weight,
                repsInput = draft.reps,
                weightError = draft.weightError,
                repsError = draft.repsError,
            )
        }
        return WorkoutSessionUiState(
            routineName = routineName,
            exercises = exercises,
            setCount = sets.size,
            totalVolumeKg = totalVolumeKg(sets),
            editing = local.editing,
            showLeaveDialog = local.showLeaveDialog,
            outcome = local.outcome,
        )
    }

    companion object {
        private const val KEY_SESSION_ID = "session_id"

        /** Used only if the routine id is unknown, which should not happen. */
        private const val FALLBACK_NAME = "Workout"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RepTrackApplication
                WorkoutSessionViewModel(app.container.workoutRepository, createSavedStateHandle())
            }
        }
    }
}

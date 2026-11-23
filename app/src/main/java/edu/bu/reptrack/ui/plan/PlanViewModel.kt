package edu.bu.reptrack.ui.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.plan.NewRoutine
import edu.bu.reptrack.data.plan.PlanRepository
import edu.bu.reptrack.data.profile.ProfileRepository
import edu.bu.reptrack.data.run.RunRepository
import edu.bu.reptrack.data.workout.WorkoutRepository
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.StarterPlan
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.workout.CustomPlan
import edu.bu.reptrack.domain.workout.PlanDay
import edu.bu.reptrack.domain.workout.PlanSnapshot
import edu.bu.reptrack.domain.workout.RoutineName
import edu.bu.reptrack.domain.workout.WorkoutTemplate
import edu.bu.reptrack.ui.navigation.Routes
import java.time.DayOfWeek
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the week plan screen needs to draw itself. */
data class PlanUiState(
    val isLoading: Boolean = true,
    val plan: StarterPlan = StarterPlan.suggest(FitnessGoal.STAY_FIT, TrainingPlace.GYM),
    val trainingDays: Set<DayOfWeek> = emptySet(),
    /** The user picked their own days. False: the plan's default days, so there is nothing to reset. */
    val customDays: Boolean = false,
    /** Monday to Sunday. */
    val week: List<PlanDay> = emptyList(),
    val routines: List<WorkoutTemplate> = emptyList(),
    /** The user made their own plan: their routines, each on the days they chose. */
    val isCustom: Boolean = false,
    /** On the user's own plan, the routine id for each training day. */
    val schedule: Map<DayOfWeek, String> = emptyMap(),
    /** The last change was not saved. */
    val saveFailed: Boolean = false,
    /** A routine was just made: the screen opens it, so the user can add exercises. */
    val createdRoutineId: String? = null,
) {
    /** A plan keeps at least one routine. */
    val canDeleteRoutine: Boolean get() = routines.size > 1
}

/**
 * The week plan: which days the user trains, and which routine goes on each day this week.
 * Uses the same [PlanSnapshot] as the Workouts tab, so both always agree on what is next.
 * The user can also make their own plan here: from the suggested one or from scratch, with
 * their own routines, each on the days they choose.
 */
class PlanViewModel(
    profileRepository: ProfileRepository,
    private val planRepository: PlanRepository,
    workouts: WorkoutRepository,
    runs: RunRepository,
    savedState: SavedStateHandle,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    /** Gym or home, as picked on the Workouts tab. Null: the profile's place. */
    private val place: TrainingPlace? = savedState.get<String>(Routes.ARG_PLACE)
        ?.let { name -> TrainingPlace.entries.firstOrNull { it.name == name } }

    private data class Local(val saveFailed: Boolean = false, val createdRoutineId: String? = null)

    private val local = MutableStateFlow(Local())

    private data class Changes(
        val edits: Map<String, List<String>>,
        val chosenDays: Set<DayOfWeek>,
        val custom: CustomPlan,
    )

    private val changes = combine(
        planRepository.observeRoutineEdits().catch { emit(emptyMap()) },
        planRepository.observeTrainingDays().catch { emit(emptySet()) },
        planRepository.observeCustomPlan().catch { emit(CustomPlan()) },
    ) { edits, chosenDays, custom -> Changes(edits, chosenDays, custom) }

    val uiState: StateFlow<PlanUiState> = combine(
        combine(
            profileRepository.profile,
            workouts.observeHistory().catch { emit(emptyList()) },
            runs.observeRuns().catch { emit(emptyList()) },
        ) { profile, history, runList -> Triple(profile, history, runList) },
        changes,
        local,
    ) { (profile, history, runList), (edits, chosenDays, custom), mine ->
        val snapshot = PlanSnapshot.of(
            goal = profile?.goal ?: FitnessGoal.STAY_FIT,
            place = place ?: profile?.place ?: TrainingPlace.GYM,
            edits = edits,
            chosenDays = chosenDays,
            history = history,
            runTimes = runList.map { it.finishedAt },
            nowMillis = now(),
            zone = zone,
            custom = custom,
        )
        PlanUiState(
            isLoading = false,
            plan = snapshot.plan,
            trainingDays = snapshot.trainingDays,
            customDays = snapshot.customDays,
            week = snapshot.week,
            routines = snapshot.routines,
            isCustom = snapshot.isCustom,
            schedule = if (snapshot.isCustom) custom.schedule.filterValues { id -> custom.routines.any { it.id == id } } else emptyMap(),
            saveFailed = mine.saveFailed,
            createdRoutineId = mine.createdRoutineId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlanUiState(),
    )

    /** Turns a training day on or off. The last day can not be turned off: a plan needs one. */
    fun toggleDay(day: DayOfWeek) {
        // The user's own plan sets its days with [setDayRoutine] instead.
        if (uiState.value.isCustom) return
        val current = uiState.value.trainingDays
        val changed = if (day in current) current - day else current + day
        if (changed.isEmpty()) return
        save { planRepository.setTrainingDays(changed) }
    }

    /** Back to the plan's default days. */
    fun resetDays() = save { planRepository.setTrainingDays(emptySet()) }

    // ---- The user's own plan ----

    /** Makes the suggested plan the user's own: the same routines and exercises, on the same days. */
    fun startFromSuggested() {
        val state = uiState.value
        if (state.isCustom || state.routines.isEmpty()) return
        val routines = state.routines.map { NewRoutine(it.name, it.exercises) }
        save { planRepository.startCustomPlan(routines, CustomPlan.rotation(routines.size, state.trainingDays)) }
    }

    /** Starts an empty plan with one routine called [name], on the current training days, and opens it. */
    fun startFromScratch(name: String) {
        val clean = RoutineName.clean(name) ?: return
        val days = uiState.value.trainingDays
        save {
            val ids = planRepository.startCustomPlan(listOf(NewRoutine(clean)), CustomPlan.rotation(1, days))
            local.update { it.copy(createdRoutineId = ids.firstOrNull()) }
        }
    }

    /** Adds an empty routine called [name] and opens it. It goes on no day until the user picks one. */
    fun addRoutine(name: String) {
        val clean = RoutineName.clean(name) ?: return
        if (!uiState.value.isCustom) return
        save {
            val id = planRepository.addRoutine(NewRoutine(clean))
            local.update { it.copy(createdRoutineId = id) }
        }
    }

    fun renameRoutine(id: String, name: String) {
        val clean = RoutineName.clean(name) ?: return
        val state = uiState.value
        if (!state.isCustom || state.routines.none { it.id == id && it.name != clean }) return
        save { planRepository.renameRoutine(id, clean) }
    }

    /** Deletes a routine; its days become days off. The last routine can not be deleted. */
    fun deleteRoutine(id: String) {
        val state = uiState.value
        if (!state.isCustom || !state.canDeleteRoutine || state.routines.none { it.id == id }) return
        val remaining = state.routines.map { it.id } - id
        save { planRepository.deleteRoutine(id, CustomPlan.scheduleAfterDelete(state.schedule, id, remaining)) }
    }

    /** Puts a routine on [day], or makes it a day off when [routineId] is null. The last training day stays. */
    fun setDayRoutine(day: DayOfWeek, routineId: String?) {
        val state = uiState.value
        if (!state.isCustom) return
        val changed = if (routineId == null) {
            state.schedule - day
        } else {
            if (state.routines.none { it.id == routineId }) return
            state.schedule + (day to routineId)
        }
        if (changed.isEmpty() || changed == state.schedule) return
        save { planRepository.setSchedule(changed) }
    }

    /** Back to the suggested plan. The user's own routines are deleted. */
    fun useSuggestedPlan() {
        if (!uiState.value.isCustom) return
        save { planRepository.clearCustomPlan() }
    }

    /** The screen opened the new routine. */
    fun onCreatedRoutineOpened() {
        local.update { it.copy(createdRoutineId = null) }
    }

    private fun save(block: suspend () -> Unit) {
        local.update { it.copy(saveFailed = false) }
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                local.update { it.copy(saveFailed = true) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RepTrackApplication).container
                PlanViewModel(
                    profileRepository = container.profileRepository,
                    planRepository = container.planRepository,
                    workouts = container.workoutRepository,
                    runs = container.runRepository,
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }
}

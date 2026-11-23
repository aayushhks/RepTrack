package edu.bu.reptrack.ui.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.plan.PlanRepository
import edu.bu.reptrack.data.profile.ProfileRepository
import edu.bu.reptrack.data.run.RunRepository
import edu.bu.reptrack.data.workout.WorkoutRepository
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.StarterPlan
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.workout.PlanDay
import edu.bu.reptrack.domain.workout.PlanSnapshot
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
    /** The last change of days was not saved. */
    val saveFailed: Boolean = false,
)

/**
 * The week plan: which days the user trains, and which routine goes on each day this week.
 * Uses the same [PlanSnapshot] as the Workouts tab, so both always agree on what is next.
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

    private val saveFailed = MutableStateFlow(false)

    val uiState: StateFlow<PlanUiState> = combine(
        combine(
            profileRepository.profile,
            workouts.observeHistory().catch { emit(emptyList()) },
            runs.observeRuns().catch { emit(emptyList()) },
        ) { profile, history, runList -> Triple(profile, history, runList) },
        planRepository.observeRoutineEdits().catch { emit(emptyMap()) },
        planRepository.observeTrainingDays().catch { emit(emptySet()) },
        saveFailed,
    ) { (profile, history, runList), edits, chosenDays, failed ->
        val snapshot = PlanSnapshot.of(
            goal = profile?.goal ?: FitnessGoal.STAY_FIT,
            place = place ?: profile?.place ?: TrainingPlace.GYM,
            edits = edits,
            chosenDays = chosenDays,
            history = history,
            runTimes = runList.map { it.finishedAt },
            nowMillis = now(),
            zone = zone,
        )
        PlanUiState(
            isLoading = false,
            plan = snapshot.plan,
            trainingDays = snapshot.trainingDays,
            customDays = snapshot.customDays,
            week = snapshot.week,
            routines = snapshot.routines,
            saveFailed = failed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlanUiState(),
    )

    /** Turns a training day on or off. The last day can not be turned off: a plan needs one. */
    fun toggleDay(day: DayOfWeek) {
        val current = uiState.value.trainingDays
        val changed = if (day in current) current - day else current + day
        if (changed.isEmpty()) return
        save { planRepository.setTrainingDays(changed) }
    }

    /** Back to the plan's default days. */
    fun resetDays() = save { planRepository.setTrainingDays(emptySet()) }

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

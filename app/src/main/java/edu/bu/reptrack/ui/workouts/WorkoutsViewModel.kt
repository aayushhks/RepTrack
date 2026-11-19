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
import edu.bu.reptrack.data.run.RunRepository
import edu.bu.reptrack.data.weight.WeightRepository
import edu.bu.reptrack.data.workout.WorkoutRepository
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.StarterPlan
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.domain.weight.WeightTrend
import edu.bu.reptrack.domain.workout.DayActivity
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.domain.workout.WeekActivity
import edu.bu.reptrack.domain.workout.WorkoutTemplate
import edu.bu.reptrack.domain.workout.nextRoutine
import edu.bu.reptrack.domain.workout.startOfWeekMillis
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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
    /** The plan's routine to do next, shown big at the top. */
    val nextRoutine: WorkoutTemplate? = routines.firstOrNull(),
    /** Monday to Sunday, with what was done on each day. */
    val week: List<DayActivity> = emptyList(),
    /** Days in a row with a workout or a run. */
    val streakDays: Int = 0,
    val workoutsThisWeek: Int = 0,
    /** From 0 to 1, from the first weigh-in to the goal. Null without a goal or weigh-ins. */
    val goalProgress: Float? = null,
) {
    /** The plan's other routines, listed under the one up next. */
    val otherRoutines: List<WorkoutTemplate> get() = routines.filter { it.id != nextRoutine?.id }
}

/**
 * Builds the Workouts tab from the saved profile, finished workouts and runs, and the weight
 * log. The user can switch between gym and home here without changing their saved profile.
 * That choice lives in [SavedStateHandle], so it survives rotation.
 */
class WorkoutsViewModel(
    profileRepository: ProfileRepository,
    private val savedState: SavedStateHandle,
    workouts: WorkoutRepository,
    runs: RunRepository,
    weights: WeightRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val chosenPlace = savedState.getStateFlow<String?>(KEY_PLACE, null)

    val uiState: StateFlow<WorkoutsUiState> = combine(
        profileRepository.profile,
        chosenPlace,
        // If history can not be read, the plan and routines still show; only the streak,
        // week strip and goal ring are left empty.
        workouts.observeHistory().catch { emit(emptyList()) },
        runs.observeRuns().catch { emit(emptyList()) },
        weights.observeWeights().catch { emit(emptyList()) },
    ) { profile, placeName, history, runList, weightList ->
        // A place picked on this screen wins over the one saved in the profile.
        val place = placeName?.let { TrainingPlace.valueOf(it) } ?: profile?.place ?: TrainingPlace.GYM
        val plan = StarterPlan.suggest(profile?.goal ?: DEFAULT_GOAL, place)
        val routines = StarterRoutines.forPlan(plan.type)

        val workoutTimes = history.map { it.session.finishedAt ?: it.session.startedAt }
        val runTimes = runList.map { it.finishedAt }
        val lastRoutineId = history
            .filter { it.session.routineId in routines.map(WorkoutTemplate::id) }
            .maxByOrNull { it.session.finishedAt ?: it.session.startedAt }
            ?.session?.routineId
        val goalProgress = profile?.let {
            val start = weightList.firstOrNull() ?: return@let null
            val latest = weightList.last()
            WeightTrend.goalProgress(start.weightKg, latest.weightKg, it.goalWeightKg)
        }

        WorkoutsUiState(
            isLoading = false,
            profile = profile,
            place = place,
            plan = plan,
            routines = routines,
            nextRoutine = nextRoutine(routines, lastRoutineId),
            week = WeekActivity.thisWeek(workoutTimes, runTimes, now(), zone),
            streakDays = WeekActivity.streakDays(workoutTimes + runTimes, now(), zone),
            workoutsThisWeek = workoutTimes.count { it >= startOfWeekMillis(now(), zone) },
            goalProgress = goalProgress,
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
                WorkoutsViewModel(
                    profileRepository = app.container.profileRepository,
                    savedState = createSavedStateHandle(),
                    workouts = app.container.workoutRepository,
                    runs = app.container.runRepository,
                    weights = app.container.weightRepository,
                )
            }
        }
    }
}

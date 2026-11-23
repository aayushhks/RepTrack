package edu.bu.reptrack.ui.workouts

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
import edu.bu.reptrack.data.weight.WeightRepository
import edu.bu.reptrack.data.workout.WorkoutRepository
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.StarterPlan
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.domain.weight.WeightTrend
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.domain.weight.WeightEntry
import edu.bu.reptrack.domain.workout.DayActivity
import edu.bu.reptrack.domain.workout.PlanDay
import edu.bu.reptrack.domain.workout.CustomPlan
import edu.bu.reptrack.domain.workout.PlanSnapshot
import edu.bu.reptrack.domain.workout.SessionSummary
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.domain.workout.WeekActivity
import edu.bu.reptrack.domain.workout.WeekPlan
import edu.bu.reptrack.domain.workout.WorkoutTemplate
import edu.bu.reptrack.domain.workout.startOfWeekMillis
import java.time.DayOfWeek
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
    /** The days the user trains: their own pick, or the plan's default days. */
    val trainingDays: Set<DayOfWeek> = WeekPlan.defaultDays(plan.daysPerWeek),
    /** Monday to Sunday: the routine planned or done each day, or a day off. */
    val weekPlan: List<PlanDay> = emptyList(),
    /** The user made their own plan, so gym or home does not change it. */
    val isCustomPlan: Boolean = false,
) {
    /** The plan's other routines, listed under the one up next. */
    val otherRoutines: List<WorkoutTemplate> get() = routines.filter { it.id != nextRoutine?.id }
}

/**
 * Builds the Workouts tab from the saved profile, finished workouts and runs, the weight log,
 * and the user's changes to their plan. The user can switch between gym and home here without
 * changing their saved profile. That choice lives in [SavedStateHandle], so it survives rotation.
 */
class WorkoutsViewModel(
    profileRepository: ProfileRepository,
    private val savedState: SavedStateHandle,
    workouts: WorkoutRepository,
    runs: RunRepository,
    weights: WeightRepository,
    planRepository: PlanRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val chosenPlace = savedState.getStateFlow<String?>(KEY_PLACE, null)

    private data class Saved(
        val profile: UserProfile?,
        val placeName: String?,
        val history: List<SessionSummary>,
        val runs: List<RunSummary>,
        val weights: List<WeightEntry>,
    )

    private val saved = combine(
        profileRepository.profile,
        chosenPlace,
        // If history can not be read, the plan and routines still show; only the streak,
        // week strip and goal ring are left empty.
        workouts.observeHistory().catch { emit(emptyList()) },
        runs.observeRuns().catch { emit(emptyList()) },
        weights.observeWeights().catch { emit(emptyList()) },
    ) { profile, placeName, history, runList, weightList -> Saved(profile, placeName, history, runList, weightList) }

    val uiState: StateFlow<WorkoutsUiState> = combine(
        saved,
        // Without the plan changes, the built-in routines and days still show.
        planRepository.observeRoutineEdits().catch { emit(emptyMap()) },
        planRepository.observeTrainingDays().catch { emit(emptySet()) },
        planRepository.observeCustomPlan().catch { emit(CustomPlan()) },
    ) { (profile, placeName, history, runList, weightList), edits, chosenDays, custom ->
        // A place picked on this screen wins over the one saved in the profile.
        val place = placeName?.let { TrainingPlace.valueOf(it) } ?: profile?.place ?: TrainingPlace.GYM
        val workoutTimes = history.map { it.session.finishedAt ?: it.session.startedAt }
        val runTimes = runList.map { it.finishedAt }
        val snapshot = PlanSnapshot.of(
            goal = profile?.goal ?: DEFAULT_GOAL,
            place = place,
            edits = edits,
            chosenDays = chosenDays,
            history = history,
            runTimes = runTimes,
            nowMillis = now(),
            zone = zone,
            custom = custom,
        )
        val goalProgress = profile?.let {
            val start = weightList.firstOrNull() ?: return@let null
            val latest = weightList.last()
            WeightTrend.goalProgress(start.weightKg, latest.weightKg, it.goalWeightKg)
        }

        WorkoutsUiState(
            isLoading = false,
            profile = profile,
            place = place,
            plan = snapshot.plan,
            routines = snapshot.routines,
            nextRoutine = snapshot.next,
            week = WeekActivity.thisWeek(workoutTimes, runTimes, now(), zone),
            streakDays = WeekActivity.streakDays(workoutTimes + runTimes, now(), zone),
            workoutsThisWeek = workoutTimes.count { it >= startOfWeekMillis(now(), zone) },
            goalProgress = goalProgress,
            trainingDays = snapshot.trainingDays,
            weekPlan = snapshot.week,
            isCustomPlan = snapshot.isCustom,
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
                    planRepository = app.container.planRepository,
                )
            }
        }
    }
}

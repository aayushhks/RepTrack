package edu.bu.reptrack.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.profile.ProfileRepository
import edu.bu.reptrack.data.run.RunRepository
import edu.bu.reptrack.data.weight.WeightRepository
import edu.bu.reptrack.data.workout.WorkoutRepository
import edu.bu.reptrack.domain.profile.FieldError
import edu.bu.reptrack.domain.profile.ProfileValidator
import edu.bu.reptrack.domain.profile.formatKg
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.domain.weight.WeightChartData
import edu.bu.reptrack.domain.weight.WeightEntry
import edu.bu.reptrack.domain.weight.WeightTrend
import edu.bu.reptrack.domain.workout.SessionSummary
import edu.bu.reptrack.domain.workout.startOfWeekMillis
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One row in the history: a finished workout or a finished walk or run. */
sealed interface HistoryItem {
    /** Unique across both kinds, for the list's keys. */
    val key: String
    val finishedAt: Long

    data class Workout(val summary: SessionSummary) : HistoryItem {
        override val key: String get() = "workout_${summary.session.id}"
        override val finishedAt: Long get() = summary.session.finishedAt ?: summary.session.startedAt
    }

    data class Run(val run: RunSummary) : HistoryItem {
        override val key: String get() = "run_${run.id}"
        override val finishedAt: Long get() = run.finishedAt
    }
}

/** What the weight card shows once there is at least one weigh-in. */
data class WeightSummary(
    val latest: WeightEntry,
    /** The very first weigh-in, where the change and the goal progress are measured from. */
    val start: WeightEntry,
    /** The oldest weigh-in on the chart, for the date and the screen reader text. */
    val chartStart: WeightEntry,
    /** Latest minus start. Negative means weight lost. Null with only one weigh-in. */
    val changeKg: Float?,
    /** Null when the user skipped goal setup. */
    val goalKg: Float?,
    /** From 0 to 1. Null without a goal, or when the start already was the goal. */
    val goalProgress: Float?,
    val chart: WeightChartData,
    /** The newest few weigh-ins, newest first, for the list under the chart. */
    val recent: List<WeightEntry>,
) {
    /** A line needs two points. With one weigh-in the card asks for another instead. */
    val hasTrend: Boolean get() = changeKg != null
}

/** The "Log weight" dialog. Kept in the ViewModel, so what was typed survives rotation. */
data class LogWeightDialog(
    val input: String = "",
    /** Shown after the user typed something or tapped Save. */
    val error: FieldError? = null,
)

/** This week's totals, from Monday: workouts finished, distance walked or run, and run calories. */
data class WeekTotals(
    val workouts: Int = 0,
    val distanceM: Double = 0.0,
    val calories: Int = 0,
)

/** Everything the Progress tab needs to draw itself. */
data class ProgressUiState(
    val isLoading: Boolean = true,
    /** Finished workouts and runs together, newest first. */
    val items: List<HistoryItem> = emptyList(),
    /** Null until the first weigh-in. */
    val weight: WeightSummary? = null,
    val week: WeekTotals = WeekTotals(),
    /** Null while the dialog is closed. */
    val logDialog: LogWeightDialog? = null,
    /** The weigh-in the user asked to delete, waiting for them to confirm. */
    val pendingDelete: WeightEntry? = null,
)

/** Workouts and runs in one list, newest first. Plain Kotlin so it is easy to test. */
fun mergeHistory(workouts: List<SessionSummary>, runs: List<RunSummary>): List<HistoryItem> =
    (workouts.map { HistoryItem.Workout(it) } + runs.map { HistoryItem.Run(it) })
        .sortedByDescending { it.finishedAt }

/** Adds up the history items finished on or after [weekStart]. */
fun weekTotals(items: List<HistoryItem>, weekStart: Long): WeekTotals {
    val thisWeek = items.filter { it.finishedAt >= weekStart }
    val runs = thisWeek.filterIsInstance<HistoryItem.Run>().map { it.run }
    return WeekTotals(
        workouts = thisWeek.count { it is HistoryItem.Workout },
        distanceM = runs.sumOf { it.distanceM },
        calories = runs.sumOf { it.calories },
    )
}

/** Builds the weight card from all weigh-ins, oldest first. Null when there are none. */
fun summarizeWeights(entries: List<WeightEntry>, goalKg: Float?): WeightSummary? {
    val chart = WeightTrend.chart(entries, goalKg) ?: return null
    val start = entries.first()
    val latest = entries.last()
    return WeightSummary(
        latest = latest,
        start = start,
        chartStart = entries.takeLast(WeightTrend.CHART_POINTS).first(),
        changeKg = WeightTrend.changeKg(entries),
        goalKg = goalKg,
        goalProgress = goalKg?.let { WeightTrend.goalProgress(start.weightKg, latest.weightKg, it) },
        chart = chart,
        recent = entries.takeLast(RECENT_COUNT).reversed(),
    )
}

/** How many weigh-ins the list under the chart shows. */
const val RECENT_COUNT = 3

/**
 * Progress tab: the weight log with its trend toward the goal, then the history of workouts
 * and runs. Room's Flows mean anything saved on another screen shows up here by itself.
 */
class ProgressViewModel(
    workouts: WorkoutRepository,
    runs: RunRepository,
    private val weights: WeightRepository,
    private val profiles: ProfileRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private data class LocalState(
        val logDialog: LogWeightDialog? = null,
        val pendingDelete: WeightEntry? = null,
    )

    private val local = MutableStateFlow(LocalState())

    val uiState: StateFlow<ProgressUiState> = combine(
        workouts.observeHistory(),
        runs.observeRuns(),
        weights.observeWeights(),
        profiles.profile,
        local,
    ) { history, runList, weightList, profile, local ->
        val items = mergeHistory(history, runList)
        ProgressUiState(
            isLoading = false,
            items = items,
            week = weekTotals(items, startOfWeekMillis(now(), zone)),
            weight = summarizeWeights(weightList, profile?.goalWeightKg),
            logDialog = local.logDialog,
            pendingDelete = local.pendingDelete,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgressUiState(),
    )

    /** Opens the dialog with the last weight filled in, so a small change is a quick edit. */
    fun openLogWeight() {
        val last = uiState.value.weight?.latest?.weightKg
        local.update { it.copy(logDialog = LogWeightDialog(input = last?.let(::formatKg) ?: "")) }
    }

    fun onWeightInput(text: String) {
        val cleaned = ProfileValidator.cleanNumber(text)
        val error = if (cleaned.isEmpty()) null else ProfileValidator.check(cleaned, ProfileValidator.WEIGHT_KG_RANGE)
        local.update { it.copy(logDialog = it.logDialog?.copy(input = cleaned, error = error)) }
    }

    fun dismissLogWeight() {
        local.update { it.copy(logDialog = null) }
    }

    /** Saves the weigh-in if the number is valid, otherwise shows why it is not. */
    fun saveWeight() {
        val dialog = local.value.logDialog ?: return
        val error = ProfileValidator.check(dialog.input, ProfileValidator.WEIGHT_KG_RANGE)
        if (error != null) {
            local.update { it.copy(logDialog = dialog.copy(error = error)) }
            return
        }
        val kg = dialog.input.trim().toFloat()
        local.update { it.copy(logDialog = null) }
        viewModelScope.launch {
            weights.addWeight(kg, now())
            syncProfileWeight(kg)
        }
    }

    /** Asks first, so a stray tap does not delete a weigh-in. */
    fun requestDelete(entry: WeightEntry) {
        local.update { it.copy(pendingDelete = entry) }
    }

    fun dismissDelete() {
        local.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val entry = local.value.pendingDelete ?: return
        local.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            weights.deleteWeight(entry.id)
            // If the newest one went, the one before it is the current weight again.
            weights.latestWeight()?.let { syncProfileWeight(it.weightKg) }
        }
    }

    /** Keeps the weight on Profile and Workouts, and the run calories, in step with the log. */
    private suspend fun syncProfileWeight(kg: Float) {
        val profile = profiles.profile.first() ?: return
        if (profile.weightKg != kg) profiles.saveProfile(profile.copy(weightKg = kg))
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RepTrackApplication).container
                ProgressViewModel(
                    workouts = container.workoutRepository,
                    runs = container.runRepository,
                    weights = container.weightRepository,
                    profiles = container.profileRepository,
                )
            }
        }
    }
}

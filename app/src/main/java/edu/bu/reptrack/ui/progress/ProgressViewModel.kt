package edu.bu.reptrack.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.run.RunRepository
import edu.bu.reptrack.data.workout.WorkoutRepository
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.domain.workout.SessionSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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

/** Everything the Progress tab needs to draw itself. */
data class ProgressUiState(
    val isLoading: Boolean = true,
    /** Finished workouts and runs together, newest first. */
    val items: List<HistoryItem> = emptyList(),
)

/** Workouts and runs in one list, newest first. Plain Kotlin so it is easy to test. */
fun mergeHistory(workouts: List<SessionSummary>, runs: List<RunSummary>): List<HistoryItem> =
    (workouts.map { HistoryItem.Workout(it) } + runs.map { HistoryItem.Run(it) })
        .sortedByDescending { it.finishedAt }

/** Progress tab. Room's Flows mean a workout or run finished on another screen shows up here by itself. */
class ProgressViewModel(workouts: WorkoutRepository, runs: RunRepository) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> =
        combine(workouts.observeHistory(), runs.observeRuns()) { history, runList ->
            ProgressUiState(isLoading = false, items = mergeHistory(history, runList))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProgressUiState(),
        )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RepTrackApplication).container
                ProgressViewModel(container.workoutRepository, container.runRepository)
            }
        }
    }
}

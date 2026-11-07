package edu.bu.reptrack.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.workout.WorkoutRepository
import edu.bu.reptrack.domain.workout.SessionSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Everything the Progress tab needs to draw itself. */
data class ProgressUiState(
    val isLoading: Boolean = true,
    /** Finished workouts, newest first. */
    val history: List<SessionSummary> = emptyList(),
)

/** Progress tab. Room's Flow means a workout finished on another screen shows up here by itself. */
class ProgressViewModel(repository: WorkoutRepository) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = repository.observeHistory()
        .map { ProgressUiState(isLoading = false, history = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProgressUiState(),
        )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RepTrackApplication
                ProgressViewModel(app.container.workoutRepository)
            }
        }
    }
}

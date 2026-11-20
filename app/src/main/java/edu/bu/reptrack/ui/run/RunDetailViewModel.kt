package edu.bu.reptrack.ui.run

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.run.RunRepository
import edu.bu.reptrack.domain.run.DayPart
import edu.bu.reptrack.domain.run.RoutePoint
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.ui.navigation.Routes
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Everything the run detail screen needs to draw itself. */
data class RunDetailUiState(
    val isLoading: Boolean = true,
    val run: RunSummary? = null,
    val route: List<RoutePoint> = emptyList(),
    /** For the name: "Morning run", "Evening walk". */
    val dayPart: DayPart = DayPart.MORNING,
    /** The run is not in the database, for example it was deleted. */
    val notFound: Boolean = false,
    val loadFailed: Boolean = false,
) {
    /** A line needs at least two points. Runs saved before routes were kept have none. */
    val hasRoute: Boolean get() = route.size >= 2
}

/**
 * A saved walk or run: its route on a map and its totals. Room's Flow means the screen would
 * update by itself if the run changed, and it notices when the run is deleted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RunDetailViewModel(
    runs: RunRepository,
    savedState: SavedStateHandle,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    /** From the navigation route, run_detail/{runId}. */
    val runId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_RUN_ID)) { "run_detail needs a run id" }

    /** Bumped by Try again, which reads the run again. */
    private val attempt = MutableStateFlow(0)

    val uiState: StateFlow<RunDetailUiState> = attempt.flatMapLatest {
        runs.observeRun(runId)
            .map { detail ->
                if (detail == null) {
                    RunDetailUiState(isLoading = false, notFound = true)
                } else {
                    RunDetailUiState(
                        isLoading = false,
                        run = detail.summary,
                        route = detail.route,
                        dayPart = DayPart.of(detail.summary.startedAt, zone),
                    )
                }
            }
            .catch { emit(RunDetailUiState(isLoading = false, loadFailed = true)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RunDetailUiState(),
    )

    fun retry() {
        attempt.update { it + 1 }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RepTrackApplication).container
                RunDetailViewModel(runs = container.runRepository, savedState = createSavedStateHandle())
            }
        }
    }
}

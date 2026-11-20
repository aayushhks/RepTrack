package edu.bu.reptrack.ui.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.profile.ProfileRepository
import edu.bu.reptrack.data.run.ActiveRun
import edu.bu.reptrack.data.run.RunRecorder
import edu.bu.reptrack.data.run.RunRepository
import edu.bu.reptrack.domain.run.RunMath
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.service.RunServiceController
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How the run screen ended, so the screen knows where to go next. */
enum class RunOutcome { SAVED, DISCARDED }

/** Everything the run screen needs to draw itself. */
data class RunUiState(
    val isRunning: Boolean = false,
    val elapsedMs: Long = 0,
    val distanceM: Double = 0.0,
    val paceSecondsPerKm: Long? = null,
    val calories: Int = 0,
    /** Running, but no good fix yet. */
    val waitingForGps: Boolean = false,
    /** Location stopped working during the run. */
    val locationFailed: Boolean = false,
    val permissionDenied: Boolean = false,
    /** Only approximate location was allowed, so fixes are too fuzzy to count. */
    val approximateOnly: Boolean = false,
    /** False when no body weight is saved, so calories use a default weight. */
    val weightKnown: Boolean = true,
    val showLeaveDialog: Boolean = false,
    val isSaving: Boolean = false,
    /** Saving failed. The run is still being recorded, so Finish can be tried again. */
    val saveFailed: Boolean = false,
    val outcome: RunOutcome? = null,
) {
    /** Like a workout needs one set, a run needs a little distance before it can be saved. */
    val canFinish: Boolean get() = isRunning && !isSaving && distanceM >= MIN_SAVE_DISTANCE_M

    companion object {
        const val MIN_SAVE_DISTANCE_M = 10.0
    }
}

/**
 * Runs the walk or run screen. The recording lives in [RunRecorder], which outlives this
 * ViewModel, so the screen can be closed and reopened while a run goes on. This ViewModel adds
 * the clock, the calories and the screen's own flags, and saves the run when it is finished.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RunViewModel(
    private val recorder: RunRecorder,
    private val runs: RunRepository,
    private val service: RunServiceController,
    profiles: ProfileRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private data class LocalState(
        val permissionDenied: Boolean = false,
        val approximateOnly: Boolean = false,
        val showLeaveDialog: Boolean = false,
        val isSaving: Boolean = false,
        val saveFailed: Boolean = false,
        val outcome: RunOutcome? = null,
    )

    private val local = MutableStateFlow(LocalState())

    /** Ticks once a second while a run is going, so the timer moves even when standing still. */
    private val clock = recorder.active.flatMapLatest { run ->
        if (run == null) {
            flowOf(0L)
        } else {
            flow {
                while (true) {
                    emit(now())
                    delay(TICK_MS)
                }
            }
        }
    }

    /** Body weight for calories. Null when the user skipped goal setup. */
    private val weightKg: StateFlow<Float?> = profiles.profile
        .map { it?.weightKg }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<RunUiState> = combine(recorder.active, clock, weightKg, local) { run, time, weight, state ->
        buildState(run, time, weight, state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RunUiState(),
    )

    /** Called after the screen checked that location is allowed. */
    fun start(approximateOnly: Boolean = false) {
        local.update { it.copy(permissionDenied = false, approximateOnly = approximateOnly) }
        recorder.start()
        service.start()
    }

    /** The answer from the permission dialog. */
    fun onPermissionResult(granted: Boolean, approximateOnly: Boolean) {
        if (granted) start(approximateOnly) else local.update { it.copy(permissionDenied = true) }
    }

    /** Saves the run to history. Does nothing until the run has some distance. */
    fun finish() {
        if (!uiState.value.canFinish) return
        val run = recorder.active.value ?: return
        val summary = run.toSummary(finishedAt = now(), weightKg = weightKg.value ?: DEFAULT_WEIGHT_KG)
        local.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                runs.saveRun(summary)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                // The recording only stops once the run is safely saved, so nothing is lost
                // and the user can tap Finish again.
                local.update { it.copy(isSaving = false, saveFailed = true) }
                return@launch
            }
            recorder.stop()
            local.update { it.copy(isSaving = false, outcome = RunOutcome.SAVED) }
        }
    }

    /** Back or close. With no run going there is nothing to lose, so it leaves straight away. */
    fun requestLeave() {
        if (recorder.active.value == null) {
            local.update { it.copy(outcome = RunOutcome.DISCARDED) }
        } else {
            local.update { it.copy(showLeaveDialog = true) }
        }
    }

    fun dismissLeave() {
        local.update { it.copy(showLeaveDialog = false) }
    }

    /** Stops the run without saving it. */
    fun discard() {
        recorder.stop()
        local.update { it.copy(showLeaveDialog = false, outcome = RunOutcome.DISCARDED) }
    }

    private fun buildState(run: ActiveRun?, time: Long, weight: Float?, state: LocalState): RunUiState {
        val elapsed = if (run == null) 0L else (time - run.startedAt).coerceAtLeast(0L)
        val distance = run?.progress?.distanceM ?: 0.0
        return RunUiState(
            isRunning = run != null,
            elapsedMs = elapsed,
            distanceM = distance,
            paceSecondsPerKm = RunMath.paceSecondsPerKm(distance, elapsed),
            calories = RunMath.caloriesKcal(weight ?: DEFAULT_WEIGHT_KG, distance, elapsed),
            waitingForGps = run != null && !run.progress.hasFix && !run.locationFailed,
            locationFailed = run?.locationFailed == true,
            permissionDenied = state.permissionDenied,
            approximateOnly = state.approximateOnly,
            weightKnown = weight != null,
            showLeaveDialog = state.showLeaveDialog,
            isSaving = state.isSaving,
            saveFailed = state.saveFailed,
            outcome = state.outcome,
        )
    }

    private fun ActiveRun.toSummary(finishedAt: Long, weightKg: Float): RunSummary {
        val duration = (finishedAt - startedAt).coerceAtLeast(0L)
        return RunSummary(
            startedAt = startedAt,
            finishedAt = finishedAt,
            distanceM = progress.distanceM,
            durationMs = duration,
            calories = RunMath.caloriesKcal(weightKg, progress.distanceM, duration),
        )
    }

    companion object {
        private const val TICK_MS = 1_000L

        /** Used for calories when the user skipped entering their weight. */
        const val DEFAULT_WEIGHT_KG = 70f

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RepTrackApplication).container
                RunViewModel(
                    recorder = container.runRecorder,
                    runs = container.runRepository,
                    service = container.runServiceController,
                    profiles = container.profileRepository,
                )
            }
        }
    }
}

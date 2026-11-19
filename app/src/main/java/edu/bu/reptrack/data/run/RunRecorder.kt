package edu.bu.reptrack.data.run

import edu.bu.reptrack.data.location.LocationTracker
import edu.bu.reptrack.domain.run.RunProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The run that is being recorded right now. */
data class ActiveRun(
    val startedAt: Long,
    val progress: RunProgress = RunProgress(),
    /** True if location stopped working, for example the permission was taken away or GPS is off. */
    val locationFailed: Boolean = false,
)

/**
 * Records one run at a time. It lives as long as the app (one per [edu.bu.reptrack.di.AppContainer]),
 * not as long as a screen, so rotating the phone or leaving the run screen does not stop a run.
 * The foreground service keeps the app alive while this records with the screen off.
 */
class RunRecorder(
    private val tracker: LocationTracker,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val _active = MutableStateFlow<ActiveRun?>(null)

    /** The current run, or null when none is being recorded. */
    val active: StateFlow<ActiveRun?> = _active.asStateFlow()

    private var job: Job? = null

    /** Starts a run. Does nothing if one is already running. */
    fun start() {
        if (_active.value != null) return
        _active.value = ActiveRun(startedAt = now())
        job = scope.launch {
            tracker.locations()
                .catch { _active.update { run -> run?.copy(locationFailed = true) } }
                .collect { point ->
                    _active.update { run -> run?.copy(progress = run.progress.add(point), locationFailed = false) }
                }
        }
    }

    /** Stops recording and returns the run as it ended, or null if none was running. */
    fun stop(): ActiveRun? {
        job?.cancel()
        job = null
        return _active.getAndUpdate { null }
    }
}

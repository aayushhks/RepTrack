package edu.bu.reptrack.ui.run.story

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
import edu.bu.reptrack.domain.run.RunDetail
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.ui.navigation.Routes
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the story screen needs to draw itself. */
data class RunStoryUiState(
    val isLoading: Boolean = true,
    /** The run could not be read, or it is not in the database any more. */
    val loadFailed: Boolean = false,
    val run: RunSummary? = null,
    val dayPart: DayPart = DayPart.MORNING,
    val hasRoute: Boolean = false,
    val options: StoryOptions = StoryOptions(),
    val isDrawing: Boolean = false,
    /** The newest card, a PNG file. Null until the first one is drawn. */
    val imagePath: String? = null,
    val drawFailed: Boolean = false,
    /** The Map look could not load the map, so the card was drawn without it. */
    val mapUnavailable: Boolean = false,
    /** Instagram did not open when asked. */
    val instagramFailed: Boolean = false,
) {
    val canShare: Boolean get() = imagePath != null && !isDrawing

    /** The Map look needs a route to center the map on, so runs without one only get the other two. */
    val styles: List<StoryStyle>
        get() = if (hasRoute) StoryStyle.entries else listOf(StoryStyle.DARK, StoryStyle.LIME)
}

/**
 * The share screen: draws the run as a story card and draws it again whenever the user picks
 * another look or turns "Hide start and end" on or off. Only the newest request is drawn; an
 * older one still drawing is cancelled. The choices live in [SavedStateHandle], so they
 * survive the app being closed in the background.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RunStoryViewModel(
    runs: RunRepository,
    private val maker: StoryImageMaker,
    private val savedState: SavedStateHandle,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    /** From the navigation route, run_story/{runId}. */
    val runId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_RUN_ID)) { "run_story needs a run id" }

    private sealed interface Loaded {
        data object Loading : Loaded
        data object Failed : Loaded
        data class Ready(val detail: RunDetail) : Loaded
    }

    private data class Drawing(
        val isDrawing: Boolean = false,
        val imagePath: String? = null,
        val failed: Boolean = false,
        val mapUnavailable: Boolean = false,
    )

    /** Bumped by Try again after the run could not be read. */
    private val loadAttempt = MutableStateFlow(0)

    /** Bumped by Try again after a card could not be drawn. */
    private val drawAttempt = MutableStateFlow(0)

    private val drawing = MutableStateFlow(Drawing())
    private val instagramFailed = MutableStateFlow(false)

    private val loaded: StateFlow<Loaded> = loadAttempt.flatMapLatest {
        runs.observeRun(runId)
            .map { detail -> if (detail == null) Loaded.Failed else Loaded.Ready(detail) }
            .catch { emit(Loaded.Failed) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Loaded.Loading)

    private val picked = combine(
        savedState.getStateFlow(KEY_STYLE, StoryStyle.MAP.name),
        savedState.getStateFlow(KEY_HIDE_ENDS, true),
    ) { style, hideEnds ->
        StoryOptions(style = StoryStyle.entries.firstOrNull { it.name == style } ?: StoryStyle.MAP, hideEnds = hideEnds)
    }

    init {
        viewModelScope.launch {
            combine(
                loaded.filterIsInstance<Loaded.Ready>().map { it.detail }.distinctUntilChanged(),
                picked,
                drawAttempt,
            ) { detail, options, _ ->
                StoryRequest(detail, DayPart.of(detail.summary.startedAt, zone), options.allowedFor(detail))
            }.collectLatest { request -> draw(request) }
        }
    }

    val uiState: StateFlow<RunStoryUiState> = combine(loaded, picked, drawing, instagramFailed) { load, options, draw, igFailed ->
        when (load) {
            Loaded.Loading -> RunStoryUiState()
            Loaded.Failed -> RunStoryUiState(isLoading = false, loadFailed = true)
            is Loaded.Ready -> RunStoryUiState(
                isLoading = false,
                run = load.detail.summary,
                dayPart = DayPart.of(load.detail.summary.startedAt, zone),
                hasRoute = load.detail.hasRoute,
                options = options.allowedFor(load.detail),
                isDrawing = draw.isDrawing,
                imagePath = draw.imagePath,
                drawFailed = draw.failed,
                mapUnavailable = draw.mapUnavailable,
                instagramFailed = igFailed,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RunStoryUiState(),
    )

    fun selectStyle(style: StoryStyle) {
        instagramFailed.value = false
        savedState[KEY_STYLE] = style.name
    }

    fun setHideEnds(hide: Boolean) {
        instagramFailed.value = false
        savedState[KEY_HIDE_ENDS] = hide
    }

    fun retry() {
        if (loaded.value == Loaded.Failed) loadAttempt.update { it + 1 } else drawAttempt.update { it + 1 }
    }

    /** The screen could not open Instagram. It says so and points to More apps. */
    fun onInstagramFailed() {
        instagramFailed.value = true
    }

    private suspend fun draw(request: StoryRequest) {
        // The old card stays on screen, dimmed, while the new one is drawn.
        drawing.update { it.copy(isDrawing = true, failed = false) }
        try {
            val image = maker.make(request)
            drawing.value = Drawing(imagePath = image.path, mapUnavailable = image.mapUnavailable)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            drawing.update { it.copy(isDrawing = false, failed = true) }
        }
    }

    /** Without a route there is nothing to center a map on, so the Map look becomes Dark. */
    private fun StoryOptions.allowedFor(detail: RunDetail): StoryOptions =
        if (!detail.hasRoute && style == StoryStyle.MAP) copy(style = StoryStyle.DARK) else this

    companion object {
        private const val KEY_STYLE = "story_style"
        private const val KEY_HIDE_ENDS = "story_hide_ends"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RepTrackApplication).container
                RunStoryViewModel(
                    runs = container.runRepository,
                    maker = container.storyImageMaker,
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }
}

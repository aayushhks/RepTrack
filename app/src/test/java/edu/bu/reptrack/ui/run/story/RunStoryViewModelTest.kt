package edu.bu.reptrack.ui.run.story

import androidx.lifecycle.SavedStateHandle
import edu.bu.reptrack.domain.run.RoutePoint
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.testing.FakeRunRepository
import edu.bu.reptrack.testing.FakeStoryImageMaker
import edu.bu.reptrack.testing.MainDispatcherRule
import edu.bu.reptrack.ui.navigation.Routes
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RunStoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val runs = FakeRunRepository()
    private val maker = FakeStoryImageMaker()
    private val savedState = SavedStateHandle()

    private val route = listOf(RoutePoint(42.35, -71.10), RoutePoint(42.36, -71.09))

    private suspend fun saveRun(route: List<RoutePoint> = this.route): Long = runs.saveRun(
        RunSummary(startedAt = 0, finishedAt = 26 * 60_000L, distanceM = 5_020.0, durationMs = 26 * 60_000L, calories = 410),
        route,
    )

    private fun TestScope.open(runId: Long): RunStoryViewModel {
        savedState[Routes.ARG_RUN_ID] = runId
        val viewModel = RunStoryViewModel(runs, maker, savedState, ZoneOffset.UTC)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun opening_drawsTheMapLook_withTheEndsHidden() = runTest {
        val viewModel = open(saveRun())

        val request = maker.requests.single()
        assertEquals(StoryStyle.MAP, request.options.style)
        assertTrue(request.options.hideEnds)
        assertEquals(route, request.detail.route)

        val state = viewModel.uiState.value
        assertEquals("/cache/stories/story_1.png", state.imagePath)
        assertTrue(state.canShare)
        assertEquals(StoryStyle.entries, state.styles)
    }

    @Test
    fun whileDrawing_sharingWaits() = runTest {
        maker.gate = CompletableDeferred()
        val viewModel = open(saveRun())

        assertTrue(viewModel.uiState.value.isDrawing)
        assertFalse(viewModel.uiState.value.canShare)

        maker.finish()
        assertFalse(viewModel.uiState.value.isDrawing)
        assertTrue(viewModel.uiState.value.canShare)
    }

    @Test
    fun pickingALook_drawsAgain_andIsRemembered() = runTest {
        val viewModel = open(saveRun())

        viewModel.selectStyle(StoryStyle.LIME)

        assertEquals(StoryStyle.LIME, maker.requests.last().options.style)
        assertEquals(StoryStyle.LIME, viewModel.uiState.value.options.style)
        assertEquals("/cache/stories/story_2.png", viewModel.uiState.value.imagePath)
        // Kept in the SavedStateHandle, so a new ViewModel after the app was closed in the
        // background draws the same look.
        RunStoryViewModel(runs, maker, savedState, ZoneOffset.UTC)
        assertEquals(StoryStyle.LIME, maker.requests.last().options.style)
        assertEquals(3, maker.requests.size)
    }

    @Test
    fun showingTheEnds_drawsAgain() = runTest {
        val viewModel = open(saveRun())
        viewModel.setHideEnds(false)
        assertFalse(maker.requests.last().options.hideEnds)
        assertFalse(viewModel.uiState.value.options.hideEnds)
    }

    @Test
    fun runWithoutARoute_getsNoMapLook() = runTest {
        val viewModel = open(saveRun(route = emptyList()))

        assertEquals(StoryStyle.DARK, maker.requests.single().options.style)
        assertEquals(listOf(StoryStyle.DARK, StoryStyle.LIME), viewModel.uiState.value.styles)
        assertFalse(viewModel.uiState.value.hasRoute)
    }

    @Test
    fun mapThatCouldNotLoad_isReported() = runTest {
        maker.mapUnavailable = true
        val viewModel = open(saveRun())
        assertTrue(viewModel.uiState.value.mapUnavailable)
    }

    @Test
    fun drawingFailure_offersTryAgain() = runTest {
        maker.fail = true
        val viewModel = open(saveRun())
        assertTrue(viewModel.uiState.value.drawFailed)
        assertNull(viewModel.uiState.value.imagePath)

        maker.fail = false
        viewModel.retry()

        assertFalse(viewModel.uiState.value.drawFailed)
        assertEquals(2, maker.requests.size)
        assertTrue(viewModel.uiState.value.canShare)
    }

    @Test
    fun missingRun_saysSo_andDrawsNothing() = runTest {
        val viewModel = open(runId = 42)
        assertTrue(viewModel.uiState.value.loadFailed)
        assertTrue(maker.requests.isEmpty())
    }

    @Test
    fun instagramFailure_isShown_untilTheNextChoice() = runTest {
        val viewModel = open(saveRun())
        viewModel.onInstagramFailed()
        assertTrue(viewModel.uiState.value.instagramFailed)

        viewModel.selectStyle(StoryStyle.DARK)
        assertFalse(viewModel.uiState.value.instagramFailed)
    }
}

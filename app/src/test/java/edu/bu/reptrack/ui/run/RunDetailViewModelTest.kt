package edu.bu.reptrack.ui.run

import androidx.lifecycle.SavedStateHandle
import edu.bu.reptrack.domain.run.DayPart
import edu.bu.reptrack.domain.run.RoutePoint
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.testing.FakeRunRepository
import edu.bu.reptrack.testing.MainDispatcherRule
import edu.bu.reptrack.ui.navigation.Routes
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RunDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val runs = FakeRunRepository()
    private val boston = ZoneId.of("America/New_York")

    /** 7:42 in the morning in Boston, so the run is a "Morning run". */
    private val sevenFortyTwo = ZonedDateTime.of(2026, 11, 19, 7, 42, 0, 0, boston).toInstant().toEpochMilli()

    private val route = listOf(RoutePoint(42.35, -71.10), RoutePoint(42.36, -71.09), RoutePoint(42.37, -71.10))

    private fun TestScope.open(runId: Long): RunDetailViewModel {
        val viewModel = RunDetailViewModel(runs, SavedStateHandle(mapOf(Routes.ARG_RUN_ID to runId)), boston)
        // uiState only updates while someone collects it, like the screen does.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private suspend fun saveRun(route: List<RoutePoint>): Long = runs.saveRun(
        RunSummary(startedAt = sevenFortyTwo, finishedAt = sevenFortyTwo + 26 * 60_000L, distanceM = 5_020.0, durationMs = 26 * 60_000L, calories = 410),
        route,
    )

    @Test
    fun savedRun_showsItsTotals_route_andName() = runTest {
        val id = saveRun(route)
        val viewModel = open(id)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(5_020.0, state.run!!.distanceM, 0.0)
        assertEquals(route, state.route)
        assertTrue(state.hasRoute)
        assertEquals(DayPart.MORNING, state.dayPart)
    }

    @Test
    fun runSavedBeforeRoutesWereKept_hasNoRoute() = runTest {
        val id = saveRun(emptyList())
        val state = open(id).uiState.value
        assertFalse(state.hasRoute)
        assertEquals(410, state.run!!.calories)
    }

    @Test
    fun unknownRun_saysItIsGone() = runTest {
        val state = open(runId = 99).uiState.value
        assertTrue(state.notFound)
        assertFalse(state.isLoading)
    }

    @Test
    fun readFailure_offersTryAgain_whichWorksOnceTheDatabaseIsBack() = runTest {
        val id = saveRun(route)
        runs.failReads = true
        val viewModel = open(id)
        assertTrue(viewModel.uiState.value.loadFailed)

        runs.failReads = false
        viewModel.retry()

        assertFalse(viewModel.uiState.value.loadFailed)
        assertEquals(route, viewModel.uiState.value.route)
    }
}

package edu.bu.reptrack.ui.run

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.domain.run.DayPart
import edu.bu.reptrack.domain.run.RoutePoint
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.ui.theme.RepTrackTheme
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for a saved run on its map, with a plain box where the real map would be. */
@RunWith(AndroidJUnit4::class)
class RunDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var backed = false
    private var shared = false
    private var retried = false

    private val boston = ZoneId.of("America/New_York")
    private val start = ZonedDateTime.of(2026, 11, 19, 7, 42, 0, 0, boston).toInstant().toEpochMilli()

    // 5.02 km in 26 minutes: a run, at 5:11 per km.
    private val run = RunSummary(id = 4, startedAt = start, finishedAt = start + 26 * 60_000L, distanceM = 5_020.0, durationMs = 26 * 60_000L, calories = 410)
    private val route = listOf(RoutePoint(42.35, -71.10), RoutePoint(42.36, -71.09), RoutePoint(42.37, -71.10))
    private val loaded = RunDetailUiState(isLoading = false, run = run, route = route, dayPart = DayPart.MORNING)

    private fun show(state: RunDetailUiState) {
        composeRule.setContent {
            RepTrackTheme {
                RunDetailScreen(
                    state = state,
                    onBack = { backed = true },
                    onShare = { shared = true },
                    onRetry = { retried = true },
                    zone = boston,
                    map = { _, mapModifier -> Box(mapModifier) },
                )
            }
        }
    }

    @Test
    fun savedRun_showsItsName_start_map_andTotals() {
        show(loaded)

        composeRule.onNodeWithTag("run_detail_title").assertTextEquals("MORNING RUN")
        composeRule.onNodeWithText("Thu, Nov 19", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Map of the route, from start to finish").assertIsDisplayed()
        // The numbers sit inside blocks TalkBack reads as one, so look them up in the unmerged tree.
        composeRule.onNodeWithTag("run_detail_distance", useUnmergedTree = true).assertTextEquals("5.02")
        composeRule.onNodeWithTag("run_detail_time", useUnmergedTree = true).assertTextEquals("26:00")
        composeRule.onNodeWithTag("run_detail_pace", useUnmergedTree = true).assertTextEquals("5:11 /km")
        composeRule.onNodeWithTag("run_detail_calories", useUnmergedTree = true).assertTextEquals("410 kcal")
    }

    @Test
    fun shareAndBack_reportTheTaps() {
        show(loaded)

        composeRule.onNodeWithTag("run_share").performScrollTo().performClick()
        composeRule.onNodeWithTag("run_detail_back").performClick()

        assertTrue(shared)
        assertTrue(backed)
    }

    @Test
    fun runFromBeforeRoutesWereKept_explainsWhyThereIsNoMap() {
        show(loaded.copy(route = emptyList()))

        composeRule.onNodeWithTag("run_no_route").assertIsDisplayed()
        composeRule.onNodeWithTag("run_map").assertDoesNotExist()
        composeRule.onNodeWithTag("run_share").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadFailure_offersTryAgain() {
        show(RunDetailUiState(isLoading = false, loadFailed = true))

        composeRule.onNodeWithText("Try again").performClick()

        assertTrue(retried)
    }

    @Test
    fun missingRun_saysSo_andBackStillWorks() {
        show(RunDetailUiState(isLoading = false, notFound = true))

        composeRule.onNodeWithTag("run_detail_not_found").assertIsDisplayed()
        composeRule.onNodeWithTag("run_detail_back").performClick()
        assertTrue(backed)
    }
}

package edu.bu.reptrack.ui.run

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.ui.theme.RepTrackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the walk or run screen, drawn from a fixed state. */
@RunWith(AndroidJUnit4::class)
class RunScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var started = false
    private var finished = false
    private var closed = false
    private var settingsOpened = false

    // The numbers sit inside blocks that TalkBack reads as one ("Time, 12:34"), so the tests
    // look them up in the unmerged tree.
    private fun show(state: RunUiState) {
        composeRule.setContent {
            RepTrackTheme {
                RunScreen(
                    state = state,
                    onStart = { started = true },
                    onFinish = { finished = true },
                    onClose = { closed = true },
                    onOpenSettings = { settingsOpened = true },
                )
            }
        }
    }

    @Test
    fun ready_showsStart_andReportsTheTap() {
        show(RunUiState())

        composeRule.onNodeWithTag("run_distance", useUnmergedTree = true).assertTextEquals("0.00")
        composeRule.onNodeWithTag("run_start").performClick()

        assertTrue(started)
    }

    @Test
    fun running_showsLiveNumbers() {
        show(RunUiState(isRunning = true, elapsedMs = 754_000, distanceM = 2_410.0, paceSecondsPerKm = 313, calories = 182))

        composeRule.onNodeWithTag("run_distance", useUnmergedTree = true).assertTextEquals("2.41")
        composeRule.onNodeWithTag("run_time", useUnmergedTree = true).assertTextEquals("12:34")
        composeRule.onNodeWithTag("run_pace", useUnmergedTree = true).assertTextEquals("5:13 /km")
        composeRule.onNodeWithTag("run_calories", useUnmergedTree = true).assertTextEquals("182 kcal")
        composeRule.onNodeWithTag("run_finish").assertIsEnabled().performClick()

        assertTrue(finished)
    }

    @Test
    fun waitingForGps_saysSo_andCannotFinishYet() {
        show(RunUiState(isRunning = true, waitingForGps = true))

        composeRule.onNodeWithText("Waiting for GPS…").assertIsDisplayed()
        composeRule.onNodeWithTag("run_finish").assertIsNotEnabled()
    }

    @Test
    fun discard_asksTheScreenToLeave() {
        show(RunUiState(isRunning = true, distanceM = 500.0))

        composeRule.onNodeWithTag("run_discard").performClick()

        assertTrue(closed)
    }

    @Test
    fun permissionDenied_offersSettings() {
        show(RunUiState(permissionDenied = true))

        composeRule.onNodeWithText("Location is needed to track a run").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("run_open_settings").performScrollTo().performClick()

        assertTrue(settingsOpened)
    }
}

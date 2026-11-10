package edu.bu.reptrack.ui.progress

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.domain.workout.SessionSummary
import edu.bu.reptrack.domain.workout.WorkoutSession
import edu.bu.reptrack.ui.theme.RepTrackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the Progress tab, drawn from a fixed state. */
@RunWith(AndroidJUnit4::class)
class ProgressScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var startClicked = false

    private fun show(state: ProgressUiState) {
        composeRule.setContent {
            RepTrackTheme { ProgressScreen(state = state, onStartWorkout = { startClicked = true }) }
        }
    }

    @Test
    fun noHistory_offersToStartAWorkout() {
        show(ProgressUiState(isLoading = false))

        composeRule.onNodeWithText("No workouts yet").assertIsDisplayed()
        composeRule.onNodeWithText("Start a workout").performClick()

        assertTrue(startClicked)
    }

    @Test
    fun history_showsNameDurationAndTotals() {
        val session = WorkoutSession(7, "upper", "Upper body", startedAt = 0, finishedAt = 52 * 60_000L)
        show(
            ProgressUiState(
                isLoading = false,
                history = listOf(SessionSummary(session, exerciseCount = 2, setCount = 3, totalVolumeKg = 1200f)),
            ),
        )

        composeRule.onNodeWithText("Upper body").assertIsDisplayed()
        composeRule.onNodeWithText("52 min").assertIsDisplayed()
        composeRule.onNodeWithTag("history_stats_7").assertTextEquals("2 exercises, 3 sets, 1200 kg volume")
    }
}

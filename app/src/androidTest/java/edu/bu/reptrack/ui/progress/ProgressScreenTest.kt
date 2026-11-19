package edu.bu.reptrack.ui.progress

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.domain.profile.FieldError
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.domain.weight.WeightEntry
import edu.bu.reptrack.domain.workout.SessionSummary
import edu.bu.reptrack.domain.workout.WorkoutSession
import edu.bu.reptrack.ui.theme.RepTrackTheme
import org.junit.Assert.assertEquals
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
    private var logClicked = false
    private var typed: String? = null
    private var saveClicked = false
    private var deleteAsked: WeightEntry? = null
    private var deleteConfirmed = false
    private var retried = false
    private var openedRun: Long? = null

    private fun show(state: ProgressUiState) {
        composeRule.setContent {
            RepTrackTheme {
                ProgressScreen(
                    state = state,
                    onStartWorkout = { startClicked = true },
                    onLogWeight = { logClicked = true },
                    onWeightInput = { typed = it },
                    onSaveWeight = { saveClicked = true },
                    onDismissLogWeight = {},
                    onDeleteWeight = { deleteAsked = it },
                    onConfirmDelete = { deleteConfirmed = true },
                    onDismissDelete = {},
                    onRetry = { retried = true },
                    onDismissMessage = {},
                    onOpenRun = { openedRun = it },
                )
            }
        }
    }

    // Weigh-ins four days apart, from November 9, 2026.
    private val day = 24 * 60 * 60_000L
    private val weighIns = listOf(
        WeightEntry(1, 1_794_240_000_000, 84f),
        WeightEntry(2, 1_794_240_000_000 + 4 * day, 83.1f),
        WeightEntry(3, 1_794_240_000_000 + 8 * day, 82.4f),
    )

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
                items = listOf(HistoryItem.Workout(SessionSummary(session, exerciseCount = 2, setCount = 3, totalVolumeKg = 1200f))),
            ),
        )

        composeRule.onNodeWithText("Upper body").assertIsDisplayed()
        composeRule.onNodeWithText("52 min").assertIsDisplayed()
        composeRule.onNodeWithTag("history_stats_7").assertTextEquals("2 exercises, 3 sets, 1200 kg volume")
    }

    @Test
    fun run_showsDistancePaceAndCalories() {
        // 5.02 km in 26 min: a run, at 5:11 per km.
        val run = RunSummary(id = 4, startedAt = 0, finishedAt = 26 * 60_000L, distanceM = 5_020.0, durationMs = 26 * 60_000L, calories = 410)
        show(ProgressUiState(isLoading = false, items = listOf(HistoryItem.Run(run))))

        composeRule.onNodeWithText("Run").assertIsDisplayed()
        composeRule.onNodeWithText("26:00").assertIsDisplayed()
        composeRule.onNodeWithTag("run_stats_4", useUnmergedTree = true).assertTextEquals("5.02 km, 5:11 /km pace, 410 kcal")
    }

    @Test
    fun tappingARun_opensItOnItsMap() {
        val run = RunSummary(id = 4, startedAt = 0, finishedAt = 26 * 60_000L, distanceM = 5_020.0, durationMs = 26 * 60_000L, calories = 410)
        show(ProgressUiState(isLoading = false, items = listOf(HistoryItem.Run(run))))

        composeRule.onNodeWithTag("run_4").performClick()

        assertEquals(4L, openedRun)
    }

    @Test
    fun noWeighIns_offersToLogTheFirst() {
        show(ProgressUiState(isLoading = false))

        composeRule.onNodeWithText("No weigh-ins yet").assertIsDisplayed()
        composeRule.onNodeWithTag("weight_log").performClick()

        assertTrue(logClicked)
    }

    @Test
    fun weighIns_showWeightChange_chartAndGoal() {
        show(ProgressUiState(isLoading = false, weight = summarizeWeights(weighIns, goalKg = 78f)))

        composeRule.onNodeWithTag("weight_current", useUnmergedTree = true).assertTextEquals("82.4 kg")
        composeRule.onNodeWithText("1.6 kg down since", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("weight_goal").assertTextEquals("Goal 78 kg, 4.4 kg to lose")
        composeRule.onNodeWithText("27% of the way").assertIsDisplayed()
        // TalkBack reads the chart as one sentence instead of a drawing.
        composeRule.onNode(hasContentDescription("Weight chart, from 84 kg", substring = true)).assertIsDisplayed()
    }

    @Test
    fun oneWeighIn_asksForAnother_andDrawsNoChart() {
        show(ProgressUiState(isLoading = false, weight = summarizeWeights(weighIns.take(1), goalKg = null)))

        composeRule.onNodeWithText("Log again on another day to see your trend.").assertIsDisplayed()
        composeRule.onNodeWithTag("weight_chart").assertDoesNotExist()
        composeRule.onNodeWithTag("weight_goal").assertDoesNotExist()
    }

    @Test
    fun logDialog_reportsTyping_andShowsTheError() {
        show(
            ProgressUiState(
                isLoading = false,
                logDialog = LogWeightDialog(input = "8", error = FieldError.OUT_OF_RANGE),
            ),
        )

        composeRule.onNodeWithText("Enter a weight between 30 and 300 kg").assertIsDisplayed()
        composeRule.onNodeWithTag("weight_input").performTextReplacement("81")
        composeRule.onNodeWithTag("weight_save").performClick()

        assertEquals("81", typed)
        assertTrue(saveClicked)
    }

    @Test
    fun deleteButton_saysWhichWeighIn() {
        val weight = summarizeWeights(weighIns, goalKg = null)
        show(ProgressUiState(isLoading = false, weight = weight))

        // The date in the label depends on the phone's time zone, so only the start is checked.
        composeRule.onNode(hasContentDescription("Delete 82.4 kg from", substring = true)).performClick()

        assertEquals(3L, deleteAsked?.id)
    }

    @Test
    fun deleteDialog_confirms() {
        show(ProgressUiState(isLoading = false, pendingDelete = weighIns.last()))

        composeRule.onNodeWithText("Delete this weigh-in?").assertIsDisplayed()
        composeRule.onNodeWithTag("weight_delete_confirm").performClick()

        assertTrue(deleteConfirmed)
    }

    @Test
    fun weekTotals_showWorkoutsDistanceAndCalories() {
        show(ProgressUiState(isLoading = false, week = WeekTotals(workouts = 3, distanceM = 12_400.0, calories = 1_240)))

        composeRule.onNodeWithText("12.40 km").assertIsDisplayed()
        composeRule.onNodeWithText("1240 kcal").assertIsDisplayed()
    }

    @Test
    fun loadFailure_saysWhat_andOffersTryAgain() {
        show(ProgressUiState(isLoading = false, loadFailed = true))

        composeRule.onNodeWithText("Your history could not be loaded", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()

        assertTrue(retried)
    }

    @Test
    fun failedSave_isShownAboveTheWeightCard() {
        show(ProgressUiState(isLoading = false, message = ProgressMessage.WEIGHT_NOT_SAVED))

        composeRule.onNodeWithText("Your weight was not saved. Please log it again.").assertIsDisplayed()
    }
}

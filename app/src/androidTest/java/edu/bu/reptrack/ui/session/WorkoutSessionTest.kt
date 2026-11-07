package edu.bu.reptrack.ui.session

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.fakes.FakeWorkoutRepository
import edu.bu.reptrack.ui.theme.RepTrackTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for logging a workout. Uses a fake repository, so the real database is never touched. */
@RunWith(AndroidJUnit4::class)
class WorkoutSessionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val repository = FakeWorkoutRepository()
    private val finished = AtomicBoolean(false)
    private val left = AtomicBoolean(false)

    private fun showSession() {
        composeRule.setContent {
            val viewModel = remember {
                WorkoutSessionViewModel(repository, SavedStateHandle(mapOf("routineId" to "full_body_a")))
            }
            RepTrackTheme {
                WorkoutSessionRoute(
                    onFinished = { finished.set(true) },
                    onLeave = { left.set(true) },
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun logSquat(weight: String, reps: String) {
        composeRule.onNodeWithTag("weight_Squat").performScrollTo().performTextInput(weight)
        composeRule.onNodeWithTag("reps_Squat").performScrollTo().performTextInput(reps)
        composeRule.onNodeWithTag("add_Squat").performScrollTo().performClick()
    }

    @Test
    fun loggingASet_showsIt_andFinishSavesTheWorkout() {
        showSession()
        composeRule.onNodeWithText("Full body A").assertIsDisplayed()
        composeRule.onNodeWithTag("finish_button").assertIsNotEnabled()

        logSquat("60", "10")

        composeRule.onNodeWithText("60 kg × 10").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1 set, 600 kg total volume").assertIsDisplayed()

        composeRule.onNodeWithTag("finish_button").assertIsEnabled().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { finished.get() }
        assertNotNull(repository.allSessions.single().finishedAt)
    }

    @Test
    fun tappingASet_letsYouEditIt() {
        showSession()
        logSquat("60", "10")

        composeRule.onNodeWithTag("set_Squat_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("edit_reps").performTextClearance()
        composeRule.onNodeWithTag("edit_reps").performTextInput("12")
        composeRule.onNodeWithTag("edit_save").performClick()

        composeRule.onNodeWithText("60 kg × 12").performScrollTo().assertIsDisplayed()
        assertEquals(12, repository.allSets.single().reps)
    }

    @Test
    fun missingReps_showsAnError() {
        showSession()

        composeRule.onNodeWithTag("weight_Squat").performScrollTo().performTextInput("60")
        composeRule.onNodeWithTag("add_Squat").performScrollTo().performClick()

        composeRule.onNodeWithText("Enter reps from 1 to 100", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(repository.allSets.isEmpty())
    }

    @Test
    fun closingWithSets_asksFirst_thenDiscardDeletesTheWorkout() {
        showSession()
        logSquat("60", "10")

        composeRule.onNodeWithTag("close_button").performScrollTo().performClick()
        composeRule.onNodeWithText("Leave this workout?").assertIsDisplayed()
        composeRule.onNodeWithTag("leave_discard").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { left.get() }
        assertTrue(repository.allSessions.isEmpty())
    }
}

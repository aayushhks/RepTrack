package edu.bu.reptrack.ui.setup

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.fakes.FakeProfileRepository
import edu.bu.reptrack.ui.theme.RepTrackTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the goal setup screen, using a fake repository. */
@RunWith(AndroidJUnit4::class)
class GoalSetupTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val repository = FakeProfileRepository()
    private val finished = AtomicBoolean(false)

    private fun showSetup() {
        composeRule.setContent {
            val viewModel = remember { GoalSetupViewModel(repository, SavedStateHandle()) }
            RepTrackTheme {
                GoalSetupRoute(onFinished = { finished.set(true) }, viewModel = viewModel)
            }
        }
    }

    @Test
    fun fillingTheForm_savesProfile_andFinishes() {
        showSetup()
        composeRule.onNodeWithText("Start my plan").performScrollTo().assertIsNotEnabled()

        composeRule.onNodeWithTag("field_height").performScrollTo().performTextInput("178")
        composeRule.onNodeWithTag("field_weight").performScrollTo().performTextInput("82")
        composeRule.onNodeWithTag("field_goal_weight").performScrollTo().performTextInput("76")
        composeRule.onNodeWithText("Build muscle").performScrollTo().performClick()

        composeRule.onNodeWithTag("plan_summary")
            .performScrollTo()
            .assertTextEquals("Upper and lower split, 4 days a week")

        composeRule.onNodeWithText("Start my plan").performScrollTo().assertIsEnabled().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { finished.get() }
        assertEquals(76f, repository.savedProfile?.goalWeightKg)
        assertTrue(repository.setupDone)
    }

    @Test
    fun heightOutOfRange_showsError_andBlocksSave() {
        showSetup()

        composeRule.onNodeWithTag("field_height").performScrollTo().performTextInput("90")
        composeRule.onNodeWithTag("field_weight").performScrollTo().performTextInput("82")
        composeRule.onNodeWithTag("field_goal_weight").performScrollTo().performTextInput("76")

        composeRule.onNodeWithText("Enter a height between 100 and 250 cm", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Start my plan").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun skip_finishesWithoutSavingAProfile() {
        showSetup()

        composeRule.onNodeWithText("Skip for now").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { finished.get() }
        assertNull(repository.savedProfile)
        assertTrue(repository.setupDone)
    }
}

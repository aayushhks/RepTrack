package edu.bu.reptrack

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.ui.RepTrackApp
import edu.bu.reptrack.ui.theme.RepTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI test: the app opens on Workouts and each tab shows its screen. */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun opensOnWorkouts_andTabsSwitchScreens() {
        composeRule.setContent {
            RepTrackTheme { RepTrackApp() }
        }

        composeRule.onNodeWithTag("screen_workouts").assertIsDisplayed()

        composeRule.onNodeWithText("Progress").performClick()
        composeRule.onNodeWithTag("screen_progress").assertIsDisplayed()

        composeRule.onNodeWithText("Exercises").performClick()
        composeRule.onNodeWithTag("screen_exercises").assertIsDisplayed()
    }
}

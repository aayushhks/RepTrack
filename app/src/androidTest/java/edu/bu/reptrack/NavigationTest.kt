package edu.bu.reptrack

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.ui.RepTrackNavHost
import edu.bu.reptrack.ui.navigation.Routes
import edu.bu.reptrack.ui.navigation.TopLevelDestination
import edu.bu.reptrack.ui.theme.RepTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for navigation. We start the nav host directly with a fixed start
 * screen, so the result does not depend on what is saved on the test device.
 */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun opensOnWorkouts_andTabsSwitchScreens() {
        composeRule.setContent {
            RepTrackTheme { RepTrackNavHost(startRoute = TopLevelDestination.START.route) }
        }

        composeRule.onNodeWithTag("screen_workouts").assertIsDisplayed()

        composeRule.onNodeWithText("Progress").performClick()
        composeRule.onNodeWithTag("screen_progress").assertIsDisplayed()

        composeRule.onNodeWithText("Exercises").performClick()
        composeRule.onNodeWithTag("screen_exercises").assertIsDisplayed()
    }

    @Test
    fun goalSetup_hidesTheBottomBar() {
        composeRule.setContent {
            RepTrackTheme { RepTrackNavHost(startRoute = Routes.GOAL_SETUP) }
        }

        composeRule.onNodeWithTag("screen_goal_setup").assertIsDisplayed()
        composeRule.onNodeWithText("Progress").assertDoesNotExist()
    }
}

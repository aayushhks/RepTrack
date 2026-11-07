package edu.bu.reptrack

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
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

    @Test
    fun startingAWorkout_opensFullScreenSession_andBackReturnsHome() {
        composeRule.setContent {
            RepTrackTheme { RepTrackNavHost(startRoute = TopLevelDestination.START.route) }
        }
        // Wait until the profile has loaded and the routine cards are on screen.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Start").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onAllNodesWithText("Start").onFirst().performScrollTo().performClick()

        composeRule.onNodeWithTag("screen_workout_session").assertIsDisplayed()
        // A workout takes the whole screen, so the bottom bar is hidden.
        composeRule.onNodeWithText("Progress").assertDoesNotExist()

        // No sets were logged, so Back leaves straight away without asking.
        Espresso.pressBack()
        composeRule.onNodeWithTag("screen_workouts").assertIsDisplayed()
    }
}

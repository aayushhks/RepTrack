package edu.bu.reptrack.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.data.auth.AuthUser
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.ui.theme.RepTrackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the Profile tab, drawn from a fixed state. */
@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var editClicked = false
    private var signInClicked = false
    private var signOutClicked = false

    private val stats = UserProfile(178f, 82f, 76f, FitnessGoal.BUILD_MUSCLE, TrainingPlace.GYM)

    private fun show(state: ProfileUiState) {
        composeRule.setContent {
            RepTrackTheme {
                ProfileScreen(
                    state = state,
                    onEditStats = { editClicked = true },
                    onSignIn = { signInClicked = true },
                    onSignOut = { signOutClicked = true },
                )
            }
        }
    }

    @Test
    fun signedIn_showsEmailStatsAndThisWeek() {
        show(
            ProfileUiState(
                isLoading = false,
                user = AuthUser("u1", "aayush@bu.edu"),
                profile = stats,
                workoutsThisWeek = 3,
                volumeThisWeekKg = 9370f,
            ),
        )

        composeRule.onNodeWithTag("profile_name").assertTextEquals("aayush@bu.edu")
        composeRule.onNodeWithText("178 cm").assertIsDisplayed()
        composeRule.onNodeWithText("Build muscle").assertIsDisplayed()
        composeRule.onNodeWithText("9370 kg").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun editStats_and_signOut_reportTaps() {
        show(ProfileUiState(isLoading = false, user = AuthUser("u1", "aayush@bu.edu"), profile = stats))

        composeRule.onNodeWithTag("edit_stats").performScrollTo().performClick()
        composeRule.onNodeWithTag("sign_out_button").performScrollTo().performClick()

        assertTrue(editClicked)
        assertTrue(signOutClicked)
    }

    @Test
    fun withoutAnAccount_offersSignIn() {
        show(ProfileUiState(isLoading = false, user = null, profile = stats))

        composeRule.onNodeWithText("Using without an account").assertIsDisplayed()
        composeRule.onNodeWithTag("sign_in_button").performScrollTo().performClick()

        assertTrue(signInClicked)
    }

    @Test
    fun withoutStats_offersSetUp() {
        show(ProfileUiState(isLoading = false, user = null, profile = null))

        composeRule.onNodeWithText("Set up your plan").performScrollTo().performClick()

        assertTrue(editClicked)
    }
}

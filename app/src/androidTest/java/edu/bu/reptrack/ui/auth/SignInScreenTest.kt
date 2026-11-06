package edu.bu.reptrack.ui.auth

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.data.auth.AuthError
import edu.bu.reptrack.data.auth.AuthResult
import edu.bu.reptrack.fakes.FakeAuthRepository
import edu.bu.reptrack.fakes.FakeProfileRepository
import edu.bu.reptrack.ui.theme.RepTrackTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the sign-in screen. Fakes only: nothing here talks to real Firebase. */
@RunWith(AndroidJUnit4::class)
class SignInScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val auth = FakeAuthRepository()
    private val profile = FakeProfileRepository()
    private val finished = AtomicBoolean(false)

    private fun showSignIn() {
        composeRule.setContent {
            val viewModel = remember { SignInViewModel(auth, profile, SavedStateHandle()) }
            RepTrackTheme {
                SignInRoute(onFinished = { finished.set(true) }, viewModel = viewModel)
            }
        }
    }

    private fun typeCredentials(email: String, password: String) {
        composeRule.onNodeWithTag("field_email").performScrollTo().performTextInput(email)
        composeRule.onNodeWithTag("field_password").performScrollTo().performTextInput(password)
    }

    @Test
    fun signingIn_callsTheRepository_andFinishes() {
        showSignIn()
        composeRule.onNodeWithTag("screen_sign_in").assertIsDisplayed()

        typeCredentials("runner@bu.edu", "secret1")
        composeRule.onNodeWithTag("submit_button").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { finished.get() }
        assertEquals(listOf("runner@bu.edu" to "secret1"), auth.signInCalls.toList())
    }

    @Test
    fun switchingToCreateAccount_changesTheButtonText() {
        showSignIn()
        composeRule.onNodeWithTag("submit_button").assertTextEquals("Sign in")

        composeRule.onNodeWithTag("switch_mode_button").performScrollTo().performClick()

        composeRule.onNodeWithTag("submit_button").assertTextEquals("Create account")
    }

    @Test
    fun badEmail_showsAnError_andDoesNotSignIn() {
        showSignIn()

        typeCredentials("nope", "secret1")
        composeRule.onNodeWithTag("submit_button").performScrollTo().performClick()

        composeRule.onNodeWithText("Enter a valid email address").assertIsDisplayed()
        assertTrue(auth.signInCalls.isEmpty())
        assertFalse(finished.get())
    }

    @Test
    fun wrongPassword_showsAMessage_andStaysOnTheScreen() {
        auth.nextResult = AuthResult.Failure(AuthError.WRONG_EMAIL_OR_PASSWORD)
        showSignIn()

        typeCredentials("runner@bu.edu", "secret1")
        composeRule.onNodeWithTag("submit_button").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { auth.signInCalls.isNotEmpty() }
        composeRule.onNodeWithTag("auth_error").assertTextEquals("Wrong email or password.")
        assertFalse(finished.get())
    }

    @Test
    fun usingWithoutAnAccount_remembersTheChoice_andFinishes() {
        showSignIn()

        composeRule.onNodeWithTag("skip_button").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { finished.get() }
        assertTrue(profile.signInSkipped)
        assertTrue(auth.signInCalls.isEmpty())
    }
}

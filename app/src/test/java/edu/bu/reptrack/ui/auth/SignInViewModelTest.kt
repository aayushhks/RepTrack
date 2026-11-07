package edu.bu.reptrack.ui.auth

import androidx.lifecycle.SavedStateHandle
import edu.bu.reptrack.data.auth.AuthError
import edu.bu.reptrack.data.auth.AuthResult
import edu.bu.reptrack.testing.FakeAuthRepository
import edu.bu.reptrack.testing.FakeProfileRepository
import edu.bu.reptrack.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val profile = FakeProfileRepository()

    private fun newViewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        SignInViewModel(auth, profile, savedState)

    private fun SignInViewModel.fillValidForm() {
        onEmailChange("runner@bu.edu")
        onPasswordChange("secret1")
    }

    @Test
    fun startsInSignInMode_empty_withNoErrors() = runTest {
        val state = newViewModel().uiState.value

        assertEquals(SignInMode.SIGN_IN, state.mode)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.emailInvalid)
        assertFalse(state.passwordTooShort)
        assertNull(state.authError)
        assertFalse(state.isLoading)
        assertFalse(state.isDone)
    }

    @Test
    fun badEmail_showsAnError_andDoesNotCallTheRepository() = runTest {
        val viewModel = newViewModel()

        viewModel.onEmailChange("not-an-email")
        viewModel.onPasswordChange("secret1")
        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.emailInvalid)
        assertFalse(state.passwordTooShort)
        assertFalse(state.isDone)
        assertTrue(auth.signInCalls.isEmpty())
        assertTrue(auth.createAccountCalls.isEmpty())
    }

    @Test
    fun shortPassword_isBlocked() = runTest {
        val viewModel = newViewModel()

        viewModel.onEmailChange("runner@bu.edu")
        viewModel.onPasswordChange("12345")
        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.passwordTooShort)
        assertFalse(state.emailInvalid)
        assertFalse(state.isDone)
        assertTrue(auth.signInCalls.isEmpty())
    }

    @Test
    fun success_finishes_andSendsTheTrimmedEmail() = runTest {
        val viewModel = newViewModel()

        viewModel.onEmailChange("  runner@bu.edu ")
        viewModel.onPasswordChange("secret1")
        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isDone)
        assertFalse(state.isLoading)
        assertNull(state.authError)
        assertEquals(listOf("runner@bu.edu" to "secret1"), auth.signInCalls.toList())
    }

    @Test
    fun failure_showsTheMatchingError_andDoesNotFinish() = runTest {
        AuthError.entries.forEach { error ->
            val viewModel = newViewModel()
            auth.nextResult = AuthResult.Failure(error)

            viewModel.fillValidForm()
            viewModel.submit()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(error, state.authError)
            assertFalse(state.isDone)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun editingAField_clearsTheErrorMessage() = runTest {
        val viewModel = newViewModel()
        auth.nextResult = AuthResult.Failure(AuthError.WRONG_EMAIL_OR_PASSWORD)
        viewModel.fillValidForm()
        viewModel.submit()
        advanceUntilIdle()

        viewModel.onPasswordChange("secret12")

        assertNull(viewModel.uiState.value.authError)
    }

    @Test
    fun createAccountMode_callsCreateAccount_notSignIn() = runTest {
        val viewModel = newViewModel()

        viewModel.onToggleMode()
        viewModel.fillValidForm()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(SignInMode.CREATE_ACCOUNT, viewModel.uiState.value.mode)
        assertTrue(viewModel.uiState.value.isDone)
        assertEquals(listOf("runner@bu.edu" to "secret1"), auth.createAccountCalls.toList())
        assertTrue(auth.signInCalls.isEmpty())
    }

    @Test
    fun togglingTheMode_clearsErrors() = runTest {
        val viewModel = newViewModel()
        auth.nextResult = AuthResult.Failure(AuthError.EMAIL_IN_USE)
        viewModel.fillValidForm()
        viewModel.submit()
        advanceUntilIdle()

        viewModel.onToggleMode()

        assertNull(viewModel.uiState.value.authError)
    }

    @Test
    fun emailAndMode_surviveInSavedState() = runTest {
        val savedState = SavedStateHandle()
        val first = newViewModel(savedState)
        first.onEmailChange("runner@bu.edu")
        first.onToggleMode()
        advanceUntilIdle()

        // A new ViewModel with the same saved state is what the app gets after rotation or process death.
        val second = newViewModel(savedState)

        assertEquals("runner@bu.edu", second.uiState.value.email)
        assertEquals(SignInMode.CREATE_ACCOUNT, second.uiState.value.mode)
    }

    @Test
    fun password_isNeverWrittenToSavedState() = runTest {
        val savedState = SavedStateHandle()
        val viewModel = newViewModel(savedState)

        viewModel.onEmailChange("runner@bu.edu")
        viewModel.onPasswordChange("secret1")
        viewModel.submit()
        advanceUntilIdle()

        assertFalse(savedState.keys().contains("password"))
        assertFalse(savedState.keys().any { savedState.get<Any?>(it) == "secret1" })
    }

    @Test
    fun skip_remembersTheChoice_andFinishes() = runTest {
        val viewModel = newViewModel()

        viewModel.skip()
        advanceUntilIdle()

        assertTrue(profile.signInSkipped)
        assertTrue(viewModel.uiState.value.isDone)
        assertTrue(auth.signInCalls.isEmpty())
    }
}

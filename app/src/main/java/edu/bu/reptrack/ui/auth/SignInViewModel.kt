package edu.bu.reptrack.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.auth.AuthError
import edu.bu.reptrack.data.auth.AuthRepository
import edu.bu.reptrack.data.auth.AuthResult
import edu.bu.reptrack.data.profile.ProfileRepository
import edu.bu.reptrack.domain.auth.CredentialsValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The screen has two modes, switched with a text button. */
enum class SignInMode { SIGN_IN, CREATE_ACCOUNT }

/** Everything the sign-in screen needs to draw itself. */
data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val mode: SignInMode = SignInMode.SIGN_IN,
    val emailInvalid: Boolean = false,
    val passwordTooShort: Boolean = false,
    /** Set when Firebase said no. Cleared as soon as the user edits a field. */
    val authError: AuthError? = null,
    val isLoading: Boolean = false,
    val isDone: Boolean = false,
)

/**
 * Holds the sign-in form. The email and the mode live in [SavedStateHandle], so they survive
 * rotation and the app being killed in the background. The password does NOT: saved state is
 * written to disk-backed storage, so the password only lives in memory, in this ViewModel.
 */
class SignInViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val email = savedState.getStateFlow(KEY_EMAIL, "")
    private val mode = savedState.getStateFlow(KEY_MODE, SignInMode.SIGN_IN.name)

    /** Everything that is not allowed in saved state, plus the short-lived flags. */
    private data class InMemory(
        val password: String = "",
        val emailInvalid: Boolean = false,
        val passwordTooShort: Boolean = false,
        val authError: AuthError? = null,
        val isLoading: Boolean = false,
        val isDone: Boolean = false,
    )

    private val inMemory = MutableStateFlow(InMemory())

    val uiState: StateFlow<SignInUiState> = combine(email, mode, inMemory) { emailText, modeName, memory ->
        buildState(emailText, modeName, memory)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = buildState(email.value, mode.value, inMemory.value),
    )

    fun onEmailChange(text: String) {
        savedState[KEY_EMAIL] = text
        inMemory.update { it.copy(emailInvalid = false, authError = null) }
    }

    fun onPasswordChange(text: String) {
        inMemory.update { it.copy(password = text, passwordTooShort = false, authError = null) }
    }

    /** Switches between Sign in and Create account. Old error messages no longer apply. */
    fun onToggleMode() {
        val next = if (currentMode() == SignInMode.SIGN_IN) SignInMode.CREATE_ACCOUNT else SignInMode.SIGN_IN
        savedState[KEY_MODE] = next.name
        inMemory.update { it.copy(emailInvalid = false, passwordTooShort = false, authError = null) }
    }

    /** Checks the form first. Firebase is only called when both fields look right. */
    fun submit() {
        val current = inMemory.value
        if (current.isLoading || current.isDone) return

        val emailText = email.value.trim()
        val emailOk = CredentialsValidator.isEmailValid(emailText)
        val passwordOk = CredentialsValidator.isPasswordValid(current.password)
        if (!emailOk || !passwordOk) {
            inMemory.update {
                it.copy(emailInvalid = !emailOk, passwordTooShort = !passwordOk, authError = null)
            }
            return
        }

        inMemory.update {
            it.copy(isLoading = true, emailInvalid = false, passwordTooShort = false, authError = null)
        }
        val password = current.password
        val signingIn = currentMode() == SignInMode.SIGN_IN
        viewModelScope.launch {
            val result = if (signingIn) {
                authRepository.signIn(emailText, password)
            } else {
                authRepository.createAccount(emailText, password)
            }
            when (result) {
                AuthResult.Success -> inMemory.update { it.copy(isLoading = false, isDone = true, password = "") }
                is AuthResult.Failure -> inMemory.update { it.copy(isLoading = false, authError = result.error) }
            }
        }
    }

    /** "Use without an account": remember the choice so the app does not ask again. */
    fun skip() {
        viewModelScope.launch {
            profileRepository.skipSignIn()
            inMemory.update { it.copy(isDone = true) }
        }
    }

    private fun currentMode(): SignInMode = SignInMode.valueOf(mode.value)

    private fun buildState(emailText: String, modeName: String, memory: InMemory) = SignInUiState(
        email = emailText,
        password = memory.password,
        mode = SignInMode.valueOf(modeName),
        emailInvalid = memory.emailInvalid,
        passwordTooShort = memory.passwordTooShort,
        authError = memory.authError,
        isLoading = memory.isLoading,
        isDone = memory.isDone,
    )

    companion object {
        private const val KEY_EMAIL = "email"
        private const val KEY_MODE = "mode"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RepTrackApplication
                SignInViewModel(
                    app.container.authRepository,
                    app.container.profileRepository,
                    createSavedStateHandle(),
                )
            }
        }
    }
}

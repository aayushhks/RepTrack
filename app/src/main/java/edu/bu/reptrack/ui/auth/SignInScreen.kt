package edu.bu.reptrack.ui.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.data.auth.AuthError
import edu.bu.reptrack.ui.theme.RepTrackTheme

/** Connects the sign-in screen to its ViewModel and calls [onFinished] once the user is signed in or skipped. */
@Composable
fun SignInRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = viewModel(factory = SignInViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isDone) {
        if (state.isDone) onFinished()
    }

    SignInScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onToggleMode = viewModel::onToggleMode,
        onSkip = viewModel::skip,
        modifier = modifier,
    )
}

/** The sign-in form. Stateless: it only draws [state] and reports events. */
@Composable
fun SignInScreen(
    state: SignInUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    // Only changes how the password is drawn, so it stays here instead of in the ViewModel.
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val creating = state.mode == SignInMode.CREATE_ACCOUNT

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("screen_sign_in"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(if (creating) R.string.sign_in_create_title else R.string.sign_in_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(if (creating) R.string.sign_in_create_subtitle else R.string.sign_in_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.sign_in_email)) },
            isError = state.emailInvalid,
            supportingText = if (state.emailInvalid) {
                { Text(stringResource(R.string.error_email_invalid)) }
            } else {
                null
            },
            singleLine = true,
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field_email"),
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.sign_in_password)) },
            isError = state.passwordTooShort,
            supportingText = if (state.passwordTooShort) {
                { Text(stringResource(R.string.error_password_short)) }
            } else {
                null
            },
            singleLine = true,
            enabled = !state.isLoading,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onSubmit()
                },
            ),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        painter = painterResource(
                            if (showPassword) R.drawable.ic_visibility_off else R.drawable.ic_visibility,
                        ),
                        contentDescription = stringResource(
                            if (showPassword) R.string.sign_in_hide_password else R.string.sign_in_show_password,
                        ),
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field_password"),
        )

        state.authError?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                // TalkBack reads the message as soon as it appears.
                modifier = Modifier
                    .semantics { liveRegion = LiveRegionMode.Assertive }
                    .testTag("auth_error"),
            )
        }

        val workingDescription = stringResource(R.string.sign_in_working)
        Button(
            onClick = onSubmit,
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("submit_button"),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(22.dp)
                        .semantics { contentDescription = workingDescription },
                    strokeWidth = 2.dp,
                    // The default color is the button color itself, which would be invisible here.
                    color = LocalContentColor.current,
                )
            } else {
                Text(stringResource(if (creating) R.string.sign_in_create_button else R.string.sign_in_button))
            }
        }

        TextButton(
            onClick = onToggleMode,
            enabled = !state.isLoading,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("switch_mode_button"),
        ) {
            Text(
                stringResource(
                    if (creating) R.string.sign_in_switch_to_sign_in else R.string.sign_in_switch_to_create,
                ),
            )
        }

        TextButton(
            onClick = onSkip,
            enabled = !state.isLoading,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("skip_button"),
        ) {
            Text(stringResource(R.string.sign_in_skip))
        }
    }
}

@StringRes
private fun AuthError.messageRes(): Int = when (this) {
    AuthError.INVALID_EMAIL -> R.string.auth_error_invalid_email
    AuthError.WRONG_EMAIL_OR_PASSWORD -> R.string.auth_error_wrong_credentials
    AuthError.EMAIL_IN_USE -> R.string.auth_error_email_in_use
    AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
    AuthError.NETWORK -> R.string.auth_error_network
    AuthError.UNKNOWN -> R.string.auth_error_unknown
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SignInScreenPreview() {
    RepTrackTheme {
        SignInScreen(
            state = SignInUiState(email = "runner@bu.edu", password = "secret1", authError = AuthError.NETWORK),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onToggleMode = {},
            onSkip = {},
        )
    }
}

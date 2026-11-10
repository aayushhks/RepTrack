package edu.bu.reptrack.ui.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.data.auth.AuthUser
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.domain.profile.formatKg
import edu.bu.reptrack.ui.components.labelRes
import edu.bu.reptrack.ui.theme.RepTrackTheme

/** Connects the Profile tab to its ViewModel. [onSignedOut] runs after signing out. */
@Composable
fun ProfileRoute(
    onEditStats: () -> Unit,
    onSignIn: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) onSignedOut()
    }

    ProfileScreen(
        state = state,
        onEditStats = onEditStats,
        onSignIn = onSignIn,
        onSignOut = viewModel::requestSignOut,
        modifier = modifier,
    )

    if (state.showSignOutDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSignOut,
            title = { Text(stringResource(R.string.profile_sign_out_title)) },
            text = { Text(stringResource(R.string.profile_sign_out_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::signOut, modifier = Modifier.testTag("confirm_sign_out")) {
                    Text(stringResource(R.string.profile_sign_out), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSignOut) {
                    Text(stringResource(R.string.session_cancel))
                }
            },
        )
    }
}

/** Profile tab: who you are, your stats, this week, your account. Stateless. */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEditStats: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("screen_profile"),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("screen_profile"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header(user = state.user)

        SectionLabel(R.string.profile_stats_label)
        StatsCard(profile = state.profile, onEditStats = onEditStats)

        SectionLabel(R.string.profile_week_label)
        WeekCard(workouts = state.workoutsThisWeek, volumeKg = state.volumeThisWeekKg)

        SectionLabel(R.string.profile_account_label)
        if (state.user != null) {
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_out_button"),
            ) {
                Text(stringResource(R.string.profile_sign_out))
            }
        } else {
            Button(
                onClick = onSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_in_button"),
            ) {
                Text(stringResource(R.string.profile_sign_in))
            }
        }

        SectionLabel(R.string.profile_about_label)
        Text(
            text = stringResource(R.string.profile_about),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Initial in a navy circle, then the email (or a note that there is no account). */
@Composable
private fun Header(user: AuthUser?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = user?.email?.firstOrNull()?.uppercase() ?: "R",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = user?.email ?: stringResource(R.string.profile_no_account),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .semantics { heading() }
                    .testTag("profile_name"),
            )
            Text(
                text = stringResource(
                    if (user != null) R.string.profile_signed_in_note else R.string.profile_no_account_note,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatsCard(profile: UserProfile?, onEditStats: () -> Unit) {
    QuietCard {
        if (profile == null) {
            Text(
                text = stringResource(R.string.profile_no_stats),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onEditStats, modifier = Modifier.testTag("edit_stats")) {
                Text(stringResource(R.string.workouts_setup_button))
            }
            return@QuietCard
        }
        StatRow(R.string.setup_height, stringResource(R.string.profile_value_cm, formatKg(profile.heightCm)))
        StatRow(R.string.setup_weight, stringResource(R.string.profile_value_kg, formatKg(profile.weightKg)))
        StatRow(R.string.setup_goal_weight, stringResource(R.string.profile_value_kg, formatKg(profile.goalWeightKg)))
        StatRow(R.string.profile_goal, stringResource(profile.goal.labelRes()))
        StatRow(R.string.workouts_place_label, stringResource(profile.place.labelRes()))
        FilledTonalButton(
            onClick = onEditStats,
            modifier = Modifier
                .align(Alignment.End)
                .testTag("edit_stats"),
        ) {
            Text(stringResource(R.string.profile_edit_stats))
        }
    }
}

@Composable
private fun StatRow(@StringRes label: Int, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun WeekCard(workouts: Int, volumeKg: Float) {
    QuietCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WeekNumber(
                value = workouts.toString(),
                label = R.string.profile_week_workouts,
                modifier = Modifier
                    .weight(1f)
                    .testTag("week_workouts"),
            )
            VerticalDivider(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(40.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            WeekNumber(
                value = stringResource(R.string.profile_value_kg, formatKg(volumeKg)),
                label = R.string.profile_week_volume,
                modifier = Modifier
                    .weight(1f)
                    .testTag("week_volume"),
            )
        }
    }
}

@Composable
private fun WeekNumber(value: String, @StringRes label: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = value, style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** White card with a hairline border, the same style as the routine and history cards. */
@Composable
private fun QuietCard(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionLabel(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(top = 8.dp)
            .semantics { heading() },
    )
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ProfileScreenPreview() {
    RepTrackTheme {
        ProfileScreen(
            state = ProfileUiState(
                isLoading = false,
                user = AuthUser(uid = "1", email = "aayush@bu.edu"),
                profile = UserProfile(178f, 82f, 76f, FitnessGoal.BUILD_MUSCLE, TrainingPlace.GYM),
                workoutsThisWeek = 3,
                volumeThisWeekKg = 9370f,
            ),
            onEditStats = {},
            onSignIn = {},
            onSignOut = {},
        )
    }
}

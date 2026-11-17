package edu.bu.reptrack.ui.run

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.run.RunFormat
import edu.bu.reptrack.ui.theme.RepTrackTheme

/**
 * Connects the run screen to its ViewModel. It also asks for the location permission (and on
 * Android 13 and newer the notification permission) the first time the user taps Start, so
 * the app never asks for location before the user wants to track a run.
 */
@Composable
fun RunRoute(
    onFinished: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RunViewModel = viewModel(factory = RunViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Read the real state instead of the result map: the dialog may have asked for only some of them.
        val precise = context.isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
        val approximate = context.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
        viewModel.onPermissionResult(granted = precise || approximate, approximateOnly = !precise && approximate)
    }

    BackHandler(enabled = state.outcome == null) { viewModel.requestLeave() }

    LaunchedEffect(state.outcome) {
        when (state.outcome) {
            RunOutcome.SAVED -> onFinished()
            RunOutcome.DISCARDED -> onLeave()
            null -> Unit
        }
    }

    RunScreen(
        state = state,
        onStart = {
            val missing = runPermissions().filterNot { context.isGranted(it) }
            if (missing.isEmpty()) viewModel.start() else permissionLauncher.launch(missing.toTypedArray())
        },
        onFinish = viewModel::finish,
        onClose = viewModel::requestLeave,
        onOpenSettings = { context.openAppSettings() },
        modifier = modifier,
    )

    if (state.showLeaveDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLeave,
            title = { Text(stringResource(R.string.run_leave_title)) },
            text = { Text(stringResource(R.string.run_leave_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::discard, modifier = Modifier.testTag("run_leave_discard")) {
                    Text(stringResource(R.string.run_leave_discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissLeave) {
                    Text(stringResource(R.string.run_leave_keep))
                }
            },
        )
    }
}

/** The walk or run screen. Stateless: it only draws [state] and reports events. */
@Composable
fun RunScreen(
    state: RunUiState,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_run"),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconButton(onClick = onClose, modifier = Modifier.testTag("run_close")) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.run_close),
                )
            }
            Text(
                text = stringResource(R.string.run_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            StatusLine(state)

            // The one big number on the screen.
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics(mergeDescendants = true) {},
            ) {
                Text(
                    text = RunFormat.km(state.distanceM),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("run_distance"),
                )
                Text(
                    text = stringResource(R.string.run_distance_unit),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatBlock(
                    label = stringResource(R.string.run_time),
                    value = RunFormat.duration(state.elapsedMs),
                    tag = "run_time",
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    label = stringResource(R.string.run_pace),
                    value = stringResource(R.string.run_pace_value, RunFormat.pace(state.paceSecondsPerKm)),
                    tag = "run_pace",
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    label = stringResource(R.string.run_calories),
                    value = stringResource(R.string.run_calories_value, state.calories),
                    tag = "run_calories",
                    modifier = Modifier.weight(1f),
                )
            }

            if (!state.weightKnown) {
                Text(
                    text = stringResource(R.string.run_weight_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.permissionDenied) {
                PermissionCard(onOpenSettings = onOpenSettings)
            }
        }

        // Fixed bottom area, so the main action is always one tap away.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                state.isSaving || state.outcome == RunOutcome.SAVED -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.run_saving),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                state.isRunning -> {
                    Button(
                        onClick = onFinish,
                        enabled = state.canFinish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("run_finish"),
                    ) {
                        Text(stringResource(R.string.run_finish))
                    }
                    if (!state.canFinish) {
                        // Says why Finish is greyed out, instead of leaving the user guessing.
                        Text(
                            text = stringResource(R.string.run_finish_hint, RunUiState.MIN_SAVE_DISTANCE_M.toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_finish_hint"),
                        )
                    }
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("run_discard"),
                    ) {
                        Text(stringResource(R.string.run_discard))
                    }
                }
                else -> Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("run_start"),
                ) {
                    Text(stringResource(R.string.run_start))
                }
            }
        }
    }
}

/** One short line that says what is happening. TalkBack reads it out when it changes. */
@Composable
private fun StatusLine(state: RunUiState) {
    val (text, isError) = when {
        state.locationFailed -> stringResource(R.string.run_location_failed) to true
        state.isRunning && state.approximateOnly -> stringResource(R.string.run_approximate) to true
        state.waitingForGps -> stringResource(R.string.run_waiting_gps) to false
        state.isRunning -> stringResource(R.string.run_tracking) to false
        else -> stringResource(R.string.run_ready) to false
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("run_status"),
    )
}

@Composable
private fun StatBlock(label: String, value: String, tag: String, modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier.semantics(mergeDescendants = true) {},
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag(tag),
            )
        }
    }
}

@Composable
private fun PermissionCard(onOpenSettings: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("run_permission_card"),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.run_permission_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.run_permission_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.testTag("run_open_settings")) {
                Text(stringResource(R.string.run_open_settings))
            }
        }
    }
}

/** Location to track the run; on Android 13 and newer also notifications, for the ongoing run notification. */
private fun runPermissions(): List<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}

private fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/** After "Don't allow" twice, Android stops showing the dialog; the user has to allow it in Settings. */
private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun RunScreenPreview() {
    RepTrackTheme {
        RunScreen(
            state = RunUiState(
                isRunning = true,
                elapsedMs = 754_000,
                distanceM = 2_410.0,
                paceSecondsPerKm = 313,
                calories = 182,
            ),
            onStart = {},
            onFinish = {},
            onClose = {},
            onOpenSettings = {},
        )
    }
}

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.run.RunFormat
import edu.bu.reptrack.ui.components.Overline
import edu.bu.reptrack.ui.theme.Chalk
import edu.bu.reptrack.ui.theme.Ember
import edu.bu.reptrack.ui.theme.Fog
import edu.bu.reptrack.ui.theme.Ink
import edu.bu.reptrack.ui.theme.Lime
import edu.bu.reptrack.ui.theme.RepTrackTheme
import edu.bu.reptrack.ui.theme.statStyle

/**
 * Connects the run screen to its ViewModel. It also asks for the location permission (and on
 * Android 13 and newer the notification permission) the first time the user taps Start, so
 * the app never asks for location before the user wants to track a run.
 */
@Composable
fun RunRoute(
    /** Gets the saved run's id, to open it on its map. */
    onFinished: (runId: Long?) -> Unit,
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
            RunOutcome.SAVED -> onFinished(state.savedRunId)
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

/**
 * The walk or run screen. The whole top is lime with one giant distance, so it reads at a
 * glance mid-run; time, pace, calories and the buttons sit on a dark panel below. It looks the
 * same in the light and dark theme. Stateless: it only draws [state] and reports events.
 */
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
            .background(Lime)
            .testTag("screen_run"),
    ) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            // At least as tall as the space, so the distance sits in the middle; scrolls when the
            // permission card or a large font size needs more room.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose, modifier = Modifier.testTag("run_close")) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.run_close),
                                tint = Ink,
                            )
                        }
                        Text(
                            text = stringResource(R.string.run_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Ink,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    StatusLine(state)
                }

                // The one big number on the screen. It does not grow with the font size setting:
                // it is already huge, and growing would push it off the screen.
                Column(
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .semantics(mergeDescendants = true) {},
                ) {
                    val distanceSize = with(LocalDensity.current) { 128.dp.toSp() }
                    Text(
                        text = RunFormat.km(state.distanceM),
                        style = statStyle(distanceSize),
                        color = Ink,
                        maxLines = 1,
                        modifier = Modifier.testTag("run_distance"),
                    )
                    Overline(stringResource(R.string.run_distance_unit), color = Ink)
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!state.weightKnown) {
                        Text(
                            text = stringResource(R.string.run_weight_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Ink,
                        )
                    }
                    if (state.permissionDenied) {
                        PermissionCard(onOpenSettings = onOpenSettings)
                    }
                }
            }
        }

        // Dark panel with rounded top corners: the stats, then the buttons, always in reach.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Ink,
            contentColor = Chalk,
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    PanelStat(
                        label = stringResource(R.string.run_time),
                        value = RunFormat.duration(state.elapsedMs),
                        tag = "run_time",
                        modifier = Modifier.weight(1f),
                    )
                    PanelStat(
                        label = stringResource(R.string.run_pace),
                        value = stringResource(R.string.run_pace_value, RunFormat.pace(state.paceSecondsPerKm)),
                        tag = "run_pace",
                        modifier = Modifier.weight(1f),
                    )
                    PanelStat(
                        label = stringResource(R.string.run_calories),
                        value = stringResource(R.string.run_calories_value, state.calories),
                        tag = "run_calories",
                        valueColor = Ember,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (state.saveFailed) {
                    Text(
                        text = stringResource(R.string.run_save_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ember,
                        modifier = Modifier
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("run_save_error"),
                    )
                }
                Buttons(state = state, onStart = onStart, onFinish = onFinish, onClose = onClose)
            }
        }
    }
}

@Composable
private fun Buttons(state: RunUiState, onStart: () -> Unit, onFinish: () -> Unit, onClose: () -> Unit) {
    val limeButton = ButtonDefaults.buttonColors(
        containerColor = Lime,
        contentColor = Ink,
        disabledContainerColor = Chalk.copy(alpha = 0.12f),
        disabledContentColor = Chalk.copy(alpha = 0.5f),
    )
    when {
        state.isSaving || state.outcome == RunOutcome.SAVED -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Lime)
            Text(
                text = stringResource(R.string.run_saving),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        state.isRunning -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onClose,
                    border = BorderStroke(1.dp, Chalk.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Chalk),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .testTag("run_discard"),
                ) {
                    Text(stringResource(R.string.run_discard))
                }
                Button(
                    onClick = onFinish,
                    enabled = state.canFinish,
                    colors = limeButton,
                    modifier = Modifier
                        .weight(2f)
                        .heightIn(min = 56.dp)
                        .testTag("run_finish"),
                ) {
                    Icon(painterResource(R.drawable.ic_stop), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.run_finish), style = MaterialTheme.typography.titleMedium)
                }
            }
            if (!state.canFinish) {
                // Says why Finish is greyed out, instead of leaving the user guessing.
                Text(
                    text = stringResource(R.string.run_finish_hint, RunUiState.MIN_SAVE_DISTANCE_M.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Fog,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_finish_hint"),
                )
            }
        }
        else -> Button(
            onClick = onStart,
            colors = limeButton,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("run_start"),
        ) {
            Icon(painterResource(R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.run_start), style = MaterialTheme.typography.titleMedium)
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
    // A problem gets a dark pill, so it stands out from the plain status text on lime.
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isError) Ink else Color.Transparent,
        contentColor = if (isError) Ember else Ink,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (isError) 12.dp else 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.isRunning && !isError) {
                Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = Ink) {}
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("run_status"),
            )
        }
    }
}

/** A stat on the dark panel: big condensed number over a small label, read as one item. */
@Composable
private fun PanelStat(
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Chalk,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = statStyle(34.sp),
            color = valueColor,
            maxLines = 1,
            modifier = Modifier.testTag(tag),
        )
        Overline(label, color = Fog)
    }
}

@Composable
private fun PermissionCard(onOpenSettings: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("run_permission_card"),
        shape = MaterialTheme.shapes.medium,
        color = Ink,
        contentColor = Chalk,
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
                color = Fog,
            )
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("run_open_settings"),
            ) {
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

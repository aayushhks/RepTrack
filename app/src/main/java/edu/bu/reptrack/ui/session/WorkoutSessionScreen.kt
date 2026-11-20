package edu.bu.reptrack.ui.session

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.profile.formatKg
import edu.bu.reptrack.domain.workout.LoggedSet
import edu.bu.reptrack.domain.workout.Rest
import edu.bu.reptrack.ui.components.ErrorCard
import edu.bu.reptrack.ui.components.VoltCard
import edu.bu.reptrack.ui.components.readableWidth
import edu.bu.reptrack.ui.theme.RepTrackTheme
import edu.bu.reptrack.ui.theme.statStyle
import kotlinx.coroutines.delay

/**
 * Connects the workout screen to its ViewModel. [onFinished] runs after the workout is saved,
 * [onLeave] after it is discarded. Back asks before throwing logged sets away.
 */
@Composable
fun WorkoutSessionRoute(
    onFinished: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutSessionViewModel = viewModel(factory = WorkoutSessionViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.outcome == null) { viewModel.requestLeave() }

    LaunchedEffect(state.outcome) {
        when (state.outcome) {
            SessionOutcome.FINISHED -> onFinished()
            SessionOutcome.DISCARDED -> onLeave()
            null -> Unit
        }
    }

    WorkoutSessionScreen(
        state = state,
        onWeightChange = viewModel::onWeightChange,
        onRepsChange = viewModel::onRepsChange,
        onAddSet = viewModel::addSet,
        onEditSet = viewModel::onEditSet,
        onClose = viewModel::requestLeave,
        onFinish = viewModel::finish,
        onAdjustRest = viewModel::adjustRest,
        onSkipRest = viewModel::skipRest,
        onDismissError = viewModel::dismissError,
        modifier = modifier,
    )

    state.editing?.let { editing ->
        EditSetDialog(
            editing = editing,
            onWeightChange = viewModel::onEditWeightChange,
            onRepsChange = viewModel::onEditRepsChange,
            onSave = viewModel::saveEdit,
            onDelete = viewModel::deleteEditingSet,
            onDismiss = viewModel::dismissEdit,
        )
    }

    if (state.showLeaveDialog) {
        LeaveDialog(onDiscard = viewModel::discard, onKeep = viewModel::dismissLeave)
    }
}

/** The workout screen. Stateless: it only draws [state] and reports events. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WorkoutSessionScreen(
    state: WorkoutSessionUiState,
    onWeightChange: (exercise: String, text: String) -> Unit,
    onRepsChange: (exercise: String, text: String) -> Unit,
    onAddSet: (exercise: String) -> Unit,
    onEditSet: (LoggedSet) -> Unit,
    onClose: () -> Unit,
    onFinish: () -> Unit,
    onAdjustRest: (seconds: Int) -> Unit,
    onSkipRest: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .readableWidth()
            .imePadding()
            .testTag("screen_workout_session"),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("close_button"),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.session_close),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = state.routineName.ifEmpty { stringResource(R.string.session_fallback_title) },
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = pluralStringResource(R.plurals.exercise_count, state.exercises.size, state.exercises.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ExerciseProgress(exercises = state.exercises)
            }
            state.exercises.forEach { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onWeightChange = { onWeightChange(exercise.name, it) },
                    onRepsChange = { onRepsChange(exercise.name, it) },
                    onAddSet = { onAddSet(exercise.name) },
                    onEditSet = onEditSet,
                )
            }
        }

        // Fixed bottom panel, so the rest timer and Finish are always one tap away.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.error?.let { error ->
                    ErrorCard(
                        message = stringResource(error.messageRes()),
                        actionLabel = stringResource(R.string.error_dismiss),
                        onAction = onDismissError,
                        modifier = Modifier.testTag("session_error"),
                    )
                }
                state.rest?.let { rest ->
                    RestTimer(rest = rest, onAdjust = onAdjustRest, onSkip = onSkipRest)
                }
                Text(
                    text = if (state.setCount == 0) {
                        stringResource(R.string.session_no_sets)
                    } else {
                        stringResource(
                            R.string.session_totals,
                            pluralStringResource(R.plurals.set_count, state.setCount, state.setCount),
                            formatKg(state.totalVolumeKg),
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag("session_totals"),
                )
                Button(
                    onClick = onFinish,
                    enabled = state.canFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("finish_button"),
                ) {
                    Text(stringResource(R.string.session_finish), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@StringRes
private fun SessionError.messageRes(): Int = when (this) {
    SessionError.SET_NOT_SAVED -> R.string.session_error_set
    SessionError.CHANGE_NOT_SAVED -> R.string.session_error_change
    SessionError.FINISH_FAILED -> R.string.session_error_finish
}

/** One bar per exercise, lime once it has a set, so the user sees how far along they are. */
@Composable
private fun ExerciseProgress(exercises: List<ExerciseUi>) {
    if (exercises.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        exercises.forEach { exercise ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp),
                shape = CircleShape,
                color = if (exercise.sets.isNotEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            ) {}
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: ExerciseUi,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onAddSet: () -> Unit,
    onEditSet: (LoggedSet) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val addLabel = stringResource(R.string.session_add_set_for, exercise.name)

    VoltCard(modifier = modifier) {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )

        if (exercise.sets.isNotEmpty()) {
            Column {
                exercise.sets.forEach { set ->
                    SetRow(set = set, exerciseName = exercise.name, onClick = { onEditSet(set) })
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = exercise.weightInput,
                onValueChange = onWeightChange,
                label = { Text(stringResource(R.string.session_weight)) },
                suffix = { Text(stringResource(R.string.unit_kg)) },
                isError = exercise.weightError,
                singleLine = true,
                textStyle = statStyle(24.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("weight_${exercise.name}"),
            )
            OutlinedTextField(
                value = exercise.repsInput,
                onValueChange = onRepsChange,
                label = { Text(stringResource(R.string.session_reps)) },
                isError = exercise.repsError,
                singleLine = true,
                textStyle = statStyle(24.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddSet() }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("reps_${exercise.name}"),
            )
        }

        if (exercise.weightError || exercise.repsError) {
            Text(
                text = stringResource(
                    if (exercise.repsError) R.string.error_reps_range else R.string.error_set_weight_range,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        Button(
            onClick = onAddSet,
            // TalkBack says "Add set to Squat" instead of just "Add set".
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { contentDescription = addLabel }
                .testTag("add_${exercise.name}"),
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.session_add_set))
        }
    }
}

/** One logged set: a lime check, the set number, the numbers big. The whole row opens the edit dialog. */
@Composable
private fun SetRow(set: LoggedSet, exerciseName: String, onClick: () -> Unit) {
    val editLabel = stringResource(R.string.session_edit_set, set.setNumber)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClickLabel = editLabel, role = Role.Button, onClick = onClick)
            .testTag("set_${exerciseName}_${set.setNumber}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(6.dp),
            )
        }
        Text(
            text = stringResource(R.string.session_set_label, set.setNumber),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp),
        )
        Text(
            text = setValueText(set),
            style = statStyle(24.sp),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.session_edit),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun setValueText(set: LoggedSet): String =
    if (set.weightKg == 0f) {
        stringResource(R.string.session_set_bodyweight, set.reps)
    } else {
        stringResource(R.string.session_set_value, formatKg(set.weightKg), set.reps)
    }

@Composable
private fun EditSetDialog(
    editing: EditSetUi,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.session_edit_title, editing.set.exerciseName, editing.set.setNumber))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editing.weightInput,
                    onValueChange = onWeightChange,
                    label = { Text(stringResource(R.string.session_weight)) },
                    suffix = { Text(stringResource(R.string.unit_kg)) },
                    isError = editing.weightError,
                    supportingText = if (editing.weightError) {
                        { Text(stringResource(R.string.error_set_weight_range)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.testTag("edit_weight"),
                )
                OutlinedTextField(
                    value = editing.repsInput,
                    onValueChange = onRepsChange,
                    label = { Text(stringResource(R.string.session_reps)) },
                    isError = editing.repsError,
                    supportingText = if (editing.repsError) {
                        { Text(stringResource(R.string.error_reps_range)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.testTag("edit_reps"),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, modifier = Modifier.testTag("edit_save")) {
                Text(stringResource(R.string.session_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete, modifier = Modifier.testTag("edit_delete")) {
                    Text(stringResource(R.string.session_delete_set), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.session_cancel))
                }
            }
        },
    )
}

/**
 * Rest countdown after a set: a ring that empties, the time left in big numbers, and
 * − 15 s, + 15 s and Skip. It counts down on screen from the end time the ViewModel keeps,
 * and buzzes once when the rest is over.
 */
@Composable
private fun RestTimer(rest: RestTimerUi, onAdjust: (Int) -> Unit, onSkip: () -> Unit) {
    val remainingMs by produceState(Rest.remainingMs(rest.endsAt, System.currentTimeMillis()), rest.endsAt) {
        var left = Rest.remainingMs(rest.endsAt, System.currentTimeMillis())
        value = left
        while (left > 0) {
            val step = minOf(1_000L, left)
            delay(step)
            // Whichever is further along: the steps waited, or the clock (it moves on while the phone sleeps).
            left = minOf(left - step, Rest.remainingMs(rest.endsAt, System.currentTimeMillis()))
            value = left
        }
    }
    val isOver = remainingMs == 0L
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(isOver) {
        if (isOver) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rest_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { Rest.fractionLeft(remainingMs, rest.totalMs) },
                modifier = Modifier.size(72.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round,
            )
            Text(text = Rest.format(remainingMs), style = statStyle(22.sp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isOver) {
                    stringResource(R.string.session_rest_over)
                } else {
                    stringResource(R.string.session_rest_left, Rest.format(remainingMs))
                },
                style = MaterialTheme.typography.titleSmall,
                // Read out once when the rest is over, not every second.
                modifier = Modifier
                    .then(if (isOver) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier)
                    .testTag("rest_text"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isOver) {
                    // TalkBack reads "15 seconds less rest" instead of "minus 15 s".
                    val lessLabel = stringResource(R.string.session_rest_minus_label, Rest.STEP_SECONDS)
                    val moreLabel = stringResource(R.string.session_rest_plus_label, Rest.STEP_SECONDS)
                    FilledTonalButton(
                        onClick = { onAdjust(-Rest.STEP_SECONDS) },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .semantics { contentDescription = lessLabel }
                            .testTag("rest_minus"),
                    ) {
                        Text(stringResource(R.string.session_rest_minus, Rest.STEP_SECONDS))
                    }
                    FilledTonalButton(
                        onClick = { onAdjust(Rest.STEP_SECONDS) },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .semantics { contentDescription = moreLabel }
                            .testTag("rest_plus"),
                    ) {
                        Text(stringResource(R.string.session_rest_plus, Rest.STEP_SECONDS))
                    }
                }
                OutlinedButton(
                    onClick = onSkip,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("rest_skip"),
                ) {
                    Text(stringResource(if (isOver) R.string.session_rest_dismiss else R.string.session_rest_skip))
                }
            }
        }
    }
}

@Composable
private fun LeaveDialog(onDiscard: () -> Unit, onKeep: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text(stringResource(R.string.session_leave_title)) },
        text = { Text(stringResource(R.string.session_leave_message)) },
        confirmButton = {
            TextButton(onClick = onDiscard, modifier = Modifier.testTag("leave_discard")) {
                Text(stringResource(R.string.session_leave_discard), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onKeep) {
                Text(stringResource(R.string.session_leave_keep))
            }
        },
    )
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WorkoutSessionScreenPreview() {
    val sets = listOf(
        LoggedSet(1, 1, "Squat", 1, 60f, 10, 0),
        LoggedSet(2, 1, "Squat", 2, 70f, 8, 0),
    )
    RepTrackTheme {
        WorkoutSessionScreen(
            state = WorkoutSessionUiState(
                routineName = "Full body A",
                exercises = listOf(
                    ExerciseUi("Squat", sets, weightInput = "70", repsInput = "8"),
                    ExerciseUi("Bench press"),
                ),
                setCount = 2,
                totalVolumeKg = 1160f,
            ),
            onWeightChange = { _, _ -> },
            onRepsChange = { _, _ -> },
            onAddSet = {},
            onEditSet = {},
            onClose = {},
            onFinish = {},
            onAdjustRest = {},
            onSkipRest = {},
            onDismissError = {},
        )
    }
}

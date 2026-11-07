package edu.bu.reptrack.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.profile.formatKg
import edu.bu.reptrack.domain.workout.LoggedSet
import edu.bu.reptrack.ui.theme.RepTrackTheme

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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("screen_workout_session"),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = state.routineName.ifEmpty { stringResource(R.string.session_fallback_title) },
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = pluralStringResource(R.plurals.exercise_count, state.exercises.size, state.exercises.size),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

        // Fixed bottom area, so Finish is always one tap away.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                    .heightIn(min = 52.dp)
                    .testTag("finish_button"),
            ) {
                Text(stringResource(R.string.session_finish))
            }
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

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )

            exercise.sets.forEach { set ->
                SetRow(set = set, exerciseName = exercise.name, onClick = { onEditSet(set) })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = exercise.weightInput,
                    onValueChange = onWeightChange,
                    label = { Text(stringResource(R.string.session_weight)) },
                    suffix = { Text(stringResource(R.string.unit_kg)) },
                    isError = exercise.weightError,
                    singleLine = true,
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

            FilledTonalButton(
                onClick = onAddSet,
                // TalkBack says "Add set to Squat" instead of just "Add set".
                modifier = Modifier
                    .align(Alignment.End)
                    .semantics { contentDescription = addLabel }
                    .testTag("add_${exercise.name}"),
            ) {
                Text(stringResource(R.string.session_add_set))
            }
        }
    }
}

/** One logged set. The whole row is a button that opens the edit dialog. */
@Composable
private fun SetRow(set: LoggedSet, exerciseName: String, onClick: () -> Unit) {
    val editLabel = stringResource(R.string.session_edit_set, set.setNumber)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = editLabel, role = Role.Button, onClick = onClick)
            .testTag("set_${exerciseName}_${set.setNumber}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.session_set_label, set.setNumber),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp),
        )
        Text(
            text = setValueText(set),
            style = MaterialTheme.typography.bodyLarge,
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
        )
    }
}

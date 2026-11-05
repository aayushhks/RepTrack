package edu.bu.reptrack.ui.setup

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.profile.FieldError
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.StarterPlan
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.ui.components.labelRes
import edu.bu.reptrack.ui.theme.RepTrackTheme

/** Connects the setup screen to its ViewModel and calls [onFinished] once setup is saved or skipped. */
@Composable
fun GoalSetupRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalSetupViewModel = viewModel(factory = GoalSetupViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isDone) {
        if (state.isDone) onFinished()
    }

    GoalSetupScreen(
        state = state,
        onHeightChange = viewModel::onHeightChange,
        onWeightChange = viewModel::onWeightChange,
        onGoalWeightChange = viewModel::onGoalWeightChange,
        onGoalSelected = viewModel::onGoalSelected,
        onPlaceSelected = viewModel::onPlaceSelected,
        onStart = viewModel::save,
        onSkip = viewModel::skip,
        modifier = modifier,
    )
}

/** The setup form. Stateless: it only draws [state] and reports events. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalSetupScreen(
    state: GoalSetupUiState,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onGoalWeightChange: (String) -> Unit,
    onGoalSelected: (FitnessGoal) -> Unit,
    onPlaceSelected: (TrainingPlace) -> Unit,
    onStart: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("screen_goal_setup"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        NumberField(
            value = state.height,
            onValueChange = onHeightChange,
            label = R.string.setup_height,
            unit = R.string.unit_cm,
            error = state.heightError,
            rangeError = R.string.error_height_range,
            tag = "field_height",
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                value = state.weight,
                onValueChange = onWeightChange,
                label = R.string.setup_weight,
                unit = R.string.unit_kg,
                error = state.weightError,
                rangeError = R.string.error_weight_range,
                tag = "field_weight",
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Right) },
                modifier = Modifier.weight(1f),
            )
            NumberField(
                value = state.goalWeight,
                onValueChange = onGoalWeightChange,
                label = R.string.setup_goal_weight,
                unit = R.string.unit_kg,
                error = state.goalWeightError,
                rangeError = R.string.error_weight_range,
                tag = "field_goal_weight",
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() },
                modifier = Modifier.weight(1f),
            )
        }

        SectionLabel(R.string.setup_goal_label)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FitnessGoal.entries.forEach { goal ->
                FilterChip(
                    selected = state.goal == goal,
                    onClick = { onGoalSelected(goal) },
                    label = { Text(stringResource(goal.labelRes())) },
                )
            }
        }

        SectionLabel(R.string.setup_place_label)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TrainingPlace.entries.forEachIndexed { index, place ->
                SegmentedButton(
                    selected = state.place == place,
                    onClick = { onPlaceSelected(place) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = TrainingPlace.entries.size),
                ) {
                    Text(stringResource(place.labelRes()))
                }
            }
        }

        PlanCard(plan = state.plan)

        Text(
            text = stringResource(R.string.setup_private_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = onStart,
            enabled = state.canSave,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.setup_start))
        }
        TextButton(
            onClick = onSkip,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(stringResource(R.string.setup_skip))
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    @StringRes unit: Int,
    error: FieldError?,
    @StringRes rangeError: Int,
    tag: String,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorText = when (error) {
        FieldError.NOT_A_NUMBER -> stringResource(R.string.error_not_number)
        FieldError.OUT_OF_RANGE -> stringResource(rangeError)
        FieldError.EMPTY, null -> null
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        suffix = { Text(stringResource(unit)) },
        isError = errorText != null,
        supportingText = if (errorText != null) {
            { Text(errorText) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag),
    )
}

@Composable
private fun SectionLabel(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun PlanCard(plan: StarterPlan, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_plan_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.plan_summary, stringResource(plan.type.labelRes()), plan.daysPerWeek),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                // TalkBack reads the new plan when the goal or place changes.
                modifier = Modifier
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("plan_summary"),
            )
            Text(
                text = stringResource(R.string.setup_plan_reason),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun GoalSetupScreenPreview() {
    RepTrackTheme {
        GoalSetupScreen(
            state = GoalSetupUiState(height = "178", weight = "82", goalWeight = "76", canSave = true),
            onHeightChange = {},
            onWeightChange = {},
            onGoalWeightChange = {},
            onGoalSelected = {},
            onPlaceSelected = {},
            onStart = {},
            onSkip = {},
        )
    }
}

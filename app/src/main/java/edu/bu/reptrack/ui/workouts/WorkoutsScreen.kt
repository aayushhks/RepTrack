package edu.bu.reptrack.ui.workouts

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.StarterPlan
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.domain.profile.WeightGoalGap
import edu.bu.reptrack.domain.profile.formatKg
import edu.bu.reptrack.domain.profile.weightGoalGap
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.domain.workout.WorkoutTemplate
import edu.bu.reptrack.ui.components.labelRes
import edu.bu.reptrack.ui.theme.RepTrackTheme

/** Connects the Workouts tab to its ViewModel. */
@Composable
fun WorkoutsRoute(
    onStartWorkout: (routineId: String) -> Unit,
    onSetUpPlan: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutsViewModel = viewModel(factory = WorkoutsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutsScreen(
        state = state,
        onPlaceSelected = viewModel::onPlaceSelected,
        onStartWorkout = onStartWorkout,
        onSetUpPlan = onSetUpPlan,
        modifier = modifier,
    )
}

/** Workouts tab: the user's plan, gym or home, and the routines they can start. Stateless. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    state: WorkoutsUiState,
    onPlaceSelected: (TrainingPlace) -> Unit,
    onStartWorkout: (routineId: String) -> Unit,
    onSetUpPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("screen_workouts"),
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
            .testTag("screen_workouts"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val profile = state.profile
        if (profile != null) {
            PlanCard(plan = state.plan, profile = profile)
        } else {
            SetUpPlanCard(onSetUpPlan = onSetUpPlan)
        }

        SectionLabel(R.string.workouts_place_label)
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

        SectionLabel(R.string.workouts_routines_label)
        state.routines.forEach { routine ->
            RoutineCard(routine = routine, onStart = { onStartWorkout(routine.id) })
        }
    }
}

/** The one bold element on the tab: a navy card with the plan name in big expanded type. */
@Composable
private fun PlanCard(plan: StarterPlan, profile: UserProfile, modifier: Modifier = Modifier) {
    HeroCard(modifier = modifier) {
        val onHero = MaterialTheme.colorScheme.onPrimary
        Text(
            text = stringResource(R.string.workouts_plan_label),
            style = MaterialTheme.typography.labelLarge,
            color = onHero.copy(alpha = 0.72f),
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(plan.type.labelRes()),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(top = 4.dp)
                .testTag("plan_name"),
        )
        Text(
            text = stringResource(R.string.plan_days, plan.daysPerWeek),
            style = MaterialTheme.typography.bodyLarge,
            color = onHero.copy(alpha = 0.85f),
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = onHero.copy(alpha = 0.18f),
        )
        Text(
            text = stringResource(R.string.workouts_goal, stringResource(profile.goal.labelRes())),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    R.string.workouts_weight_now_goal,
                    formatKg(profile.weightKg),
                    formatKg(profile.goalWeightKg),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = onHero.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f),
            )
            // Olive pill: the app's single accent color, used for progress.
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ) {
                Text(
                    text = weightGapText(profile),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("weight_gap"),
                )
            }
        }
    }
}

@Composable
private fun weightGapText(profile: UserProfile): String =
    when (val gap = weightGoalGap(profile.weightKg, profile.goalWeightKg)) {
        is WeightGoalGap.ToLose -> stringResource(R.string.workouts_weight_to_lose, formatKg(gap.kg))
        is WeightGoalGap.ToGain -> stringResource(R.string.workouts_weight_to_gain, formatKg(gap.kg))
        WeightGoalGap.AtGoal -> stringResource(R.string.workouts_weight_at_goal)
    }

@Composable
private fun SetUpPlanCard(onSetUpPlan: () -> Unit, modifier: Modifier = Modifier) {
    HeroCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.workouts_setup_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.workouts_setup_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )
        // Flipped colors so the button stands out on the navy card.
        Button(
            onClick = onSetUpPlan,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(stringResource(R.string.workouts_setup_button))
        }
    }
}

/** Navy card with the largest corner radius in the app. Only used for the top card. */
@Composable
private fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Column(modifier = Modifier.padding(24.dp), content = content)
    }
}

/** Quiet white card with a hairline border, so the plan card above stays the focus. */
@Composable
private fun RoutineCard(routine: WorkoutTemplate, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val startLabel = stringResource(R.string.workouts_start_routine, routine.name)
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = routine.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = routine.exercises.joinToString(separator = ", "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.workouts_exercise_count, routine.exercises.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    onClick = onStart,
                    // TalkBack says "Start Full body A" instead of just "Start".
                    modifier = Modifier
                        .semantics { contentDescription = startLabel }
                        .testTag("start_${routine.id}"),
                ) {
                    Text(stringResource(R.string.workouts_start))
                }
            }
        }
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
private fun WorkoutsScreenPreview() {
    val plan = StarterPlan.suggest(FitnessGoal.BUILD_MUSCLE, TrainingPlace.GYM)
    RepTrackTheme {
        WorkoutsScreen(
            state = WorkoutsUiState(
                isLoading = false,
                profile = UserProfile(178f, 82f, 76f, FitnessGoal.BUILD_MUSCLE, TrainingPlace.GYM),
                place = TrainingPlace.GYM,
                plan = plan,
                routines = StarterRoutines.forPlan(plan.type),
            ),
            onPlaceSelected = {},
            onStartWorkout = {},
            onSetUpPlan = {},
        )
    }
}

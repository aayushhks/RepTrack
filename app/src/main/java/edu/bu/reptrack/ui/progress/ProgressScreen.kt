package edu.bu.reptrack.ui.progress

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import edu.bu.reptrack.domain.profile.FieldError
import edu.bu.reptrack.domain.profile.WeightGoalGap
import edu.bu.reptrack.domain.profile.formatKg
import edu.bu.reptrack.domain.profile.weightGoalGap
import edu.bu.reptrack.domain.run.RunFormat
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.domain.weight.WeightEntry
import edu.bu.reptrack.domain.workout.SessionSummary
import edu.bu.reptrack.domain.workout.WorkoutSession
import edu.bu.reptrack.domain.workout.durationMinutes
import edu.bu.reptrack.ui.components.ErrorCard
import edu.bu.reptrack.ui.components.IconTile
import edu.bu.reptrack.ui.components.Overline
import edu.bu.reptrack.ui.components.StatTile
import edu.bu.reptrack.ui.components.readableWidth
import edu.bu.reptrack.ui.theme.RepTrackTheme
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Connects the Progress tab to its ViewModel. */
@Composable
fun ProgressRoute(
    onStartWorkout: () -> Unit,
    onOpenRun: (runId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(
        state = state,
        onStartWorkout = onStartWorkout,
        onLogWeight = viewModel::openLogWeight,
        onWeightInput = viewModel::onWeightInput,
        onSaveWeight = viewModel::saveWeight,
        onDismissLogWeight = viewModel::dismissLogWeight,
        onDeleteWeight = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDelete,
        onRetry = viewModel::retry,
        onDismissMessage = viewModel::dismissMessage,
        onOpenRun = onOpenRun,
        modifier = modifier,
    )
}

/** Progress tab: the weight log and its trend, then finished workouts and runs, newest first. Stateless. */
@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onStartWorkout: () -> Unit,
    onLogWeight: () -> Unit,
    onWeightInput: (String) -> Unit,
    onSaveWeight: () -> Unit,
    onDismissLogWeight: () -> Unit,
    onDeleteWeight: (WeightEntry) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onRetry: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    /** A run card was tapped: open it on its map. */
    onOpenRun: (runId: Long) -> Unit = {},
) {
    if (state.loadFailed) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .readableWidth()
                .padding(20.dp)
                .testTag("screen_progress"),
            contentAlignment = Alignment.Center,
        ) {
            ErrorCard(
                message = stringResource(R.string.progress_load_failed),
                actionLabel = stringResource(R.string.error_try_again),
                onAction = onRetry,
                modifier = Modifier.testTag("progress_error"),
            )
        }
        return
    }
    if (state.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("screen_progress"),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // A lazy list, so a long history only draws the rows on screen.
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .readableWidth()
            .testTag("screen_progress"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "page_title") {
            Text(
                text = stringResource(R.string.tab_progress).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
        state.message?.let { message ->
            item(key = "message") {
                ErrorCard(
                    message = stringResource(
                        when (message) {
                            ProgressMessage.WEIGHT_NOT_SAVED -> R.string.progress_weight_not_saved
                            ProgressMessage.WEIGHT_NOT_DELETED -> R.string.progress_weight_not_deleted
                        },
                    ),
                    actionLabel = stringResource(R.string.error_dismiss),
                    onAction = onDismissMessage,
                    modifier = Modifier.testTag("progress_message"),
                )
            }
        }
        item(key = "weight_title") { SectionTitle(stringResource(R.string.weight_title)) }
        item(key = "weight_card", contentType = "weight") {
            val weight = state.weight
            if (weight == null) {
                EmptyWeightCard(onLogWeight = onLogWeight)
            } else {
                WeightCard(weight = weight, onLogWeight = onLogWeight, onDeleteWeight = onDeleteWeight)
            }
        }
        item(key = "week_title") {
            SectionTitle(stringResource(R.string.workouts_this_week), Modifier.padding(top = 8.dp))
        }
        item(key = "week_totals", contentType = "week") { WeekTotalsRow(week = state.week) }
        item(key = "history_title") {
            SectionTitle(stringResource(R.string.progress_history_title), Modifier.padding(top = 8.dp))
        }
        if (state.items.isEmpty()) {
            item { EmptyHistory(onStartWorkout = onStartWorkout) }
        } else {
            // Stable keys keep each card in place when a new one arrives; the content type lets
            // the list reuse a workout card for a workout and a run card for a run.
            items(
                items = state.items,
                key = { it.key },
                contentType = { item -> if (item is HistoryItem.Run) "run" else "workout" },
            ) { item ->
                when (item) {
                    is HistoryItem.Workout -> HistoryCard(summary = item.summary)
                    is HistoryItem.Run -> RunHistoryCard(run = item.run, onClick = { onOpenRun(item.run.id) })
                }
            }
        }
    }

    state.logDialog?.let { dialog ->
        LogWeightDialogView(
            dialog = dialog,
            onInput = onWeightInput,
            onSave = onSaveWeight,
            onDismiss = onDismissLogWeight,
        )
    }
    state.pendingDelete?.let { entry ->
        DeleteWeightDialog(entry = entry, onConfirm = onConfirmDelete, onDismiss = onDismissDelete)
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Overline(text = text, modifier = modifier.semantics { heading() })
}

/** Workouts, distance and calories since Monday, as three stat tiles. */
@Composable
private fun WeekTotalsRow(week: WeekTotals) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("week_totals"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatTile(
            label = stringResource(R.string.progress_week_workouts),
            value = week.workouts.toString(),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.progress_week_distance),
            value = stringResource(R.string.progress_km, RunFormat.km(week.distanceM)),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.progress_week_calories),
            value = stringResource(R.string.run_calories_value, week.calories),
            valueColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Before the first weigh-in: says what the card is for and how to start. */
@Composable
private fun EmptyWeightCard(onLogWeight: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weight_empty"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = stringResource(R.string.weight_empty_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.weight_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onLogWeight,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .heightIn(min = 48.dp)
                    .testTag("weight_log"),
            ) {
                Text(stringResource(R.string.weight_log))
            }
        }
    }
}

/** Current weight, change since the start, the chart, progress to the goal and the last weigh-ins. */
@Composable
private fun WeightCard(
    weight: WeightSummary,
    onLogWeight: () -> Unit,
    onDeleteWeight: (WeightEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weight_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Read as one item by TalkBack: "82.4 kg, Logged Wed, Nov 18".
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Text(
                        text = stringResource(R.string.weight_kg, formatKg(weight.latest.weightKg)),
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.testTag("weight_current"),
                    )
                    Text(
                        text = stringResource(R.string.weight_logged_on, formatWorkoutDate(weight.latest.loggedAt)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onLogWeight,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("weight_log"),
                ) {
                    Text(stringResource(R.string.weight_log))
                }
            }

            Text(
                text = changeText(weight),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("weight_change"),
            )

            if (weight.hasTrend) {
                WeightTrendChart(
                    chart = weight.chart,
                    description = chartDescription(weight),
                    startDate = formatShortDate(weight.chartStart.loggedAt),
                    endDate = formatShortDate(weight.latest.loggedAt),
                )
            }

            weight.goalKg?.let { goalKg -> GoalProgress(weight = weight, goalKg = goalKg) }

            RecentWeights(entries = weight.recent, onDeleteWeight = onDeleteWeight)
        }
    }
}

@Composable
private fun changeText(weight: WeightSummary): String {
    val change = weight.changeKg ?: return stringResource(R.string.weight_one_entry)
    val since = formatWorkoutDate(weight.start.loggedAt)
    return when {
        abs(change) < 0.05f -> stringResource(R.string.weight_change_none, since)
        change < 0 -> stringResource(R.string.weight_change_down, formatKg(-change), since)
        else -> stringResource(R.string.weight_change_up, formatKg(change), since)
    }
}

@Composable
private fun chartDescription(weight: WeightSummary): String {
    val trend = stringResource(
        R.string.weight_chart_description,
        formatKg(weight.chartStart.weightKg),
        formatWorkoutDate(weight.chartStart.loggedAt),
        formatKg(weight.latest.weightKg),
        formatWorkoutDate(weight.latest.loggedAt),
    )
    val goal = weight.goalKg ?: return trend
    return trend + " " + stringResource(R.string.weight_chart_goal_description, formatKg(goal))
}

@Composable
private fun GoalProgress(weight: WeightSummary, goalKg: Float, modifier: Modifier = Modifier) {
    val goalText = when (val gap = weightGoalGap(weight.latest.weightKg, goalKg)) {
        is WeightGoalGap.ToLose -> stringResource(
            R.string.weight_goal_gap,
            formatKg(goalKg),
            stringResource(R.string.workouts_weight_to_lose, formatKg(gap.kg)),
        )
        is WeightGoalGap.ToGain -> stringResource(
            R.string.weight_goal_gap,
            formatKg(goalKg),
            stringResource(R.string.workouts_weight_to_gain, formatKg(gap.kg)),
        )
        WeightGoalGap.AtGoal -> stringResource(R.string.weight_goal_reached, formatKg(goalKg))
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = goalText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .testTag("weight_goal"),
            )
            weight.goalProgress?.let { progress ->
                Text(
                    text = stringResource(R.string.weight_goal_percent, (progress * 100).roundToInt()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
        weight.goalProgress?.let { progress ->
            val label = stringResource(R.string.weight_goal_progress_label)
            LinearProgressIndicator(
                progress = { progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .testTag("weight_goal_progress")
                    .semantics { contentDescription = label },
            )
        }
    }
}

/** The newest weigh-ins with a delete button each, to fix a typo. */
@Composable
private fun RecentWeights(
    entries: List<WeightEntry>,
    onDeleteWeight: (WeightEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.weight_recent),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .padding(bottom = 4.dp)
                .semantics { heading() },
        )
        entries.forEach { entry ->
            val date = formatWorkoutDate(entry.loggedAt)
            val kg = formatKg(entry.weightKg)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = date, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(text = stringResource(R.string.weight_kg, kg), style = MaterialTheme.typography.bodyMedium)
                // IconButton is 48 dp, big enough to tap. The label says which weigh-in it deletes.
                IconButton(
                    onClick = { onDeleteWeight(entry) },
                    modifier = Modifier.testTag("weight_delete_${entry.id}"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.weight_delete, kg, date),
                    )
                }
            }
        }
    }
}

@Composable
private fun LogWeightDialogView(
    dialog: LogWeightDialog,
    onInput: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val error = dialog.error
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weight_dialog_title)) },
        text = {
            OutlinedTextField(
                value = dialog.input,
                onValueChange = onInput,
                label = { Text(stringResource(R.string.weight_dialog_label)) },
                suffix = { Text(stringResource(R.string.weight_dialog_unit)) },
                singleLine = true,
                isError = error != null,
                // Read out as soon as it appears, so a TalkBack user hears why Save did nothing.
                supportingText = if (error == null) {
                    null
                } else {
                    {
                        Text(
                            text = stringResource(error.weightMessage()),
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weight_input"),
            )
        },
        confirmButton = {
            TextButton(onClick = onSave, modifier = Modifier.testTag("weight_save")) {
                Text(stringResource(R.string.weight_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.weight_dialog_cancel)) }
        },
    )
}

@StringRes
private fun FieldError.weightMessage(): Int = when (this) {
    FieldError.EMPTY -> R.string.weight_error_empty
    FieldError.NOT_A_NUMBER -> R.string.error_not_number
    FieldError.OUT_OF_RANGE -> R.string.error_weight_range
}

@Composable
private fun DeleteWeightDialog(entry: WeightEntry, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weight_delete_title)) },
        text = {
            Text(
                stringResource(R.string.weight_delete_message, formatKg(entry.weightKg), formatWorkoutDate(entry.loggedAt)),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("weight_delete_confirm")) {
                Text(stringResource(R.string.weight_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.weight_delete_cancel)) }
        },
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HistoryCard(summary: SessionSummary, modifier: Modifier = Modifier) {
    val session = summary.session
    HistoryRow(
        icon = R.drawable.ic_tab_workouts,
        tint = MaterialTheme.colorScheme.primary,
        container = MaterialTheme.colorScheme.surfaceContainerHigh,
        date = formatWorkoutDate(session.finishedAt ?: session.startedAt),
        duration = durationText(session),
        title = session.name,
        stats = stringResource(
            R.string.history_stats,
            pluralStringResource(R.plurals.exercise_count, summary.exerciseCount, summary.exerciseCount),
            pluralStringResource(R.plurals.set_count, summary.setCount, summary.setCount),
            formatKg(summary.totalVolumeKg),
        ),
        statsTag = "history_stats_${session.id}",
        modifier = modifier.testTag("history_${session.id}"),
    )
}

/** A walk or run in the history: date, time, then distance, pace and calories. Tap to see its map. */
@Composable
private fun RunHistoryCard(run: RunSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HistoryRow(
        icon = R.drawable.ic_run,
        tint = MaterialTheme.colorScheme.tertiary,
        container = MaterialTheme.colorScheme.tertiaryContainer,
        date = formatWorkoutDate(run.finishedAt),
        duration = RunFormat.duration(run.durationMs),
        title = stringResource(if (run.isWalk) R.string.history_walk else R.string.history_run),
        stats = stringResource(
            R.string.history_run_stats,
            RunFormat.km(run.distanceM),
            RunFormat.pace(run.paceSecondsPerKm),
            run.calories,
        ),
        statsTag = "run_stats_${run.id}",
        onClick = onClick,
        onClickLabel = stringResource(R.string.history_open_run),
        modifier = modifier.testTag("run_${run.id}"),
    )
}

/** One history card: a lime tile for a workout or an orange one for cardio, then the details. */
@Composable
private fun HistoryRow(
    icon: Int,
    tint: Color,
    container: Color,
    date: String,
    duration: String,
    title: String,
    stats: String,
    statsTag: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    // A tappable card says what a tap does ("Open on map"), so TalkBack can read it out.
    val clickModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier
            .clip(CardDefaults.shape)
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(icon = icon, tint = tint, container = container)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = duration, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stats,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(statsTag),
                )
            }
        }
    }
}

@Composable
private fun durationText(session: WorkoutSession): String {
    val minutes = durationMinutes(session) ?: 0L
    if (minutes < 1) return stringResource(R.string.history_duration_short)
    val (hours, rest) = splitHoursMinutes(minutes)
    return if (hours == 0L) {
        stringResource(R.string.history_duration_minutes, rest)
    } else {
        stringResource(R.string.history_duration_hours, hours, rest)
    }
}

@Composable
private fun EmptyHistory(onStartWorkout: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.progress_empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.progress_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onStartWorkout, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.progress_empty_button))
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun ProgressScreenPreview() {
    val session = WorkoutSession(1, "upper", "Upper body", startedAt = 0, finishedAt = 52 * 60_000L)
    val day = 24 * 60 * 60_000L
    val weights = listOf(84.6f, 84.1f, 83.9f, 83.2f, 82.9f, 82.4f)
        .mapIndexed { index, kg -> WeightEntry(id = index + 1L, loggedAt = index * 4 * day, weightKg = kg) }
    RepTrackTheme {
        ProgressScreen(
            state = ProgressUiState(
                isLoading = false,
                weight = summarizeWeights(weights, goalKg = 78f),
                items = listOf(
                    HistoryItem.Run(RunSummary(2, 0, 60 * 60_000L, 5_020.0, 26 * 60_000L, 410)),
                    HistoryItem.Workout(SessionSummary(session, exerciseCount = 5, setCount = 15, totalVolumeKg = 4250f)),
                ),
            ),
            onStartWorkout = {},
            onLogWeight = {},
            onWeightInput = {},
            onSaveWeight = {},
            onDismissLogWeight = {},
            onDeleteWeight = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onRetry = {},
            onDismissMessage = {},
        )
    }
}

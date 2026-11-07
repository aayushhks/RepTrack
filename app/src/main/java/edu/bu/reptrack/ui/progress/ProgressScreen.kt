package edu.bu.reptrack.ui.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.profile.formatKg
import edu.bu.reptrack.domain.workout.SessionSummary
import edu.bu.reptrack.domain.workout.WorkoutSession
import edu.bu.reptrack.domain.workout.durationMinutes
import edu.bu.reptrack.ui.theme.RepTrackTheme

/** Connects the Progress tab to its ViewModel. */
@Composable
fun ProgressRoute(
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(state = state, onStartWorkout = onStartWorkout, modifier = modifier)
}

/** Progress tab: finished workouts, newest first. The weight chart comes in the beta. Stateless. */
@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            .testTag("screen_progress"),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.progress_history_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .semantics { heading() },
            )
        }
        if (state.history.isEmpty()) {
            item { EmptyHistory(onStartWorkout = onStartWorkout) }
        } else {
            items(state.history, key = { it.session.id }) { summary ->
                HistoryCard(summary = summary)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HistoryCard(summary: SessionSummary, modifier: Modifier = Modifier) {
    val session = summary.session
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("history_${session.id}"),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatWorkoutDate(session.finishedAt ?: session.startedAt),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = durationText(session),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(text = session.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    R.string.history_stats,
                    pluralStringResource(R.plurals.exercise_count, summary.exerciseCount, summary.exerciseCount),
                    pluralStringResource(R.plurals.set_count, summary.setCount, summary.setCount),
                    formatKg(summary.totalVolumeKg),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("history_stats_${session.id}"),
            )
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

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun ProgressScreenPreview() {
    val session = WorkoutSession(1, "upper", "Upper body", startedAt = 0, finishedAt = 52 * 60_000L)
    RepTrackTheme {
        ProgressScreen(
            state = ProgressUiState(
                isLoading = false,
                history = listOf(SessionSummary(session, exerciseCount = 5, setCount = 15, totalVolumeKg = 4250f)),
            ),
            onStartWorkout = {},
        )
    }
}

package edu.bu.reptrack.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.workout.PlanDay
import edu.bu.reptrack.domain.workout.PlanDayStatus
import edu.bu.reptrack.domain.workout.WorkoutTemplate
import edu.bu.reptrack.ui.components.IconTile
import edu.bu.reptrack.ui.components.Overline
import edu.bu.reptrack.ui.components.VoltCard
import edu.bu.reptrack.ui.components.labelRes
import edu.bu.reptrack.ui.components.readableWidth
import edu.bu.reptrack.ui.theme.RepTrackTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Connects the week plan to its ViewModel. */
@Composable
fun PlanRoute(
    onBack: () -> Unit,
    onOpenRoutine: (routineId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlanViewModel = viewModel(factory = PlanViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlanScreen(
        state = state,
        onBack = onBack,
        onToggleDay = viewModel::toggleDay,
        onResetDays = viewModel::resetDays,
        onOpenRoutine = onOpenRoutine,
        modifier = modifier,
    )
}

/**
 * The week plan: the days the user trains, this week day by day (what was done, what is today,
 * what is coming, days off), and the plan's routines to open and edit. Stateless.
 */
@Composable
fun PlanScreen(
    state: PlanUiState,
    onBack: () -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onResetDays: () -> Unit,
    onOpenRoutine: (routineId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_week_plan"),
    ) {
        Row(
            modifier = Modifier
                .readableWidth()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("plan_back")) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.plan_back))
            }
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = stringResource(R.string.plan_title).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                if (!state.isLoading) {
                    Text(
                        text = stringResource(R.string.plan_summary, stringResource(state.plan.type.labelRes()), state.trainingDays.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        Column(
            modifier = Modifier
                .readableWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Overline(stringResource(R.string.plan_training_days), modifier = Modifier.semantics { heading() })
            Text(
                text = stringResource(R.string.plan_training_days_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DayPicker(selected = state.trainingDays, onToggle = onToggleDay)
            if (state.customDays) {
                TextButton(onClick = onResetDays, modifier = Modifier.testTag("plan_reset_days")) {
                    Text(stringResource(R.string.plan_use_default_days))
                }
            }
            if (state.saveFailed) {
                Text(
                    text = stringResource(R.string.plan_save_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag("plan_save_failed"),
                )
            }

            Overline(
                stringResource(R.string.plan_this_week),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { heading() },
            )
            VoltCard(contentPadding = 6.dp, modifier = Modifier.testTag("plan_week")) {
                state.week.forEach { day ->
                    val routineId = day.routineId?.takeIf { id -> state.routines.any { it.id == id } }
                    WeekDayRow(day = day, onOpen = routineId?.let { id -> { onOpenRoutine(id) } })
                }
            }

            Overline(
                stringResource(R.string.plan_routines),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { heading() },
            )
            state.routines.forEach { routine ->
                RoutineCard(routine = routine, onOpen = { onOpenRoutine(routine.id) })
            }
        }
    }
}

/**
 * Seven chips, Monday first, each at least 48 dp wide. They wrap onto a second line on a narrow
 * phone instead of shrinking. Each says its full day name to TalkBack.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayPicker(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    val locale = Locale.getDefault()
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DayOfWeek.entries.forEach { day ->
            val fullName = day.getDisplayName(TextStyle.FULL, locale)
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                label = { Text(day.getDisplayName(TextStyle.NARROW, locale)) },
                modifier = Modifier
                    .widthIn(min = 48.dp)
                    .semantics { contentDescription = fullName }
                    .testTag("plan_day_${day.name.lowercase()}"),
            )
        }
    }
}

/** "Thu, Nov 26 · Upper body · Today". Tap to open the routine. */
@Composable
private fun WeekDayRow(day: PlanDay, onOpen: (() -> Unit)?) {
    val locale = Locale.getDefault()
    val openLabel = stringResource(R.string.workouts_open_routine)
    val background = if (day.isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest
    val content = if (day.isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val muted = if (day.isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val click = if (onOpen == null) Modifier else Modifier.clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .then(click)
            .semantics(mergeDescendants = true) {}
            .heightIn(min = 52.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("plan_day_row_${day.date.dayOfWeek.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.width(72.dp)) {
            Text(
                text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                style = MaterialTheme.typography.titleSmall,
                color = content,
            )
            Text(
                text = day.date.format(DateTimeFormatter.ofPattern("MMM d", locale)),
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
        Text(
            text = day.routineName ?: stringResource(R.string.plan_rest),
            style = MaterialTheme.typography.bodyLarge,
            color = if (day.routineName == null) muted else content,
            modifier = Modifier.weight(1f),
        )
        statusLabel(day.status)?.let { label ->
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = muted)
        }
    }
}

@Composable
private fun statusLabel(status: PlanDayStatus): String? = when (status) {
    PlanDayStatus.DONE -> stringResource(R.string.plan_status_done)
    PlanDayStatus.TODAY -> stringResource(R.string.plan_status_today)
    PlanDayStatus.MISSED -> stringResource(R.string.plan_status_missed)
    PlanDayStatus.RAN -> stringResource(R.string.plan_status_ran)
    PlanDayStatus.PLANNED, PlanDayStatus.REST -> null
}

/** A routine: name, how many exercises, whether it was edited. Tap to open it. */
@Composable
private fun RoutineCard(routine: WorkoutTemplate, onOpen: () -> Unit) {
    val count = stringResource(R.string.workouts_exercise_count, routine.exercises.size)
    val subtitle = if (routine.isEdited) stringResource(R.string.plan_routine_summary, count, stringResource(R.string.plan_edited)) else count
    VoltCard(
        contentPadding = 14.dp,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = stringResource(R.string.workouts_open_routine), role = Role.Button, onClick = onOpen)
            .testTag("plan_routine_${routine.id}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(
                icon = R.drawable.ic_tab_workouts,
                tint = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PlanScreenPreview() {
    val monday = LocalDate.parse("2026-11-23")
    RepTrackTheme {
        PlanScreen(
            state = PlanUiState(
                isLoading = false,
                trainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                week = (0L until 7L).map { offset ->
                    val date = monday.plusDays(offset)
                    PlanDay(date, offset == 0L, offset == 0L || offset == 3L, null, if (offset == 0L) "Full body A" else null, if (offset == 0L) PlanDayStatus.TODAY else PlanDayStatus.REST)
                },
            ),
            onBack = {},
            onToggleDay = {},
            onResetDays = {},
            onOpenRoutine = {},
        )
    }
}

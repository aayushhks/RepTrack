package edu.bu.reptrack.ui.workouts

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import edu.bu.reptrack.domain.workout.DayActivity
import edu.bu.reptrack.domain.workout.DayKind
import edu.bu.reptrack.domain.workout.PlanDay
import edu.bu.reptrack.domain.workout.PlanDayStatus
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.domain.workout.WeekActivity
import edu.bu.reptrack.domain.workout.WorkoutTemplate
import edu.bu.reptrack.ui.components.GoalRing
import edu.bu.reptrack.ui.components.IconTile
import edu.bu.reptrack.ui.components.Overline
import edu.bu.reptrack.ui.components.VoltCard
import edu.bu.reptrack.ui.components.labelRes
import edu.bu.reptrack.ui.components.readableWidth
import edu.bu.reptrack.ui.theme.RepTrackTheme
import edu.bu.reptrack.ui.theme.statStyle
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

/** Connects the Workouts tab to its ViewModel. */
@Composable
fun WorkoutsRoute(
    onStartWorkout: (routineId: String) -> Unit,
    onStartRun: () -> Unit,
    onSetUpPlan: () -> Unit,
    onOpenPlan: (place: TrainingPlace) -> Unit,
    onOpenRoutine: (routineId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutsViewModel = viewModel(factory = WorkoutsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutsScreen(
        state = state,
        onPlaceSelected = viewModel::onPlaceSelected,
        onStartWorkout = onStartWorkout,
        onStartRun = onStartRun,
        onSetUpPlan = onSetUpPlan,
        onOpenPlan = { onOpenPlan(state.place) },
        onOpenRoutine = onOpenRoutine,
        modifier = modifier,
    )
}

/**
 * Workouts tab: the streak, the goal and this week, then today's workout in a lime card,
 * the plan's other routines and a walk or run. Stateless.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    state: WorkoutsUiState,
    onPlaceSelected: (TrainingPlace) -> Unit,
    onStartWorkout: (routineId: String) -> Unit,
    onStartRun: () -> Unit,
    onSetUpPlan: () -> Unit,
    modifier: Modifier = Modifier,
    /** Opens the week plan: training days and which routine goes on which day. */
    onOpenPlan: () -> Unit = {},
    /** Opens a routine to see and edit its exercises. */
    onOpenRoutine: (routineId: String) -> Unit = {},
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
            .readableWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .testTag("screen_workouts"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header(streakDays = state.streakDays, today = state.week.firstOrNull { it.isToday }?.date)

        val profile = state.profile
        if (profile != null) {
            GoalCard(profile = profile, goalProgress = state.goalProgress, week = state.week)
        } else {
            SetUpPlanCard(onSetUpPlan = onSetUpPlan)
            if (state.week.isNotEmpty()) {
                VoltCard { WeekStrip(week = state.week) }
            }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TrainingPlace.entries.forEachIndexed { index, place ->
                SegmentedButton(
                    selected = state.place == place,
                    onClick = { onPlaceSelected(place) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = TrainingPlace.entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        activeBorderColor = MaterialTheme.colorScheme.primaryContainer,
                        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    icon = {},
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(place.labelRes()))
                }
            }
        }

        val next = state.nextRoutine
        if (profile != null && next != null) {
            TodayCard(
                plan = state.plan,
                daysPerWeek = state.trainingDays.size,
                routine = next,
                workoutsThisWeek = state.workoutsThisWeek,
                onStart = { onStartWorkout(next.id) },
                onOpen = { onOpenRoutine(next.id) },
            )
            WeekPlanCard(week = state.weekPlan, onOpen = onOpenPlan)
            if (state.otherRoutines.isNotEmpty()) {
                SectionLabel(R.string.workouts_routines_label)
                state.otherRoutines.forEach { routine ->
                    RoutineRow(routine = routine, onStart = { onStartWorkout(routine.id) }, onOpen = { onOpenRoutine(routine.id) })
                }
            }
        } else {
            WeekPlanCard(week = state.weekPlan, onOpen = onOpenPlan)
            SectionLabel(R.string.workouts_routines_label)
            state.routines.forEach { routine ->
                RoutineRow(routine = routine, onStart = { onStartWorkout(routine.id) }, onOpen = { onOpenRoutine(routine.id) })
            }
        }

        SectionLabel(R.string.workouts_cardio_label)
        RunRow(onStart = onStartRun)
    }
}

@Composable
private fun Header(streakDays: Int, today: LocalDate?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (today != null) {
                Text(
                    text = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()).format(today),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.workouts_greeting).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (streakDays > 0) {
            val streakText = pluralStringResource(R.plurals.streak_days, streakDays, streakDays)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    // The tag goes first: clearAndSetSemantics would also clear a tag set after it.
                    .testTag("streak")
                    .clearAndSetSemantics { contentDescription = streakText },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_flame),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(text = streakDays.toString(), style = statStyle(18.sp))
                    Text(
                        text = stringResource(R.string.workouts_streak_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The goal ring, the weight from now to the goal, and this week's days. */
@Composable
private fun GoalCard(profile: UserProfile, goalProgress: Float?, week: List<DayActivity>) {
    VoltCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val progress = goalProgress ?: 0f
            GoalRing(
                progress = progress,
                description = stringResource(R.string.workouts_goal_ring, (progress * 100).toInt()),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.workouts_goal, stringResource(profile.goal.labelRes())),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.workouts_weight_from_to,
                        formatKg(profile.weightKg),
                        formatKg(profile.goalWeightKg),
                    ),
                    style = statStyle(30.sp),
                )
                Text(
                    text = weightGapText(profile),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("weight_gap"),
                )
            }
        }
        if (week.isNotEmpty()) WeekStrip(week = week)
    }
}

@Composable
private fun weightGapText(profile: UserProfile): String =
    when (val gap = weightGoalGap(profile.weightKg, profile.goalWeightKg)) {
        is WeightGoalGap.ToLose -> stringResource(R.string.workouts_weight_to_lose, formatKg(gap.kg))
        is WeightGoalGap.ToGain -> stringResource(R.string.workouts_weight_to_gain, formatKg(gap.kg))
        WeightGoalGap.AtGoal -> stringResource(R.string.workouts_weight_at_goal)
    }

/**
 * Monday to Sunday as dots: lime for a workout, orange for a walk or run, a ring for today.
 * The colors also differ in lightness, and TalkBack reads one summary sentence instead.
 */
@Composable
private fun WeekStrip(week: List<DayActivity>) {
    val summary = stringResource(
        R.string.workouts_week_summary,
        pluralStringResource(R.plurals.workout_count, week.count { it.kind == DayKind.WORKOUT }, week.count { it.kind == DayKind.WORKOUT }),
        pluralStringResource(R.plurals.run_count, week.count { it.kind == DayKind.RUN }, week.count { it.kind == DayKind.RUN }),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("week_strip")
            .clearAndSetSemantics { contentDescription = summary },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        week.forEach { day ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = day.date.dayOfWeek.getDisplayName(DateTextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                val fill = when (day.kind) {
                    DayKind.WORKOUT -> MaterialTheme.colorScheme.primaryContainer
                    DayKind.RUN -> MaterialTheme.colorScheme.tertiary
                    DayKind.NONE -> MaterialTheme.colorScheme.surfaceContainerHigh
                }
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = fill,
                    border = if (day.isToday && day.kind == DayKind.NONE) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                ) {}
            }
        }
    }
}

/** The one loud element on the tab: today's workout on a lime card, with Start. */
@Composable
private fun TodayCard(
    plan: StarterPlan,
    daysPerWeek: Int,
    routine: WorkoutTemplate,
    workoutsThisWeek: Int,
    onStart: () -> Unit,
    onOpen: () -> Unit,
) {
    val onHero = MaterialTheme.colorScheme.onPrimaryContainer
    val startLabel = stringResource(R.string.workouts_start_routine, routine.name)
    val openLabel = stringResource(R.string.workouts_edit_routine_description, routine.name)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = onHero,
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Overline(stringResource(R.string.workouts_plan_label), color = onHero, modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.workouts_week_progress, minOf(workoutsThisWeek, daysPerWeek), daysPerWeek),
                    style = MaterialTheme.typography.labelMedium,
                    color = onHero,
                )
            }
            Text(
                text = stringResource(plan.type.labelRes()),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .semantics { heading() }
                    .testTag("plan_name"),
            )
            Text(
                text = stringResource(R.string.plan_days, daysPerWeek),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.size(6.dp))
            Overline(stringResource(R.string.workouts_up_next), color = onHero)
            Text(
                text = routine.name.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = routine.exercises.joinToString(separator = " · "),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // See and edit the exercises. Ink on lime, like the Start button.
                TextButton(
                    onClick = onOpen,
                    colors = ButtonDefaults.textButtonColors(contentColor = onHero),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = openLabel }
                        .testTag("open_${routine.id}"),
                ) {
                    Text(
                        text = stringResource(R.string.workouts_exercise_count, routine.exercises.size),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.size(18.dp))
                }
                // Ink pill with lime text: the one place the flipped colors are used.
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = onHero,
                        contentColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .semantics { contentDescription = startLabel }
                        .testTag("start_${routine.id}"),
                ) {
                    Icon(painterResource(R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.workouts_start), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun SetUpPlanCard(onSetUpPlan: () -> Unit) {
    val onHero = MaterialTheme.colorScheme.onPrimaryContainer
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = onHero,
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.workouts_setup_title).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = stringResource(R.string.workouts_setup_message), style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = onSetUpPlan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = onHero,
                    contentColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.workouts_setup_button))
            }
        }
    }
}

/** One routine: a dumbbell tile, its name and size, and Start. Tap the card to see its exercises. */
@Composable
private fun RoutineRow(routine: WorkoutTemplate, onStart: () -> Unit, onOpen: () -> Unit) {
    val startLabel = stringResource(R.string.workouts_start_routine, routine.name)
    val count = stringResource(R.string.workouts_exercise_count, routine.exercises.size)
    ListRow(
        icon = R.drawable.ic_tab_workouts,
        iconTint = MaterialTheme.colorScheme.primary,
        iconContainer = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = routine.name,
        subtitle = if (routine.isEdited) stringResource(R.string.plan_routine_summary, count, stringResource(R.string.plan_edited)) else count,
        startLabel = startLabel,
        startTag = "start_${routine.id}",
        onStart = onStart,
        onOpen = onOpen,
        openTag = "open_${routine.id}",
    )
}

/**
 * The week at a glance, "Mon Full body A · Thu Full body B", with a way into the week plan to
 * change the days and edit the routines.
 */
@Composable
private fun WeekPlanCard(week: List<PlanDay>, onOpen: () -> Unit) {
    val locale = Locale.getDefault()
    val summary = week
        .filter { it.isTrainingDay || it.status == PlanDayStatus.DONE }
        .joinToString("  ·  ") { day ->
            val name = day.date.dayOfWeek.getDisplayName(DateTextStyle.SHORT, locale)
            day.routineName?.let { "$name $it" } ?: name
        }
    ListRow(
        icon = R.drawable.ic_calendar,
        iconTint = MaterialTheme.colorScheme.primary,
        iconContainer = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = stringResource(R.string.workouts_week_plan),
        subtitle = summary.ifEmpty { stringResource(R.string.workouts_week_plan_edit) },
        startLabel = null,
        startTag = null,
        onStart = null,
        onOpen = onOpen,
        openTag = "week_plan_card",
        openLabel = stringResource(R.string.workouts_open_week_plan),
    )
}

/** A GPS walk or run, in orange like all cardio. */
@Composable
private fun RunRow(onStart: () -> Unit) {
    ListRow(
        icon = R.drawable.ic_run,
        iconTint = MaterialTheme.colorScheme.tertiary,
        iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
        title = stringResource(R.string.workouts_run_title),
        subtitle = stringResource(R.string.workouts_run_message),
        startLabel = stringResource(R.string.workouts_run_start),
        startTag = "start_run",
        onStart = onStart,
    )
}

/**
 * A card with a tile, a title and a line under it, and a Start button when [onStart] is given.
 * With [onOpen] the whole card is tappable too; Start stays its own button.
 */
@Composable
private fun ListRow(
    icon: Int,
    iconTint: Color,
    iconContainer: Color,
    title: String,
    subtitle: String,
    startLabel: String?,
    startTag: String?,
    onStart: (() -> Unit)?,
    onOpen: (() -> Unit)? = null,
    openTag: String? = null,
    openLabel: String = stringResource(R.string.workouts_open_routine),
) {
    val open = if (onOpen == null) {
        Modifier
    } else {
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen)
            .then(if (openTag != null) Modifier.testTag(openTag) else Modifier)
    }
    VoltCard(contentPadding = 14.dp, modifier = open) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(icon = icon, tint = iconTint, container = iconContainer)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onStart != null) {
                FilledTonalButton(
                    onClick = onStart,
                    // TalkBack says "Start Upper body" instead of just "Start".
                    modifier = Modifier
                        .semantics { if (startLabel != null) contentDescription = startLabel }
                        .then(if (startTag != null) Modifier.testTag(startTag) else Modifier),
                ) {
                    Text(stringResource(R.string.workouts_start))
                }
            } else {
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(@StringRes text: Int) {
    Overline(
        text = stringResource(text),
        modifier = Modifier
            .padding(top = 4.dp)
            .semantics { heading() },
    )
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun WorkoutsScreenPreview() {
    val plan = StarterPlan.suggest(FitnessGoal.BUILD_MUSCLE, TrainingPlace.GYM)
    val routines = StarterRoutines.forPlan(plan.type)
    val now = LocalDate.parse("2026-11-18").atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    val day = 24 * 60 * 60_000L
    RepTrackTheme {
        WorkoutsScreen(
            state = WorkoutsUiState(
                isLoading = false,
                profile = UserProfile(178f, 82.4f, 78f, FitnessGoal.LOSE_WEIGHT, TrainingPlace.GYM),
                place = TrainingPlace.GYM,
                plan = plan,
                routines = routines,
                nextRoutine = routines.last(),
                week = WeekActivity.thisWeek(listOf(now - 2 * day), listOf(now - day), now, ZoneOffset.UTC),
                streakDays = 2,
                workoutsThisWeek = 1,
                goalProgress = 0.33f,
            ),
            onPlaceSelected = {},
            onStartWorkout = {},
            onStartRun = {},
            onSetUpPlan = {},
        )
    }
}

package edu.bu.reptrack.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.workout.PlanDay
import edu.bu.reptrack.domain.workout.PlanDayStatus
import edu.bu.reptrack.domain.workout.RoutineName
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
    // A routine the user just made opens right away, so they can add its exercises.
    LaunchedEffect(state.createdRoutineId) {
        state.createdRoutineId?.let { id ->
            viewModel.onCreatedRoutineOpened()
            onOpenRoutine(id)
        }
    }
    PlanScreen(
        state = state,
        onBack = onBack,
        onToggleDay = viewModel::toggleDay,
        onResetDays = viewModel::resetDays,
        onOpenRoutine = onOpenRoutine,
        modifier = modifier,
        onStartFromSuggested = viewModel::startFromSuggested,
        onStartFromScratch = viewModel::startFromScratch,
        onAddRoutine = viewModel::addRoutine,
        onRenameRoutine = viewModel::renameRoutine,
        onDeleteRoutine = viewModel::deleteRoutine,
        onSetDayRoutine = viewModel::setDayRoutine,
        onUseSuggestedPlan = viewModel::useSuggestedPlan,
    )
}

/**
 * The week plan: the days the user trains, this week day by day (what was done, what is today,
 * what is coming, days off), and the plan's routines to open and edit. The user can make the
 * plan their own: then they name their routines and pick one for each day of the week.
 * Stateless, apart from which dialog is open.
 */
@Composable
fun PlanScreen(
    state: PlanUiState,
    onBack: () -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onResetDays: () -> Unit,
    onOpenRoutine: (routineId: String) -> Unit,
    modifier: Modifier = Modifier,
    onStartFromSuggested: () -> Unit = {},
    onStartFromScratch: (name: String) -> Unit = {},
    onAddRoutine: (name: String) -> Unit = {},
    onRenameRoutine: (routineId: String, name: String) -> Unit = { _, _ -> },
    onDeleteRoutine: (routineId: String) -> Unit = {},
    onSetDayRoutine: (day: DayOfWeek, routineId: String?) -> Unit = { _, _ -> },
    onUseSuggestedPlan: () -> Unit = {},
) {
    // Which dialog is open: "scratch", "new", "rename:<id>", "delete:<id>" or "suggested". Kept on rotation.
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    PlanDialog(
        dialog = dialog,
        routines = state.routines,
        onDismiss = { dialog = null },
        onStartFromScratch = { name -> dialog = null; onStartFromScratch(name) },
        onAddRoutine = { name -> dialog = null; onAddRoutine(name) },
        onRenameRoutine = { id, name -> dialog = null; onRenameRoutine(id, name) },
        onDeleteRoutine = { id -> dialog = null; onDeleteRoutine(id) },
        onUseSuggestedPlan = { dialog = null; onUseSuggestedPlan() },
    )

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
                        text = stringResource(
                            R.string.plan_summary,
                            if (state.isCustom) stringResource(R.string.plan_my_plan) else stringResource(state.plan.type.labelRes()),
                            state.trainingDays.size,
                        ),
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
            if (state.isCustom) {
                Overline(stringResource(R.string.plan_your_week), modifier = Modifier.semantics { heading() })
                Text(
                    text = stringResource(R.string.plan_your_week_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // One list: this week's dates and how each day went, and what each day is set to.
                VoltCard(contentPadding = 6.dp, modifier = Modifier.testTag("plan_schedule")) {
                    state.week.forEach { day ->
                        OwnWeekDayRow(
                            day = day,
                            routineId = state.schedule[day.date.dayOfWeek],
                            routines = state.routines,
                            onOpenRoutine = onOpenRoutine,
                            onPick = { routineId -> onSetDayRoutine(day.date.dayOfWeek, routineId) },
                        )
                    }
                }
            } else {
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

            if (!state.isCustom) {
                Overline(
                    stringResource(R.string.plan_this_week),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .semantics { heading() },
                )
                VoltCard(contentPadding = 6.dp, modifier = Modifier.testTag("plan_week")) {
                    state.week.forEach { day ->
                        val routineId = day.routineId?.takeIf { id -> state.routines.any { it.id == id } }
                        WeekDayRow(
                            day = day,
                            onClick = routineId?.let { id -> { onOpenRoutine(id) } },
                            clickLabel = stringResource(R.string.workouts_open_routine),
                        )
                    }
                }
            }

            Overline(
                stringResource(R.string.plan_routines),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { heading() },
            )
            state.routines.forEach { routine ->
                RoutineCard(
                    routine = routine,
                    onOpen = { onOpenRoutine(routine.id) },
                    menu = if (state.isCustom) {
                        {
                            RoutineCardMenu(
                                name = routine.name,
                                routineId = routine.id,
                                canDelete = state.canDeleteRoutine,
                                onRename = { dialog = "rename:${routine.id}" },
                                onDelete = { dialog = "delete:${routine.id}" },
                            )
                        }
                    } else {
                        null
                    },
                )
            }
            if (state.isCustom) {
                OutlinedButton(
                    onClick = { dialog = "new" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("plan_new_routine"),
                ) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.plan_new_routine))
                }
                TextButton(
                    onClick = { dialog = "suggested" },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("plan_use_suggested"),
                ) {
                    Text(stringResource(R.string.plan_use_suggested))
                }
            } else {
                MakeItYourOwnCard(
                    onStartFromSuggested = onStartFromSuggested,
                    onStartFromScratch = { dialog = "scratch" },
                )
            }
        }
    }
}

/**
 * On the user's own plan, one day of this week: its date, its routine and how it went. Tap for a
 * menu to open the routine, give the day another routine, or make it a rest day. The change
 * holds for that day every week.
 */
@Composable
private fun OwnWeekDayRow(
    day: PlanDay,
    routineId: String?,
    routines: List<WorkoutTemplate>,
    onOpenRoutine: (String) -> Unit,
    onPick: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val dayOfWeek = day.date.dayOfWeek
    val routine = routines.firstOrNull { it.id == routineId }
    Box {
        WeekDayRow(
            day = day,
            onClick = { open = true },
            clickLabel = stringResource(R.string.plan_choose_for_day, dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())),
            tag = "plan_schedule_${dayOfWeek.name.lowercase()}",
            showChevron = true,
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (routine != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.plan_open_routine, routine.name)) },
                    onClick = { open = false; onOpenRoutine(routine.id) },
                    modifier = Modifier.testTag("plan_open_${dayOfWeek.name.lowercase()}"),
                )
                HorizontalDivider()
            }
            routines.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.name) },
                    onClick = { open = false; onPick(choice.id) },
                    trailingIcon = if (choice.id == routineId) {
                        { Icon(painterResource(R.drawable.ic_check), contentDescription = stringResource(R.string.plan_current)) }
                    } else {
                        null
                    },
                    modifier = Modifier.testTag("plan_pick_${choice.id}"),
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plan_day_off)) },
                onClick = { open = false; onPick(null) },
                trailingIcon = if (routine == null) {
                    { Icon(painterResource(R.drawable.ic_check), contentDescription = stringResource(R.string.plan_current)) }
                } else {
                    null
                },
                modifier = Modifier.testTag("plan_pick_rest"),
            )
        }
    }
}

/** The way to make the suggested plan the user's own, from it or from nothing. */
@Composable
private fun MakeItYourOwnCard(onStartFromSuggested: () -> Unit, onStartFromScratch: () -> Unit) {
    VoltCard(modifier = Modifier.padding(top = 8.dp).testTag("plan_make_own")) {
        Overline(stringResource(R.string.plan_make_own_title), modifier = Modifier.semantics { heading() })
        Text(
            text = stringResource(R.string.plan_make_own_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onStartFromSuggested,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("plan_start_from_suggested"),
        ) {
            Text(stringResource(R.string.plan_start_from_suggested))
        }
        OutlinedButton(
            onClick = onStartFromScratch,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("plan_start_from_scratch"),
        ) {
            Text(stringResource(R.string.plan_start_from_scratch))
        }
    }
}

/** Rename or delete one of the user's own routines. */
@Composable
private fun RoutineCardMenu(name: String, routineId: String, canDelete: Boolean, onRename: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.testTag("plan_routine_menu_$routineId")) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.routine_more, name))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plan_rename)) },
                onClick = { open = false; onRename() },
                modifier = Modifier.testTag("plan_rename_$routineId"),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plan_delete)) },
                onClick = { open = false; onDelete() },
                enabled = canDelete,
                modifier = Modifier.testTag("plan_delete_$routineId"),
            )
        }
    }
}

/** The open dialog, if any: a routine name to type, or a delete to confirm. */
@Composable
private fun PlanDialog(
    dialog: String?,
    routines: List<WorkoutTemplate>,
    onDismiss: () -> Unit,
    onStartFromScratch: (String) -> Unit,
    onAddRoutine: (String) -> Unit,
    onRenameRoutine: (String, String) -> Unit,
    onDeleteRoutine: (String) -> Unit,
    onUseSuggestedPlan: () -> Unit,
) {
    when {
        dialog == null -> Unit
        dialog == "scratch" -> RoutineNameDialog(
            title = stringResource(R.string.plan_first_routine_title),
            initial = "",
            confirmLabel = stringResource(R.string.plan_create),
            onConfirm = onStartFromScratch,
            onDismiss = onDismiss,
        )
        dialog == "new" -> RoutineNameDialog(
            title = stringResource(R.string.plan_new_routine),
            initial = "",
            confirmLabel = stringResource(R.string.plan_create),
            onConfirm = onAddRoutine,
            onDismiss = onDismiss,
        )
        dialog.startsWith("rename:") -> {
            val routine = routines.firstOrNull { it.id == dialog.removePrefix("rename:") } ?: return
            RoutineNameDialog(
                title = stringResource(R.string.plan_rename_title),
                initial = routine.name,
                confirmLabel = stringResource(R.string.plan_save),
                onConfirm = { name -> onRenameRoutine(routine.id, name) },
                onDismiss = onDismiss,
            )
        }
        dialog.startsWith("delete:") -> {
            val routine = routines.firstOrNull { it.id == dialog.removePrefix("delete:") } ?: return
            ConfirmDialog(
                title = stringResource(R.string.plan_delete_title, routine.name),
                message = stringResource(R.string.plan_delete_message),
                confirmLabel = stringResource(R.string.plan_delete),
                confirmTag = "plan_delete_confirm",
                onConfirm = { onDeleteRoutine(routine.id) },
                onDismiss = onDismiss,
            )
        }
        dialog == "suggested" -> ConfirmDialog(
            title = stringResource(R.string.plan_use_suggested_title),
            message = stringResource(R.string.plan_use_suggested_message),
            confirmLabel = stringResource(R.string.plan_use_suggested_confirm),
            confirmTag = "plan_use_suggested_confirm",
            onConfirm = onUseSuggestedPlan,
            onDismiss = onDismiss,
        )
    }
}

/** Asks for a routine name. Save stays off until there is one. */
@Composable
private fun RoutineNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val clean = RoutineName.clean(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= RoutineName.MAX_LENGTH) text = it },
                label = { Text(stringResource(R.string.plan_routine_name)) },
                placeholder = { Text(stringResource(R.string.plan_routine_name_hint)) },
                singleLine = true,
                supportingText = { Text(stringResource(R.string.plan_routine_name_count, text.length, RoutineName.MAX_LENGTH)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { clean?.let(onConfirm) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plan_name_input"),
            )
        },
        confirmButton = {
            TextButton(onClick = { clean?.let(onConfirm) }, enabled = clean != null, modifier = Modifier.testTag("plan_name_save")) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.plan_cancel)) }
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmTag: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag(confirmTag),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.plan_cancel)) }
        },
    )
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
private fun WeekDayRow(
    day: PlanDay,
    onClick: (() -> Unit)?,
    clickLabel: String,
    tag: String = "plan_day_row_${day.date.dayOfWeek.name.lowercase()}",
    showChevron: Boolean = false,
) {
    val locale = Locale.getDefault()
    val background = if (day.isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest
    val content = if (day.isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val muted = if (day.isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val click = if (onClick == null) Modifier else Modifier.clickable(onClickLabel = clickLabel, role = Role.Button, onClick = onClick)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .then(click)
            .semantics(mergeDescendants = true) {}
            .heightIn(min = 52.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(tag),
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
        if (showChevron) {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = muted,
                modifier = Modifier.size(20.dp),
            )
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

/** A routine: name, how many exercises, whether it was edited. Tap to open it. [menu] goes at the end. */
@Composable
private fun RoutineCard(routine: WorkoutTemplate, onOpen: () -> Unit, menu: (@Composable () -> Unit)? = null) {
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
            if (menu != null) {
                menu()
            } else {
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
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

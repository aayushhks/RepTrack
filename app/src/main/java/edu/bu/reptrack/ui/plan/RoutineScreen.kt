package edu.bu.reptrack.ui.plan

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.ui.components.Overline
import edu.bu.reptrack.ui.components.VoltCard
import edu.bu.reptrack.ui.components.readableWidth
import edu.bu.reptrack.ui.theme.RepTrackTheme
import edu.bu.reptrack.ui.theme.statStyle
import java.util.Locale

/** Connects the routine screen to its ViewModel. */
@Composable
fun RoutineRoute(
    onBack: () -> Unit,
    onStart: (routineId: String) -> Unit,
    onAdd: (routineId: String) -> Unit,
    onSwap: (routineId: String, index: Int) -> Unit,
    onHowTo: (exerciseName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutineViewModel = viewModel(factory = RoutineViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RoutineScreen(
        state = state,
        onBack = onBack,
        onStart = { onStart(viewModel.routineId) },
        onAdd = { onAdd(viewModel.routineId) },
        onSwap = { index -> onSwap(viewModel.routineId, index) },
        onRemove = viewModel::remove,
        onMoveUp = viewModel::moveUp,
        onMoveDown = viewModel::moveDown,
        onReset = viewModel::reset,
        onHowTo = onHowTo,
        modifier = modifier,
    )
}

/**
 * One routine and its exercises, each with its muscles and equipment from the wger catalog. Tap
 * an exercise to see how to do it (photos and steps); the ⋮ menu swaps, moves or removes it.
 * Stateless.
 */
@Composable
fun RoutineScreen(
    state: RoutineUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onAdd: () -> Unit,
    onSwap: (index: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    onMoveUp: (index: Int) -> Unit,
    onMoveDown: (index: Int) -> Unit,
    onReset: () -> Unit,
    onHowTo: (exerciseName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_routine"),
    ) {
        Row(
            modifier = Modifier
                .readableWidth()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("routine_back")) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.plan_back))
            }
            if (state.name.isNotEmpty()) {
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = state.name.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .semantics { heading() }
                            .testTag("routine_name"),
                    )
                    val count = stringResource(R.string.workouts_exercise_count, state.exercises.size)
                    Text(
                        text = if (state.isEdited) stringResource(R.string.plan_routine_summary, count, stringResource(R.string.plan_edited)) else count,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.notFound -> Text(
                text = stringResource(R.string.routine_not_found),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .readableWidth()
                    .padding(20.dp)
                    .testTag("routine_not_found"),
            )
            else -> RoutineContent(state, onStart, onAdd, onSwap, onRemove, onMoveUp, onMoveDown, onReset, onHowTo)
        }
    }
}

@Composable
private fun RoutineContent(
    state: RoutineUiState,
    onStart: () -> Unit,
    onAdd: () -> Unit,
    onSwap: (index: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    onMoveUp: (index: Int) -> Unit,
    onMoveDown: (index: Int) -> Unit,
    onReset: () -> Unit,
    onHowTo: (exerciseName: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .readableWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onStart,
            enabled = state.canStart,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("routine_start"),
        ) {
            Icon(painterResource(R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.routine_start), style = MaterialTheme.typography.titleMedium)
        }
        if (state.saveFailed) {
            Text(
                text = stringResource(R.string.plan_save_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("routine_save_failed"),
            )
        }
        Overline(
            stringResource(R.string.routine_exercises),
            modifier = Modifier
                .padding(top = 8.dp)
                .semantics { heading() },
        )
        if (state.catalogEmpty) {
            Text(
                text = stringResource(R.string.routine_catalog_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("routine_catalog_empty"),
            )
        }
        if (state.exercises.isEmpty()) {
            Text(
                text = stringResource(R.string.routine_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("routine_empty"),
            )
        }
        state.exercises.forEachIndexed { index, exercise ->
            ExerciseRow(
                number = index + 1,
                exercise = exercise,
                canMoveUp = index > 0,
                canMoveDown = index < state.exercises.lastIndex,
                canRemove = state.canRemove,
                onOpen = { onHowTo(exercise.name) },
                onSwap = { onSwap(index) },
                onMoveUp = { onMoveUp(index) },
                onMoveDown = { onMoveDown(index) },
                onRemove = { onRemove(index) },
            )
        }
        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("routine_add"),
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.routine_add))
        }
        if (state.isEdited) {
            TextButton(onClick = onReset, modifier = Modifier.testTag("routine_reset")) {
                Text(stringResource(R.string.routine_reset))
            }
        }
        if (state.exercises.any { it.match != null }) {
            Text(
                text = stringResource(R.string.routine_credit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One exercise: its number, name, muscles and equipment, and a ⋮ menu. */
@Composable
private fun ExerciseRow(
    number: Int,
    exercise: RoutineExerciseUi,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    onOpen: () -> Unit,
    onSwap: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    VoltCard(
        contentPadding = 12.dp,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = stringResource(R.string.routine_open_how_to), role = Role.Button, onClick = onOpen)
            .testTag("routine_exercise_$number"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = number.toString(), style = statStyle(22.sp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = exercise.match?.let { matchDetail(it) } ?: stringResource(R.string.routine_not_in_catalog),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExerciseMenu(
                name = exercise.name,
                number = number,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                canRemove = canRemove,
                onSwap = onSwap,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onRemove = onRemove,
            )
        }
    }
}

/** "Chest, Triceps · Barbell", or "Bodyweight" when it needs no equipment. */
@Composable
private fun matchDetail(exercise: Exercise): String {
    val equipment = exercise.equipment.joinToString(", ").ifEmpty { stringResource(R.string.exercises_bodyweight) }
    val muscles = exercise.muscles.take(2).joinToString(", ").ifEmpty { exercise.category }
    return listOf(muscles, equipment).filter { it.isNotBlank() }.joinToString(" · ")
}

@Composable
private fun ExerciseMenu(
    name: String,
    number: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    onSwap: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.testTag("routine_menu_$number")) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.routine_more, name))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.routine_swap)) },
                onClick = { open = false; onSwap() },
                modifier = Modifier.testTag("routine_swap_$number"),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.routine_move_up)) },
                onClick = { open = false; onMoveUp() },
                enabled = canMoveUp,
                modifier = Modifier.testTag("routine_up_$number"),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.routine_move_down)) },
                onClick = { open = false; onMoveDown() },
                enabled = canMoveDown,
                modifier = Modifier.testTag("routine_down_$number"),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.routine_remove)) },
                onClick = { open = false; onRemove() },
                enabled = canRemove,
                modifier = Modifier.testTag("routine_remove_$number"),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun RoutineScreenPreview() {
    val bench = Exercise(73, "Bench Press", "", "Chest", listOf("Chest", "Triceps"), listOf("Barbell"), null)
    RepTrackTheme {
        RoutineScreen(
            state = RoutineUiState(
                isLoading = false,
                routineId = "upper",
                name = "Upper body",
                exercises = listOf(RoutineExerciseUi("Bench press", bench), RoutineExerciseUi("Barbell row", null)),
                isEdited = true,
            ),
            onBack = {},
            onStart = {},
            onAdd = {},
            onSwap = {},
            onRemove = {},
            onMoveUp = {},
            onMoveDown = {},
            onReset = {},
            onHowTo = {},
        )
    }
}

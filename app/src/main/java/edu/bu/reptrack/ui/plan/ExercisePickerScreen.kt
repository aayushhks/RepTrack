package edu.bu.reptrack.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.ui.components.IconTile
import edu.bu.reptrack.ui.components.Overline
import edu.bu.reptrack.ui.components.VoltCard
import edu.bu.reptrack.ui.components.readableWidth

/** Connects the exercise picker to its ViewModel, and closes it once the change is saved. */
@Composable
fun ExercisePickerRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExercisePickerViewModel = viewModel(factory = ExercisePickerViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) {
        if (state.done) onBack()
    }
    ExercisePickerScreen(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onPick = viewModel::pick,
        modifier = modifier,
    )
}

/**
 * Picks an exercise from the wger catalog: to add to the routine, or to swap one out. When
 * swapping, exercises for the same muscles come first. A typed name the catalog does not have
 * can be added as the user's own. Stateless.
 */
@Composable
fun ExercisePickerScreen(
    state: ExercisePickerUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onPick: (name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .readableWidth()
            .testTag("screen_exercise_picker"),
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("picker_back")) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.plan_back))
            }
            val replacing = state.replacing
            Text(
                text = if (replacing != null) {
                    stringResource(R.string.picker_swap_title, replacing)
                } else {
                    stringResource(R.string.picker_add_title, state.routineName)
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .semantics { heading() }
                    .testTag("picker_title"),
            )
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.exercises_search)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("picker_search"),
        )
        if (state.saveFailed) {
            Text(
                text = stringResource(R.string.plan_save_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("picker_save_failed"),
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.customName?.let { name ->
                item(key = "custom") {
                    PickRow(
                        title = stringResource(R.string.picker_custom, name),
                        subtitle = null,
                        onClick = { onPick(name) },
                        tag = "picker_custom",
                    )
                }
            }
            if (state.catalogEmpty) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.picker_catalog_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("picker_catalog_empty"),
                    )
                }
            }
            if (state.sameMuscles.isNotEmpty()) {
                item(key = "same_header") { Header(R.string.picker_same_muscles) }
                items(state.sameMuscles, key = { "same_${it.id}" }) { exercise ->
                    ExercisePickRow(exercise = exercise, onPick = onPick)
                }
            }
            if (state.results.isNotEmpty()) {
                item(key = "all_header") { Header(R.string.picker_all) }
                items(state.results, key = { "all_${it.id}" }) { exercise ->
                    ExercisePickRow(exercise = exercise, onPick = onPick)
                }
            }
        }
    }
}

@Composable
private fun Header(text: Int) {
    Overline(
        text = stringResource(text),
        modifier = Modifier
            .padding(top = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun ExercisePickRow(exercise: Exercise, onPick: (String) -> Unit) {
    val equipment = exercise.equipment.joinToString(", ").ifEmpty { stringResource(R.string.exercises_bodyweight) }
    val muscles = exercise.muscles.take(2).joinToString(", ").ifEmpty { exercise.category }
    PickRow(
        title = exercise.name,
        subtitle = listOf(muscles, equipment).filter { it.isNotBlank() }.joinToString(" · "),
        onClick = { onPick(exercise.name) },
        tag = "picker_exercise_${exercise.id}",
    )
}

@Composable
private fun PickRow(title: String, subtitle: String?, onClick: () -> Unit, tag: String) {
    VoltCard(
        contentPadding = 12.dp,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .testTag(tag),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(
                icon = R.drawable.ic_add,
                tint = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

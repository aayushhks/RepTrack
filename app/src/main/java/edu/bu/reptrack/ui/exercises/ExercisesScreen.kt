package edu.bu.reptrack.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.ui.components.ErrorCard
import edu.bu.reptrack.ui.components.IconTile
import edu.bu.reptrack.ui.components.VoltCard
import edu.bu.reptrack.ui.components.readableWidth

/** Connects the Exercises list to its ViewModel. */
@Composable
fun ExercisesRoute(
    onOpenExercise: (exerciseId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisesScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onCategoryClick = viewModel::onCategoryClick,
        onClearSearch = viewModel::clearSearch,
        onRetry = viewModel::retry,
        onOpenExercise = onOpenExercise,
        modifier = modifier,
    )
}

/** Exercises tab: search and filter the wger catalog, tap one to open it. Stateless. */
@Composable
fun ExercisesScreen(
    state: ExercisesUiState,
    onQueryChange: (String) -> Unit,
    onCategoryClick: (String?) -> Unit,
    onClearSearch: () -> Unit,
    onRetry: () -> Unit,
    onOpenExercise: (exerciseId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .readableWidth()
            .padding(horizontal = 20.dp)
            .testTag("screen_exercises"),
    ) {
        Text(
            text = stringResource(R.string.tab_exercises),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 24.dp, bottom = 12.dp)
                .semantics { heading() },
        )
        when {
            state.isLoading -> {
                val loading = stringResource(R.string.exercises_loading)
                Box(Modifier.fillMaxSize().testTag("exercises_loading"), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.semantics { contentDescription = loading })
                }
            }

            !state.hasSaved -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.refreshFailed) {
                    ErrorCard(
                        message = stringResource(R.string.exercises_load_failed),
                        actionLabel = stringResource(R.string.error_try_again),
                        onAction = onRetry,
                        modifier = Modifier.testTag("exercises_error"),
                    )
                } else {
                    Column(
                        modifier = Modifier.testTag("exercises_empty"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.exercises_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = onRetry) { Text(stringResource(R.string.error_try_again)) }
                    }
                }
            }

            else -> ExerciseList(state, onQueryChange, onCategoryClick, onClearSearch, onRetry, onOpenExercise)
        }
    }
}

@Composable
private fun ColumnScope.ExerciseList(
    state: ExercisesUiState,
    onQueryChange: (String) -> Unit,
    onCategoryClick: (String?) -> Unit,
    onClearSearch: () -> Unit,
    onRetry: () -> Unit,
    onOpenExercise: (Int) -> Unit,
) {
    OutlinedTextField(
        value = state.query,
        onValueChange = onQueryChange,
        label = { Text(stringResource(R.string.exercises_search)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("exercises_search"),
    )
    LazyRow(
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = state.category == null,
                onClick = { onCategoryClick(null) },
                label = { Text(stringResource(R.string.exercises_all)) },
            )
        }
        items(state.categories) { name ->
            FilterChip(
                selected = state.category == name,
                onClick = { onCategoryClick(name) },
                label = { Text(name) },
            )
        }
    }
    if (state.refreshFailed) {
        // Same list, saved on the phone: say so, and offer another try.
        ErrorCard(
            message = stringResource(R.string.exercises_offline),
            actionLabel = stringResource(R.string.error_try_again),
            onAction = onRetry,
            modifier = Modifier.padding(bottom = 8.dp).testTag("exercises_offline"),
        )
    }
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.exercises.isEmpty()) {
            item(key = "no_matches") {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp).testTag("exercises_no_matches"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.exercises_no_matches, state.query.trim()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onClearSearch) {
                        Text(stringResource(R.string.exercises_clear_search))
                    }
                }
            }
        }
        items(state.exercises, key = { it.id }) { exercise ->
            ExerciseRow(exercise, onClick = { onOpenExercise(exercise.id) })
        }
        item(key = "credit") {
            Text(
                text = stringResource(R.string.exercises_credit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** One exercise in the list: name, main muscles and equipment. */
@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val muscles = exercise.muscles.joinToString(", ").ifEmpty { exercise.category }
    val equipment = exercise.equipment.joinToString(", ").ifEmpty { stringResource(R.string.exercises_bodyweight) }
    VoltCard(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("exercise_${exercise.id}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(
                icon = R.drawable.ic_tab_exercises,
                tint = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (muscles.isNotBlank()) {
                    Text(
                        muscles,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    equipment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

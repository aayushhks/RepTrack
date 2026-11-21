package edu.bu.reptrack.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.ui.components.ErrorCard
import edu.bu.reptrack.ui.components.readableWidth

/** Connects the Exercises tab to its ViewModel. */
@Composable
fun ExercisesRoute(
    modifier: Modifier = Modifier,
    viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisesScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onCategoryClick = viewModel::onCategoryClick,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

/** Exercises tab: search and filter the wger catalog. Stateless. */
@Composable
fun ExercisesScreen(
    state: ExercisesUiState,
    onQueryChange: (String) -> Unit,
    onCategoryClick: (String?) -> Unit,
    onRetry: () -> Unit,
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

            else -> ExerciseList(state, onQueryChange, onCategoryClick, onRetry)
        }
    }
}

@Composable
private fun ColumnScope.ExerciseList(
    state: ExercisesUiState,
    onQueryChange: (String) -> Unit,
    onCategoryClick: (String?) -> Unit,
    onRetry: () -> Unit,
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
            item {
                Text(
                    text = stringResource(R.string.exercises_no_matches),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp).testTag("exercises_no_matches"),
                )
            }
        }
        items(state.exercises, key = { it.id }) { exercise -> ExerciseCard(exercise) }
        item {
            Text(
                text = stringResource(R.string.exercises_credit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** One exercise. Tapping it shows or hides the muscles, equipment and how to do it. */
@Composable
private fun ExerciseCard(exercise: Exercise, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable(exercise.id) { mutableStateOf(false) }
    Card(
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth().testTag("exercise_${exercise.id}"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                exercise.imageUrl?.let { url ->
                    // The name next to it says everything, so TalkBack skips the picture.
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.small),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (exercise.category.isNotBlank()) {
                        Text(
                            exercise.category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (expanded) {
                if (exercise.muscles.isNotEmpty()) {
                    Text(stringResource(R.string.exercises_muscles, exercise.muscles.joinToString(", ")))
                }
                if (exercise.equipment.isNotEmpty()) {
                    Text(stringResource(R.string.exercises_equipment, exercise.equipment.joinToString(", ")))
                }
                if (exercise.description.isNotBlank()) {
                    Text(exercise.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

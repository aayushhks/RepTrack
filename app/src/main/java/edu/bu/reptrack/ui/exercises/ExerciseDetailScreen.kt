package edu.bu.reptrack.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import edu.bu.reptrack.ui.components.Overline
import edu.bu.reptrack.ui.components.VoltCard
import edu.bu.reptrack.ui.components.readableWidth

/** Connects the exercise detail to its ViewModel. */
@Composable
fun ExerciseDetailRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseDetailViewModel = viewModel(factory = ExerciseDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseDetailScreen(state = state, onBack = onBack, modifier = modifier)
}

/** One exercise: picture when wger has one, category, muscles, equipment and how to do it. Stateless. */
@Composable
fun ExerciseDetailScreen(
    state: ExerciseDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .readableWidth()
            .padding(horizontal = 12.dp)
            .testTag("screen_exercise_detail"),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp).testTag("exercise_detail_back")) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.exercises_back),
            )
        }
        val exercise = state.exercise
        when {
            state.isLoading -> {
                val loading = stringResource(R.string.exercises_loading)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.semantics { contentDescription = loading })
                }
            }

            exercise == null -> Text(
                text = stringResource(R.string.exercises_not_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp).testTag("exercise_not_found"),
            )

            else -> ExerciseDetailContent(exercise)
        }
    }
}

@Composable
private fun ExerciseDetailContent(exercise: Exercise) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        exercise.imageUrl?.let { url ->
            // The name above says it, so TalkBack skips the picture.
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium),
            )
        }
        VoltCard {
            Fact(stringResource(R.string.exercises_category_label), exercise.category)
            Fact(stringResource(R.string.exercises_muscles_label), exercise.muscles.joinToString(", "))
            Fact(
                stringResource(R.string.exercises_equipment_label),
                exercise.equipment.joinToString(", ").ifEmpty { stringResource(R.string.exercises_bodyweight) },
            )
        }
        if (exercise.description.isNotBlank()) {
            VoltCard {
                Overline(stringResource(R.string.exercises_description_label))
                Text(exercise.description, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Text(
            text = stringResource(R.string.exercises_credit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A small label over its value. Skipped when there is no value. */
@Composable
private fun Fact(label: String, value: String) {
    if (value.isBlank()) return
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Overline(label)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

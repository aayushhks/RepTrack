package edu.bu.reptrack.ui.howto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.exercise.ExerciseDemo
import edu.bu.reptrack.ui.components.ErrorCard
import edu.bu.reptrack.ui.components.Overline
import edu.bu.reptrack.ui.components.VoltCard
import edu.bu.reptrack.ui.components.readableWidth
import edu.bu.reptrack.ui.theme.RepTrackTheme
import edu.bu.reptrack.ui.theme.statStyle
import java.util.Locale

/** Connects the how-to screen to its ViewModel. */
@Composable
fun HowToRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HowToViewModel = viewModel(factory = HowToViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HowToScreen(state = state, onBack = onBack, onRetry = viewModel::retry, modifier = modifier)
}

/**
 * How to do one exercise: the two photos moving like a GIF, the steps, and its muscles and
 * equipment. Opened from a routine or in the middle of a workout. Stateless.
 */
@Composable
fun HowToScreen(
    state: HowToUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_how_to"),
    ) {
        Row(
            modifier = Modifier
                .readableWidth()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("how_to_back")) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.plan_back))
            }
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = state.name.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .semantics { heading() }
                        .testTag("how_to_name"),
                )
                details(state)?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.howto_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("how_to_loading"),
                )
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .readableWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val demo = state.demo
            when {
                demo != null -> {
                    DemoPhotos(demo = demo, exerciseName = state.name)
                    Steps(demo.steps)
                }
                state.loadFailed -> ErrorCard(
                    message = stringResource(R.string.howto_failed),
                    actionLabel = stringResource(R.string.error_try_again),
                    onAction = onRetry,
                    modifier = Modifier.testTag("how_to_error"),
                )
                else -> {
                    Text(
                        text = stringResource(R.string.howto_no_photos),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("how_to_no_photos"),
                    )
                    // The catalog's own description, if it has one.
                    state.catalog?.description?.takeIf { it.isNotBlank() }?.let { text ->
                        VoltCard {
                            Overline(stringResource(R.string.howto_steps))
                            Text(text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            if (demo != null) {
                Text(
                    text = stringResource(R.string.howto_credit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.catalog != null) {
                Text(
                    text = stringResource(R.string.routine_credit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Chest, Triceps · Barbell", from the catalog, or from the photo set when the catalog does not have it. */
@Composable
private fun details(state: HowToUiState): String? {
    val catalog = state.catalog
    val muscles: List<String>
    val equipment: String?
    if (catalog != null) {
        muscles = catalog.muscles
        equipment = catalog.equipment.joinToString(", ").ifEmpty { stringResource(R.string.exercises_bodyweight) }
    } else {
        val demo = state.demo ?: return null
        muscles = demo.primaryMuscles.map { name -> name.replaceFirstChar { it.titlecase(Locale.getDefault()) } }
        equipment = demo.equipment?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }
    return listOf(muscles.take(2).joinToString(", "), equipment.orEmpty()).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { null }
}

/** The steps, numbered, each read by TalkBack as one item. */
@Composable
fun Steps(steps: List<String>, modifier: Modifier = Modifier) {
    if (steps.isEmpty()) return
    Column(modifier = modifier.testTag("how_to_steps"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Overline(stringResource(R.string.howto_steps), modifier = Modifier.semantics { heading() })
        steps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = (index + 1).toString(), style = statStyle(16.sp))
                    }
                }
                Text(text = step, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun HowToScreenPreview() {
    RepTrackTheme {
        HowToScreen(
            state = HowToUiState(
                name = "Bench press",
                isLoading = false,
                demo = ExerciseDemo(
                    id = "Barbell_Bench_Press_-_Medium_Grip",
                    name = "Barbell Bench Press - Medium Grip",
                    imageUrls = emptyList(),
                    steps = listOf("Lie back on a flat bench.", "Lower the bar to your chest.", "Push it back up."),
                    primaryMuscles = listOf("chest"),
                    equipment = "barbell",
                ),
            ),
            onBack = {},
            onRetry = {},
        )
    }
}

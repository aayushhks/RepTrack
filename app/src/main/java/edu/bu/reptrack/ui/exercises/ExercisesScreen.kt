package edu.bu.reptrack.ui.exercises

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import edu.bu.reptrack.R
import edu.bu.reptrack.ui.components.PlaceholderScreen

/** Exercises tab: the exercise catalog. Placeholder for now. */
@Composable
fun ExercisesScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.tab_exercises),
        message = stringResource(R.string.exercises_placeholder),
        tag = "screen_exercises",
        modifier = modifier,
    )
}

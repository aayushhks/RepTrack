package edu.bu.reptrack.ui.progress

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import edu.bu.reptrack.R
import edu.bu.reptrack.ui.components.PlaceholderScreen

/** Progress tab: weight trend, workouts and runs. Placeholder for now. */
@Composable
fun ProgressScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.tab_progress),
        message = stringResource(R.string.progress_placeholder),
        tag = "screen_progress",
        modifier = modifier,
    )
}

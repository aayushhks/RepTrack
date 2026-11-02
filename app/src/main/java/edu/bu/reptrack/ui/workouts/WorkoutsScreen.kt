package edu.bu.reptrack.ui.workouts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import edu.bu.reptrack.R
import edu.bu.reptrack.ui.components.PlaceholderScreen

/** Workouts tab: gym and home routines. Placeholder for now. */
@Composable
fun WorkoutsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.tab_workouts),
        message = stringResource(R.string.workouts_placeholder),
        tag = "screen_workouts",
        modifier = modifier,
    )
}

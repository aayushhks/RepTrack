package edu.bu.reptrack.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.ui.components.PlaceholderScreen

/** Workout session for one routine. Set logging is the next feature, so this is a placeholder for now. */
@Composable
fun WorkoutSessionScreen(routineId: String, modifier: Modifier = Modifier) {
    val routine = StarterRoutines.find(routineId)
    PlaceholderScreen(
        title = routine?.name ?: stringResource(R.string.session_fallback_title),
        message = stringResource(R.string.session_placeholder),
        tag = "screen_workout_session",
        modifier = modifier,
    )
}

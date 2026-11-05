package edu.bu.reptrack.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import edu.bu.reptrack.R

/**
 * The three tabs in the bottom navigation bar. [route] is where the tab goes.
 * For Workouts it is a nested graph that holds Workouts home and the workout session.
 */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    WORKOUTS("workouts", R.string.tab_workouts, R.drawable.ic_tab_workouts),
    EXERCISES("exercises", R.string.tab_exercises, R.drawable.ic_tab_exercises),
    PROGRESS("progress", R.string.tab_progress, R.drawable.ic_tab_progress),
    ;

    companion object {
        /** The tab the app opens on. */
        val START = WORKOUTS
    }
}

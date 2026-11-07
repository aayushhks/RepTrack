package edu.bu.reptrack.ui.navigation

/** Routes that are not bottom-bar tabs. */
object Routes {
    const val SIGN_IN = "sign_in"
    const val GOAL_SETUP = "goal_setup"

    // Screens inside the Workouts tab. The tab itself is a nested graph, so it keeps
    // its own back stack: Workouts home, then a workout session on top.
    const val WORKOUTS_HOME = "workouts_home"
    const val ARG_ROUTINE_ID = "routineId"
    const val WORKOUT_SESSION = "workout_session/{$ARG_ROUTINE_ID}"

    fun workoutSession(routineId: String) = "workout_session/$routineId"
}

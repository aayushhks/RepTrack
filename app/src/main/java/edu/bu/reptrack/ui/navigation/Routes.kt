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

    /** The walk or run screen, also inside the Workouts tab. */
    const val RUN = "run"

    // The week plan and routines, also inside the Workouts tab: the week, a routine with its
    // exercises, the catalog to add or swap one, and one exercise's catalog page.
    const val ARG_PLACE = "place"
    const val WEEK_PLAN = "week_plan/{$ARG_PLACE}"
    const val ROUTINE = "routine/{$ARG_ROUTINE_ID}"
    const val ARG_REPLACE_INDEX = "replace"
    const val ROUTINE_PICK = "routine_pick/{$ARG_ROUTINE_ID}?$ARG_REPLACE_INDEX={$ARG_REPLACE_INDEX}"

    fun weekPlan(place: String) = "week_plan/$place"

    fun routine(routineId: String) = "routine/$routineId"

    /** [replaceIndex] is the exercise to swap, or -1 to add a new one. */
    fun routinePick(routineId: String, replaceIndex: Int = -1) = "routine_pick/$routineId?$ARG_REPLACE_INDEX=$replaceIndex"

    // Screens inside the Progress tab, also a nested graph: the history, then a saved run on
    // its map, then the picture to share it as a story.
    const val PROGRESS_HOME = "progress_home"
    const val ARG_RUN_ID = "runId"
    const val RUN_DETAIL = "run_detail/{$ARG_RUN_ID}"
    const val RUN_STORY = "run_story/{$ARG_RUN_ID}"

    fun runDetail(runId: Long) = "run_detail/$runId"

    fun runStory(runId: Long) = "run_story/$runId"

    // Screens inside the Exercises tab, also a nested graph: the list, then one exercise on top.
    const val EXERCISES_HOME = "exercises_home"
    const val ARG_EXERCISE_ID = "exerciseId"
    const val EXERCISE_DETAIL = "exercise_detail/{$ARG_EXERCISE_ID}"

    fun exerciseDetail(exerciseId: Int) = "exercise_detail/$exerciseId"

    /** The same exercise page, opened from a routine, so it stays inside the Workouts tab. */
    const val PLAN_EXERCISE = "plan_exercise/{$ARG_EXERCISE_ID}"

    fun planExercise(exerciseId: Int) = "plan_exercise/$exerciseId"
}

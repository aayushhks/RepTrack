package edu.bu.reptrack.ui.components

import androidx.annotation.StringRes
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.PlanType
import edu.bu.reptrack.domain.profile.TrainingPlace

// Display names for our enums. Shared by goal setup and the Workouts tab.

@StringRes
fun FitnessGoal.labelRes(): Int = when (this) {
    FitnessGoal.LOSE_WEIGHT -> R.string.goal_lose_weight
    FitnessGoal.BUILD_MUSCLE -> R.string.goal_build_muscle
    FitnessGoal.STAY_FIT -> R.string.goal_stay_fit
}

@StringRes
fun TrainingPlace.labelRes(): Int = when (this) {
    TrainingPlace.GYM -> R.string.place_gym
    TrainingPlace.HOME -> R.string.place_home
}

@StringRes
fun PlanType.labelRes(): Int = when (this) {
    PlanType.FULL_BODY -> R.string.plan_full_body
    PlanType.UPPER_LOWER -> R.string.plan_upper_lower
    PlanType.BODYWEIGHT_FULL_BODY -> R.string.plan_bodyweight_full_body
    PlanType.BODYWEIGHT_STRENGTH -> R.string.plan_bodyweight_strength
}

package edu.bu.reptrack.domain.profile

/** Which starter routine to suggest. The routines themselves come with the workouts feature. */
enum class PlanType { FULL_BODY, UPPER_LOWER, BODYWEIGHT_FULL_BODY, BODYWEIGHT_STRENGTH }

/** A suggested plan: what kind of routine and how many days a week. */
data class StarterPlan(val type: PlanType, val daysPerWeek: Int) {
    companion object {
        /** Simple rules for a first plan based on the goal and where the user trains. */
        fun suggest(goal: FitnessGoal, place: TrainingPlace): StarterPlan = when (place) {
            TrainingPlace.GYM -> when (goal) {
                FitnessGoal.LOSE_WEIGHT -> StarterPlan(PlanType.FULL_BODY, 3)
                FitnessGoal.BUILD_MUSCLE -> StarterPlan(PlanType.UPPER_LOWER, 4)
                FitnessGoal.STAY_FIT -> StarterPlan(PlanType.FULL_BODY, 2)
            }
            TrainingPlace.HOME -> when (goal) {
                FitnessGoal.LOSE_WEIGHT -> StarterPlan(PlanType.BODYWEIGHT_FULL_BODY, 3)
                FitnessGoal.BUILD_MUSCLE -> StarterPlan(PlanType.BODYWEIGHT_STRENGTH, 3)
                FitnessGoal.STAY_FIT -> StarterPlan(PlanType.BODYWEIGHT_FULL_BODY, 2)
            }
        }
    }
}

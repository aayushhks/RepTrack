package edu.bu.reptrack.domain.profile

/** What the user wants from training. Drives the starter plan. */
enum class FitnessGoal { LOSE_WEIGHT, BUILD_MUSCLE, STAY_FIT }

/** Where the user usually trains. */
enum class TrainingPlace { GYM, HOME }

/** Body stats and goal from the setup screen. Metric units for now. */
data class UserProfile(
    val heightCm: Float,
    val weightKg: Float,
    val goalWeightKg: Float,
    val goal: FitnessGoal,
    val place: TrainingPlace,
)

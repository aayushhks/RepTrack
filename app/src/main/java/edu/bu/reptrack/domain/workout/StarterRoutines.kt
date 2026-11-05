package edu.bu.reptrack.domain.workout

import edu.bu.reptrack.domain.profile.PlanType

/** A ready-made workout: a name and its exercises in order. */
data class WorkoutTemplate(
    val id: String,
    val name: String,
    val exercises: List<String>,
)

/**
 * Built-in routines for each starter plan. For now the exercises are plain names.
 * Later they will link to exercises in the catalog.
 */
object StarterRoutines {

    private val routinesByPlan: Map<PlanType, List<WorkoutTemplate>> = mapOf(
        PlanType.FULL_BODY to listOf(
            WorkoutTemplate(
                id = "full_body_a",
                name = "Full body A",
                exercises = listOf("Squat", "Bench press", "Barbell row", "Plank"),
            ),
            WorkoutTemplate(
                id = "full_body_b",
                name = "Full body B",
                exercises = listOf("Deadlift", "Overhead press", "Lat pulldown", "Walking lunge"),
            ),
        ),
        PlanType.UPPER_LOWER to listOf(
            WorkoutTemplate(
                id = "upper",
                name = "Upper body",
                exercises = listOf("Bench press", "Barbell row", "Overhead press", "Lat pulldown", "Biceps curl"),
            ),
            WorkoutTemplate(
                id = "lower",
                name = "Lower body",
                exercises = listOf("Squat", "Romanian deadlift", "Leg press", "Calf raise"),
            ),
        ),
        PlanType.BODYWEIGHT_FULL_BODY to listOf(
            WorkoutTemplate(
                id = "bodyweight_a",
                name = "Bodyweight circuit A",
                exercises = listOf("Push-up", "Bodyweight squat", "Glute bridge", "Plank"),
            ),
            WorkoutTemplate(
                id = "bodyweight_b",
                name = "Bodyweight circuit B",
                exercises = listOf("Reverse lunge", "Pike push-up", "Superman", "Mountain climber"),
            ),
        ),
        PlanType.BODYWEIGHT_STRENGTH to listOf(
            WorkoutTemplate(
                id = "home_push",
                name = "Push and legs",
                exercises = listOf("Push-up", "Pike push-up", "Chair dip", "Split squat"),
            ),
            WorkoutTemplate(
                id = "home_pull",
                name = "Pull and core",
                exercises = listOf("Table row", "Single-leg glute bridge", "Hollow hold", "Side plank"),
            ),
        ),
    )

    /** The routines that belong to a plan, in the order to do them. */
    fun forPlan(type: PlanType): List<WorkoutTemplate> = routinesByPlan.getValue(type)

    /** Looks up a routine by its id, or returns null if there is none. */
    fun find(id: String): WorkoutTemplate? =
        routinesByPlan.values.flatten().firstOrNull { it.id == id }
}

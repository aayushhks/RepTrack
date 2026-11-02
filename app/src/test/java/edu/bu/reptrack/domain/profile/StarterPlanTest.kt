package edu.bu.reptrack.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StarterPlanTest {

    @Test
    fun everyGoalAndPlace_getsAPlan_withTwoToFourDays() {
        for (goal in FitnessGoal.entries) {
            for (place in TrainingPlace.entries) {
                val plan = StarterPlan.suggest(goal, place)
                assertTrue("$goal at $place", plan.daysPerWeek in 2..4)
            }
        }
    }

    @Test
    fun buildMuscleAtGym_isUpperLowerFourDays() {
        assertEquals(
            StarterPlan(PlanType.UPPER_LOWER, 4),
            StarterPlan.suggest(FitnessGoal.BUILD_MUSCLE, TrainingPlace.GYM),
        )
    }

    @Test
    fun homePlans_areAlwaysBodyweight() {
        val bodyweight = setOf(PlanType.BODYWEIGHT_FULL_BODY, PlanType.BODYWEIGHT_STRENGTH)
        for (goal in FitnessGoal.entries) {
            val plan = StarterPlan.suggest(goal, TrainingPlace.HOME)
            assertTrue("$goal at home", plan.type in bodyweight)
        }
    }
}

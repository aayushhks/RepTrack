package edu.bu.reptrack.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightTest {

    @Test
    fun formatKg_dropsTrailingZero_andKeepsOneDecimal() {
        assertEquals("82", formatKg(82f))
        assertEquals("82.5", formatKg(82.5f))
        assertEquals("82.5", formatKg(82.46f))
        assertEquals("100", formatKg(100f))
    }

    @Test
    fun heavierThanGoal_isWeightToLose() {
        assertEquals(WeightGoalGap.ToLose(6f), weightGoalGap(currentKg = 82f, goalKg = 76f))
    }

    @Test
    fun lighterThanGoal_isWeightToGain() {
        assertEquals(WeightGoalGap.ToGain(4f), weightGoalGap(currentKg = 66f, goalKg = 70f))
    }

    @Test
    fun withinATenthOfAKilo_isAtGoal() {
        assertEquals(WeightGoalGap.AtGoal, weightGoalGap(currentKg = 70.05f, goalKg = 70f))
    }
}

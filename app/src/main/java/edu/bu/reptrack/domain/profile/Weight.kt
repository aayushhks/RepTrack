package edu.bu.reptrack.domain.profile

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/** How far the user is from their goal weight. */
sealed interface WeightGoalGap {
    data class ToLose(val kg: Float) : WeightGoalGap
    data class ToGain(val kg: Float) : WeightGoalGap
    data object AtGoal : WeightGoalGap
}

/** Compares the current weight with the goal. Anything under 0.1 kg counts as at the goal. */
fun weightGoalGap(currentKg: Float, goalKg: Float): WeightGoalGap {
    val difference = currentKg - goalKg
    return when {
        abs(difference) < 0.1f -> WeightGoalGap.AtGoal
        difference > 0 -> WeightGoalGap.ToLose(difference)
        else -> WeightGoalGap.ToGain(-difference)
    }
}

/** Shows a weight with at most one decimal and no trailing zero: 82.0 -> "82", 82.46 -> "82.5". */
fun formatKg(kg: Float): String =
    BigDecimal(kg.toString())
        .setScale(1, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()

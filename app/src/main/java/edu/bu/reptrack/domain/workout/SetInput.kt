package edu.bu.reptrack.domain.workout

import edu.bu.reptrack.domain.profile.ProfileValidator

/** A set the user typed in, after it passed the checks. */
data class ParsedSet(val weightKg: Float, val reps: Int)

/**
 * Checks for the weight and reps boxes on the workout screen. Pure Kotlin so it is easy to test.
 * An empty weight means a bodyweight exercise, so it is saved as 0 kg.
 */
object SetInput {
    val WEIGHT_KG_RANGE = 0f..500f
    val REPS_RANGE = 1..100

    /** Digits and one decimal point, the same rule as the goal setup screen. */
    fun cleanWeight(text: String): String = ProfileValidator.cleanNumber(text)

    /** Whole numbers only, at most 3 digits. */
    fun cleanReps(text: String): String = text.filter { it.isDigit() }.take(3)

    /** True when the weight box has something in it that is not a valid weight. Empty is fine. */
    fun isWeightBad(text: String): Boolean {
        if (text.isBlank()) return false
        val weight = text.toFloatOrNull() ?: return true
        return weight !in WEIGHT_KG_RANGE
    }

    /** True when reps are missing or outside 1 to 100. */
    fun isRepsBad(text: String): Boolean {
        val reps = text.toIntOrNull() ?: return true
        return reps !in REPS_RANGE
    }

    /** Both boxes as numbers, or null if either one is wrong. */
    fun parse(weightText: String, repsText: String): ParsedSet? {
        if (isWeightBad(weightText) || isRepsBad(repsText)) return null
        val weight = if (weightText.isBlank()) 0f else weightText.toFloat()
        return ParsedSet(weightKg = weight, reps = repsText.toInt())
    }
}

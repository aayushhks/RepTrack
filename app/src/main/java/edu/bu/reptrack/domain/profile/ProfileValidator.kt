package edu.bu.reptrack.domain.profile

/** Why a number field is not valid yet. */
enum class FieldError { EMPTY, NOT_A_NUMBER, OUT_OF_RANGE }

/** Checks the setup form. Pure Kotlin so it is easy to unit test. */
object ProfileValidator {
    val HEIGHT_CM_RANGE = 100f..250f
    val WEIGHT_KG_RANGE = 30f..300f

    /** Returns null when [text] is a number inside [range], otherwise the reason it is not. */
    fun check(text: String, range: ClosedFloatingPointRange<Float>): FieldError? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return FieldError.EMPTY
        val value = trimmed.toFloatOrNull() ?: return FieldError.NOT_A_NUMBER
        return if (value in range) null else FieldError.OUT_OF_RANGE
    }

    /** Keeps only digits and one decimal point. A comma counts as a decimal point. */
    fun cleanNumber(input: String): String {
        val out = StringBuilder()
        var seenDot = false
        for (ch in input.replace(',', '.')) {
            when {
                ch.isDigit() -> out.append(ch)
                ch == '.' && !seenDot -> { out.append(ch); seenDot = true }
            }
        }
        return out.toString().take(6)
    }

    /** Builds a profile when every field is valid, otherwise returns null. */
    fun toProfile(
        height: String,
        weight: String,
        goalWeight: String,
        goal: FitnessGoal,
        place: TrainingPlace,
    ): UserProfile? {
        if (check(height, HEIGHT_CM_RANGE) != null) return null
        if (check(weight, WEIGHT_KG_RANGE) != null) return null
        if (check(goalWeight, WEIGHT_KG_RANGE) != null) return null
        return UserProfile(
            heightCm = height.trim().toFloat(),
            weightKg = weight.trim().toFloat(),
            goalWeightKg = goalWeight.trim().toFloat(),
            goal = goal,
            place = place,
        )
    }
}

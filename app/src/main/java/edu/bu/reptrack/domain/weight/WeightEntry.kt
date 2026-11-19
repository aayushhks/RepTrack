package edu.bu.reptrack.domain.weight

/** One weigh-in: what the scale said, and when. */
data class WeightEntry(
    val id: Long = 0,
    val loggedAt: Long,
    val weightKg: Float,
)

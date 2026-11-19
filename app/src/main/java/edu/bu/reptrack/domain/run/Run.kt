package edu.bu.reptrack.domain.run

import java.util.Locale

/** A finished walk or run. Only the totals are kept; the route never leaves the phone. */
data class RunSummary(
    val id: Long = 0,
    val startedAt: Long,
    val finishedAt: Long,
    val distanceM: Double,
    val durationMs: Long,
    val calories: Int,
) {
    val paceSecondsPerKm: Long? get() = RunMath.paceSecondsPerKm(distanceM, durationMs)

    /** Slower than about 7.2 km/h on average counts as a walk. */
    val isWalk: Boolean
        get() = durationMs <= 0 || distanceM / (durationMs / 1000.0) < WALK_MAX_SPEED_MPS

    companion object {
        const val WALK_MAX_SPEED_MPS = 2.0
    }
}

/** How a run's numbers are shown. Plain Kotlin so it is easy to test. */
object RunFormat {
    /** 1234.5 m -> "1.23". Always two decimals, in kilometers. */
    fun km(distanceM: Double): String = String.format(Locale.US, "%.2f", distanceM / 1000.0)

    /** 307 000 ms -> "05:07"; 3 723 000 ms -> "1:02:03". */
    fun duration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /** 312 s per km -> "5:12". Null (not enough distance yet) -> "--:--". */
    fun pace(secondsPerKm: Long?): String {
        if (secondsPerKm == null || secondsPerKm >= MAX_SHOWN_PACE_S) return "--:--"
        return String.format(Locale.US, "%d:%02d", secondsPerKm / 60, secondsPerKm % 60)
    }

    /** Paces slower than 60 min/km are standing still, not a pace. */
    private const val MAX_SHOWN_PACE_S = 3600L
}

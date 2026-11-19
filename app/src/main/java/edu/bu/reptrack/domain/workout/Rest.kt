package edu.bu.reptrack.domain.workout

import java.util.Locale

/** The rest timer between sets. Plain Kotlin so it is easy to test. */
object Rest {
    /** A common rest for strength sets: 90 seconds. */
    const val DEFAULT_MS = 90_000L

    /** The − and + buttons change the rest by this much. */
    const val STEP_SECONDS = 15

    /** Milliseconds left, never below zero. */
    fun remainingMs(endsAt: Long, now: Long): Long = (endsAt - now).coerceAtLeast(0L)

    /** 83 000 ms -> "1:23". Rounds up, so the timer shows 0:00 only when the rest is really over. */
    fun format(remainingMs: Long): String {
        val seconds = (remainingMs.coerceAtLeast(0L) + 999) / 1000
        return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
    }

    /** How much of the rest is left, from 1 (just started) to 0 (over), for the progress bar. */
    fun fractionLeft(remainingMs: Long, totalMs: Long): Float =
        if (totalMs <= 0) 0f else (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
}

package edu.bu.reptrack.domain.run

/**
 * Running total for one run. Each new fix goes through [add], which keeps the good ones and
 * drops the ones that would add fake distance. Immutable: [add] returns a new value.
 */
data class RunProgress(
    val distanceM: Double = 0.0,
    /** The last fix that was kept. Null until the first good fix arrives. */
    val lastPoint: GeoPoint? = null,
) {
    /** True once at least one good fix has arrived. */
    val hasFix: Boolean get() = lastPoint != null

    fun add(point: GeoPoint): RunProgress {
        // A fuzzy fix (for example indoors or with approximate location) would add made-up meters.
        if (point.accuracyM > MAX_ACCURACY_M) return this
        val last = lastPoint ?: return copy(lastPoint = point)

        val seconds = (point.timeMillis - last.timeMillis) / 1000.0
        if (seconds <= 0) return this

        val meters = RunMath.distanceM(last, point)
        // Faster than a sprinter: the GPS jumped, not the runner.
        if (meters / seconds > MAX_SPEED_MPS) return this
        // Standing still, GPS drifts a few meters. Keep the old fix so the drift never adds up.
        if (meters < MIN_STEP_M) return this

        return RunProgress(distanceM = distanceM + meters, lastPoint = point)
    }

    companion object {
        const val MAX_ACCURACY_M = 25f
        const val MAX_SPEED_MPS = 12.5
        const val MIN_STEP_M = 3.0
    }
}

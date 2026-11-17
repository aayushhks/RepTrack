package edu.bu.reptrack.domain.run

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** One location fix from the phone: where, how precise, and when. Plain Kotlin, no Android types. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    /** Radius in meters the true position is likely inside. Smaller is better. */
    val accuracyM: Float,
    /** When the fix was taken, in ms since the phone booted. That clock never jumps, unlike wall time. */
    val timeMillis: Long,
)

/** The math behind a run: distance between fixes, pace and calories. Pure Kotlin so it is easy to test. */
object RunMath {
    private const val EARTH_RADIUS_M = 6_371_008.8
    private const val MPS_TO_MPH = 2.2369363

    /** Great-circle distance in meters (haversine formula). Close enough to the phone's own for a run. */
    fun distanceM(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /** Seconds per kilometer, or null until there is enough distance for a pace to mean anything. */
    fun paceSecondsPerKm(distanceM: Double, durationMs: Long): Long? {
        if (distanceM < MIN_PACE_DISTANCE_M || durationMs <= 0) return null
        return (durationMs / 1000.0 / (distanceM / 1000.0)).roundToInt().toLong()
    }

    /**
     * Calories from the activity's MET: kcal = MET × body weight in kg × hours.
     * Uses the average speed of the whole run.
     */
    fun caloriesKcal(weightKg: Float, distanceM: Double, durationMs: Long): Int {
        if (durationMs <= 0 || distanceM <= 0.0) return 0
        val hours = durationMs / 3_600_000.0
        val speedMps = distanceM / (durationMs / 1000.0)
        return (metFor(speedMps) * weightKg * hours).roundToInt()
    }

    /**
     * MET for walking or running at a speed, from the 2024 Adult Compendium of Physical Activities
     * (Herrmann et al.). Walking codes 17151 to 17230, running codes 12028 to 12135.
     * Each pair is (lowest speed in mph for that row, MET).
     */
    fun metFor(speedMps: Double): Double {
        val mph = speedMps * MPS_TO_MPH
        return MET_TABLE.last { (fromMph, _) -> mph >= fromMph }.second
    }

    /** Below this distance a pace jumps around too much to show. */
    const val MIN_PACE_DISTANCE_M = 50.0

    private val MET_TABLE: List<Pair<Double, Double>> = listOf(
        0.0 to 2.3, // walking, less than 2.0 mph
        2.0 to 2.8, // walking, 2.0 to 2.4 mph
        2.5 to 3.0, // walking, 2.5 mph
        2.8 to 3.8, // walking, 2.8 to 3.4 mph
        3.5 to 4.8, // walking, 3.5 to 3.9 mph
        4.0 to 5.5, // walking, 4.0 to 4.4 mph
        4.5 to 7.0, // walking, 4.5 to 4.9 mph
        5.0 to 8.5, // running, 5.0 to 5.2 mph
        5.5 to 9.0, // running, 5.5 to 5.8 mph
        6.0 to 9.3, // running, 6 to 6.3 mph
        6.7 to 10.5, // running, 6.7 mph
        7.0 to 11.0, // running, 7 mph
        7.5 to 11.8, // running, 7.5 mph
        8.0 to 12.0, // running, 8 mph
        8.6 to 12.5, // running, 8.6 mph
        9.0 to 13.0, // running, 9 mph
        10.0 to 14.8, // running, 10 mph
        11.0 to 16.8, // running, 11 mph
        12.0 to 18.5, // running, 12 mph
        13.0 to 19.8, // running, 13 mph
        14.0 to 23.0, // running, 14 mph
    )
}

package edu.bu.reptrack.domain.run

import java.time.Instant
import java.time.ZoneId

/** One point of a saved route. Only where: a saved route needs no accuracy or time. */
data class RoutePoint(val latitude: Double, val longitude: Double)

fun GeoPoint.toRoutePoint() = RoutePoint(latitude, longitude)

/** A saved walk or run with the path it took. */
data class RunDetail(
    val summary: RunSummary,
    /** Empty for runs saved before routes were kept, or with too few good fixes to draw. */
    val route: List<RoutePoint>,
) {
    /** A line needs at least two points. */
    val hasRoute: Boolean get() = route.size >= 2
}

/**
 * Part of the day a run started in, for Strava-style names like "Morning run" or
 * "Lunch walk". Plain Kotlin so it is easy to test; the words come from string resources.
 */
enum class DayPart {
    MORNING,
    LUNCH,
    AFTERNOON,
    EVENING,
    NIGHT,
    ;

    companion object {
        fun of(hour: Int): DayPart = when (hour) {
            in 5..10 -> MORNING
            in 11..13 -> LUNCH
            in 14..17 -> AFTERNOON
            in 18..21 -> EVENING
            else -> NIGHT
        }

        fun of(timeMillis: Long, zone: ZoneId): DayPart = of(Instant.ofEpochMilli(timeMillis).atZone(zone).hour)
    }
}

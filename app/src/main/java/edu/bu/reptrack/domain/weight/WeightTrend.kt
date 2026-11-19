package edu.bu.reptrack.domain.weight

import kotlin.math.abs

/** A point on the chart. Both values go from 0 to 1: x from first to last weigh-in, y from bottom to top. */
data class ChartPoint(val x: Float, val y: Float)

/** Everything the chart needs, already scaled, so the drawing code only multiplies by its size. */
data class WeightChartData(
    val points: List<ChartPoint>,
    /** Height of the goal line, from 0 to 1. Null when there is no goal. */
    val goalY: Float?,
    /** Weight at the bottom and at the top of the chart, for the labels next to it. */
    val bottomKg: Float,
    val topKg: Float,
)

/** The math behind the weight card. Pure Kotlin so it is easy to test. */
object WeightTrend {
    /** How many weigh-ins the chart shows. Older ones stay saved; the chart just starts later. */
    const val CHART_POINTS = 30

    /** Space above and below the line, so it never touches the edge of the chart. */
    private const val PADDING_KG = 1f

    /** Same rule as the rest of the app: closer than 0.1 kg counts as at the goal. */
    private const val AT_GOAL_KG = 0.1f

    /** Change from the first to the last weigh-in. Negative means weight lost. Null with fewer than two. */
    fun changeKg(entries: List<WeightEntry>): Float? {
        if (entries.size < 2) return null
        return entries.last().weightKg - entries.first().weightKg
    }

    /**
     * How much of the way from the start weight to the goal is done, from 0 to 1. Works for
     * losing and for gaining. Moving away from the goal counts as 0, passing it as 1. Null
     * when the start already was the goal, since there was no way to go.
     */
    fun goalProgress(startKg: Float, currentKg: Float, goalKg: Float): Float? {
        val total = startKg - goalKg
        if (abs(total) < AT_GOAL_KG) return null
        return ((startKg - currentKg) / total).coerceIn(0f, 1f)
    }

    /** Scales the last [CHART_POINTS] weigh-ins (given oldest first) and the goal to fit the chart. */
    fun chart(entries: List<WeightEntry>, goalKg: Float?): WeightChartData? {
        if (entries.isEmpty()) return null
        val shown = entries.takeLast(CHART_POINTS)
        // The goal is part of the range, so the chart shows how far there is still to go.
        val weights = shown.map { it.weightKg } + listOfNotNull(goalKg)
        val bottom = weights.min() - PADDING_KG
        val top = weights.max() + PADDING_KG
        fun heightOf(kg: Float) = (kg - bottom) / (top - bottom)

        val firstTime = shown.first().loggedAt
        val timeSpan = (shown.last().loggedAt - firstTime).toFloat()
        val points = shown.map { entry ->
            // Spaced by date, so a two-week break looks like one. A single weigh-in sits in the middle.
            val x = if (timeSpan > 0f) (entry.loggedAt - firstTime) / timeSpan else 0.5f
            ChartPoint(x, heightOf(entry.weightKg))
        }
        return WeightChartData(points, goalKg?.let(::heightOf), bottom, top)
    }
}

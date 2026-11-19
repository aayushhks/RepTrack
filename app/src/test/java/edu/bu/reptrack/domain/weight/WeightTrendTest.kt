package edu.bu.reptrack.domain.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightTrendTest {

    private val day = 24 * 60 * 60_000L

    private fun entries(vararg kg: Float) = kg.mapIndexed { i, w -> WeightEntry(id = i + 1L, loggedAt = i * day, weightKg = w) }

    @Test
    fun change_isLastMinusFirst_andNeedsTwoWeighIns() {
        assertNull(WeightTrend.changeKg(entries(82f)))
        assertEquals(-2.5f, WeightTrend.changeKg(entries(84.5f, 83f, 82f))!!, 0.001f)
    }

    @Test
    fun goalProgress_whenLosing() {
        // Start 90, goal 80, now 85: half way.
        assertEquals(0.5f, WeightTrend.goalProgress(90f, 85f, 80f)!!, 0.001f)
    }

    @Test
    fun goalProgress_whenGaining() {
        // Start 60, goal 70, now 63: 30 % of the way.
        assertEquals(0.3f, WeightTrend.goalProgress(60f, 63f, 70f)!!, 0.001f)
    }

    @Test
    fun goalProgress_staysBetweenZeroAndOne() {
        assertEquals(0f, WeightTrend.goalProgress(90f, 92f, 80f)!!, 0f) // moved away from the goal
        assertEquals(1f, WeightTrend.goalProgress(90f, 78f, 80f)!!, 0f) // went past the goal
    }

    @Test
    fun goalProgress_isNull_whenTheStartWasTheGoal() {
        assertNull(WeightTrend.goalProgress(80f, 79f, 80f))
    }

    @Test
    fun chart_isNull_withoutWeighIns() {
        assertNull(WeightTrend.chart(emptyList(), goalKg = 75f))
    }

    @Test
    fun chart_spreadsPointsByDate_andIncludesTheGoal() {
        val list = listOf(
            WeightEntry(1, loggedAt = 0, weightKg = 84f),
            WeightEntry(2, loggedAt = 1 * day, weightKg = 83f),
            WeightEntry(3, loggedAt = 4 * day, weightKg = 82f),
        )
        val chart = WeightTrend.chart(list, goalKg = 78f)!!

        // Range is goal 78 to 84, plus 1 kg on each side: 77 to 85.
        assertEquals(77f, chart.bottomKg, 0.001f)
        assertEquals(85f, chart.topKg, 0.001f)
        assertEquals(listOf(0f, 0.25f, 1f), chart.points.map { it.x })
        assertEquals(7f / 8f, chart.points.first().y, 0.001f) // 84 kg
        assertEquals(1f / 8f, chart.goalY!!, 0.001f) // 78 kg
    }

    @Test
    fun chart_withOneWeighIn_putsItInTheMiddle() {
        val chart = WeightTrend.chart(entries(80f), goalKg = null)!!

        assertEquals(ChartPoint(0.5f, 0.5f), chart.points.single())
        assertNull(chart.goalY)
    }

    @Test
    fun chart_showsOnlyTheLatestWeighIns() {
        val many = (1..40).map { WeightEntry(it.toLong(), loggedAt = it * day, weightKg = 80f + it / 10f) }

        val chart = WeightTrend.chart(many, goalKg = null)!!

        assertEquals(WeightTrend.CHART_POINTS, chart.points.size)
    }
}

package edu.bu.reptrack.domain.workout

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** What the user did on one day. A workout wins over a run when both happened. */
enum class DayKind { NONE, WORKOUT, RUN }

/** One day in the week strip on the Workouts tab. */
data class DayActivity(val date: LocalDate, val kind: DayKind, val isToday: Boolean)

/** The week strip and the streak. Pure Kotlin so it is easy to test. */
object WeekActivity {

    /** Monday to Sunday of the week that holds [nowMillis], with what was done on each day. */
    fun thisWeek(
        workoutTimes: List<Long>,
        runTimes: List<Long>,
        nowMillis: Long,
        zone: ZoneId,
    ): List<DayActivity> {
        val today = dateOf(nowMillis, zone)
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val workoutDays = workoutTimes.map { dateOf(it, zone) }.toSet()
        val runDays = runTimes.map { dateOf(it, zone) }.toSet()
        return (0L until 7L).map { offset ->
            val date = monday.plusDays(offset)
            val kind = when (date) {
                in workoutDays -> DayKind.WORKOUT
                in runDays -> DayKind.RUN
                else -> DayKind.NONE
            }
            DayActivity(date, kind, isToday = date.isEqual(today))
        }
    }

    /**
     * Days in a row with a workout or a run, counting back from today. A day off today does not
     * break it yet (the day is not over), so the count then starts from yesterday.
     */
    fun streakDays(activityTimes: List<Long>, nowMillis: Long, zone: ZoneId): Int {
        val days = activityTimes.map { dateOf(it, zone) }.toSet()
        val today = dateOf(nowMillis, zone)
        var day = if (today in days) today else today.minusDays(1)
        var count = 0
        while (day in days) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    private fun dateOf(millis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}

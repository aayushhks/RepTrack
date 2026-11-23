package edu.bu.reptrack.domain.workout

import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.StarterPlan
import edu.bu.reptrack.domain.profile.TrainingPlace
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** What a day of the week plan says. */
enum class PlanDayStatus {
    /** A workout was finished that day. */
    DONE,

    /** A training day in the past with no workout. */
    MISSED,

    /** Today is a training day and there is a workout to do. */
    TODAY,

    /** A training day later this week. */
    PLANNED,

    /** A day off. */
    REST,

    /** A day off with a walk or run. */
    RAN,
}

/** One day of the week plan: the routine planned or done, or a day off. */
data class PlanDay(
    val date: LocalDate,
    val isToday: Boolean,
    val isTrainingDay: Boolean,
    /** The routine planned for the day, or the one done. Null on a day off or a missed day. */
    val routineId: String?,
    val routineName: String?,
    val status: PlanDayStatus,
)

/**
 * The week from Monday to Sunday: which routine goes on which training day. It follows the same
 * order as "Up next" on the Workouts tab: the next training day gets the next routine, the one
 * after gets the one after, and so on. Days already done show what was done. Plain Kotlin so it
 * is easy to test.
 */
object WeekPlan {

    /** A workout finished on [date]. */
    data class Done(val date: LocalDate, val routineId: String?, val name: String)

    /** Training days spread over the week, for a plan of [daysPerWeek] days, until the user picks their own. */
    fun defaultDays(daysPerWeek: Int): Set<DayOfWeek> = when {
        daysPerWeek <= 1 -> setOf(DayOfWeek.MONDAY)
        daysPerWeek == 2 -> setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
        daysPerWeek == 3 -> setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        daysPerWeek == 4 -> setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        daysPerWeek == 5 -> setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
        daysPerWeek == 6 -> DayOfWeek.entries.toSet() - DayOfWeek.SUNDAY
        else -> DayOfWeek.entries.toSet()
    }

    /** The user's own days, or the plan's default days when they have not picked any. */
    fun trainingDays(chosen: Set<DayOfWeek>, daysPerWeek: Int): Set<DayOfWeek> =
        chosen.ifEmpty { defaultDays(daysPerWeek) }

    fun build(
        today: LocalDate,
        trainingDays: Set<DayOfWeek>,
        routines: List<WorkoutTemplate>,
        next: WorkoutTemplate?,
        done: List<Done>,
        runDates: Set<LocalDate>,
    ): List<PlanDay> {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val doneByDate = done.groupBy { it.date }.mapValues { (_, workouts) -> workouts.last() }
        var upcoming = routines.indexOfFirst { it.id == next?.id }.coerceAtLeast(0)
        return (0L until 7L).map { offset ->
            val date = monday.plusDays(offset)
            val isToday = date.isEqual(today)
            val training = date.dayOfWeek in trainingDays
            val workout = doneByDate[date]
            val dayOff = if (date in runDates) PlanDayStatus.RAN else PlanDayStatus.REST
            when {
                workout != null -> PlanDay(date, isToday, training, workout.routineId, workout.name, PlanDayStatus.DONE)
                date.isBefore(today) -> PlanDay(date, isToday, training, null, null, if (training) PlanDayStatus.MISSED else dayOff)
                training && routines.isNotEmpty() -> {
                    val routine = routines[upcoming % routines.size]
                    upcoming++
                    PlanDay(date, isToday, true, routine.id, routine.name, if (isToday) PlanDayStatus.TODAY else PlanDayStatus.PLANNED)
                }
                else -> PlanDay(date, isToday, training, null, null, dayOff)
            }
        }
    }
}

/** Everything about the user's plan this week, worked out from what is saved. */
data class PlanSnapshot(
    val plan: StarterPlan,
    /** The plan's routines, with the user's changes. */
    val routines: List<WorkoutTemplate>,
    /** The routine to do next, going round the routines in order. */
    val next: WorkoutTemplate?,
    val trainingDays: Set<DayOfWeek>,
    /** The user picked their own days, instead of the plan's default ones. */
    val customDays: Boolean,
    val week: List<PlanDay>,
) {
    companion object {
        /**
         * Shared by the Workouts tab and the week plan screen, so both always agree on what is
         * next. [history] is finished workouts; [runTimes] are when runs finished.
         */
        fun of(
            goal: FitnessGoal,
            place: TrainingPlace,
            edits: Map<String, List<String>>,
            chosenDays: Set<DayOfWeek>,
            history: List<SessionSummary>,
            runTimes: List<Long>,
            nowMillis: Long,
            zone: ZoneId,
        ): PlanSnapshot {
            val plan = StarterPlan.suggest(goal, place)
            val routines = RoutineEdits.apply(StarterRoutines.forPlan(plan.type), edits)
            val ids = routines.map { it.id }.toSet()
            val lastRoutineId = history
                .filter { it.session.routineId in ids }
                .maxByOrNull { it.session.finishedAt ?: it.session.startedAt }
                ?.session?.routineId
            val next = nextRoutine(routines, lastRoutineId)
            val days = WeekPlan.trainingDays(chosenDays, plan.daysPerWeek)
            val week = WeekPlan.build(
                today = dateOf(nowMillis, zone),
                trainingDays = days,
                routines = routines,
                next = next,
                done = history.map { WeekPlan.Done(dateOf(it.session.finishedAt ?: it.session.startedAt, zone), it.session.routineId, it.session.name) },
                runDates = runTimes.map { dateOf(it, zone) }.toSet(),
            )
            return PlanSnapshot(plan, routines, next, days, customDays = chosenDays.isNotEmpty(), week = week)
        }

        private fun dateOf(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    }
}

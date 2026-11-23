package edu.bu.reptrack.domain.workout

import java.time.DayOfWeek

/**
 * The user's own plan: routines they named and filled themselves, and which one they do on each
 * day of the week, the same every week. With no routines the user follows the suggested plan.
 * Plain Kotlin so it is easy to test.
 */
data class CustomPlan(
    val routines: List<WorkoutTemplate> = emptyList(),
    /** Routine id for each training day. Days that are not in the map are days off. */
    val schedule: Map<DayOfWeek, String> = emptyMap(),
) {
    /** The user made their own plan, so it is used instead of the suggested one. */
    val isActive: Boolean get() = routines.isNotEmpty()

    /** Each training day with its routine. Days on a routine that no longer exists are left out. */
    val scheduledRoutines: Map<DayOfWeek, WorkoutTemplate>
        get() = schedule.mapNotNull { (day, id) -> routines.firstOrNull { it.id == id }?.let { day to it } }.toMap()

    companion object {
        /**
         * Which routine goes on each of [days] when a plan of [count] routines starts: the first
         * day gets the first routine, the next day the next one, and so on round the list. As
         * indexes into the routine list.
         */
        fun rotation(count: Int, days: Set<DayOfWeek>): Map<DayOfWeek, Int> {
            if (count <= 0) return emptyMap()
            return days.sorted().withIndex().associate { (index, day) -> day to index % count }
        }

        /**
         * The week after the routine [deleted] is gone: its days become days off. If that would
         * leave no training day at all, they go to the first routine left instead.
         */
        fun scheduleAfterDelete(
            schedule: Map<DayOfWeek, String>,
            deleted: String,
            remaining: List<String>,
        ): Map<DayOfWeek, String> {
            val kept = schedule.filterValues { it != deleted && it in remaining }
            val first = remaining.firstOrNull { it != deleted }
            if (kept.isNotEmpty() || first == null) return kept
            return schedule.filterValues { it == deleted }.mapValues { first }
        }
    }
}

/** A routine name the user typed. */
object RoutineName {
    const val MAX_LENGTH = 30

    /** Trimmed, single spaces, at most [MAX_LENGTH] characters. Null when nothing is left. */
    fun clean(text: String): String? =
        text.trim().replace(Regex("\\s+"), " ").take(MAX_LENGTH).trim().ifEmpty { null }
}

/** A routine by its id: one of the user's own, or a built-in one with the user's changes. */
fun findRoutine(id: String, edits: Map<String, List<String>>, custom: CustomPlan): WorkoutTemplate? =
    custom.routines.firstOrNull { it.id == id } ?: StarterRoutines.find(id)?.withEdits(edits)

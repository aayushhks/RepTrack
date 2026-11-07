package edu.bu.reptrack.domain.workout

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Total volume: the sum of weight times reps over all the sets. */
fun totalVolumeKg(sets: List<LoggedSet>): Float =
    sets.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat()

/** Whole minutes from start to finish, or null if the session is not finished yet. */
fun durationMinutes(session: WorkoutSession): Long? =
    session.finishedAt?.let { finished -> (finished - session.startedAt).coerceAtLeast(0) / 60_000 }

/**
 * The exercises to show for a session: the routine's exercises in order, then any other
 * exercise that has sets (so a set is never hidden if the routine list changes later).
 */
fun exercisesToShow(routineExercises: List<String>, sets: List<LoggedSet>): List<String> =
    (routineExercises + sets.map { it.exerciseName }).distinct()

/** Midnight on the Monday of the current week, in the phone's time zone, as epoch milliseconds. */
fun startOfWeekMillis(nowMillis: Long, zone: ZoneId): Long =
    Instant.ofEpochMilli(nowMillis)
        .atZone(zone)
        .toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()

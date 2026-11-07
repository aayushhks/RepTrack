package edu.bu.reptrack.domain.workout

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

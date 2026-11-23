package edu.bu.reptrack.domain.workout

/**
 * Changes to a routine's exercise list. Each one returns a new list and leaves the rest of the
 * routine as it was, so swapping one exercise keeps the others in place. Plain Kotlin so it is
 * easy to test.
 */
object RoutineEdits {
    fun add(exercises: List<String>, name: String): List<String> =
        if (name.isBlank() || exercises.containsName(name)) exercises else exercises + name.trim()

    /** A routine keeps at least one exercise. */
    fun remove(exercises: List<String>, index: Int): List<String> =
        if (exercises.size <= 1 || index !in exercises.indices) exercises else exercises.filterIndexed { i, _ -> i != index }

    /** Puts [name] where the old exercise was. Nothing changes if [name] is already elsewhere in the routine. */
    fun replace(exercises: List<String>, index: Int, name: String): List<String> {
        if (name.isBlank() || index !in exercises.indices) return exercises
        val elsewhere = exercises.withIndex().any { (i, it) -> i != index && it.equals(name.trim(), ignoreCase = true) }
        return if (elsewhere) exercises else exercises.toMutableList().apply { set(index, name.trim()) }
    }

    /** Moves the exercise at [from] one place up (-1) or down (+1). */
    fun move(exercises: List<String>, from: Int, by: Int): List<String> {
        val to = from + by
        if (from !in exercises.indices || to !in exercises.indices) return exercises
        return exercises.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** The built-in routines with the user's changes, keyed by routine id. */
    fun apply(routines: List<WorkoutTemplate>, edits: Map<String, List<String>>): List<WorkoutTemplate> =
        routines.map { it.withEdits(edits) }

    private fun List<String>.containsName(name: String) = any { it.equals(name.trim(), ignoreCase = true) }
}

/** This routine with the user's exercise list, if they changed it. */
fun WorkoutTemplate.withEdits(edits: Map<String, List<String>>): WorkoutTemplate {
    val changed = edits[id]?.takeIf { it.isNotEmpty() } ?: return this
    return copy(exercises = changed, isEdited = changed != exercises)
}

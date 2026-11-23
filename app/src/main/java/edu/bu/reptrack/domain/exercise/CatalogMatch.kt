package edu.bu.reptrack.domain.exercise

/**
 * Finds a routine's exercise in the wger catalog by name. The built-in routines use short names
 * like "Lat pulldown", and wger may call it "Lat Pull Down" or "Lat Pulldown (Wide Grip)", so
 * this tries, in order: the same name, a catalog name that holds the whole name, then one that
 * holds every word. The shortest such name wins, since it is usually the plain version.
 * Plain Kotlin so it is easy to test.
 */
object CatalogMatch {

    fun find(name: String, catalog: List<Exercise>): Exercise? {
        val key = squash(name)
        if (key.isEmpty()) return null
        catalog.firstOrNull { squash(it.name) == key }?.let { return it }
        catalog.filter { squash(it.name).contains(key) }.minByOrNull { it.name.length }?.let { return it }
        val words = words(name)
        return catalog
            .filter { exercise -> words(exercise.name).let { theirs -> words.all { it in theirs } } }
            .minByOrNull { it.name.length }
    }

    /**
     * Catalog exercises for the same muscles as [exercise], best first: most muscles in common,
     * then the same equipment. Leaves out [exercise] itself and the names in [exclude].
     */
    fun sameMuscles(exercise: Exercise, catalog: List<Exercise>, exclude: Collection<String>, limit: Int = 8): List<Exercise> {
        val muscles = exercise.muscles.toSet()
        if (muscles.isEmpty()) return emptyList()
        val skip = exclude.map(::squash).toSet() + squash(exercise.name)
        return catalog
            .filter { it.id != exercise.id && squash(it.name) !in skip }
            .map { it to it.muscles.count { muscle -> muscle in muscles } }
            .filter { (_, shared) -> shared > 0 }
            .sortedWith(
                compareByDescending<Pair<Exercise, Int>> { it.second }
                    .thenByDescending { (other, _) -> other.equipment == exercise.equipment }
                    .thenBy { it.first.name },
            )
            .take(limit)
            .map { it.first }
    }

    /** Lower case, letters and digits only: "Lat Pull-Down" and "lat pulldown" become the same. */
    fun squash(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

    private fun words(text: String): Set<String> =
        text.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }.toSet()
}

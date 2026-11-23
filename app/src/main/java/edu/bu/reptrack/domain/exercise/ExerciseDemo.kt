package edu.bu.reptrack.domain.exercise

/**
 * How to do one exercise, from Free Exercise DB: two photos (the start and the end of the
 * movement) and the steps. Public domain, so the app can show it freely.
 */
data class ExerciseDemo(
    val id: String,
    val name: String,
    /** Usually two: the start position, then the end position. */
    val imageUrls: List<String>,
    val steps: List<String>,
    val primaryMuscles: List<String>,
    val equipment: String?,
)

/**
 * Finds the photos for an exercise by its name. Our routines say "Bench press" where the photo
 * set says "Barbell Bench Press - Medium Grip", so the exercises in the built-in routines are
 * matched by hand below; any other name goes to the photo set exercise that has all its words
 * and the fewest extra ones, preferring the same equipment. Plain Kotlin so it is easy to test.
 */
object DemoMatch {

    /** The built-in routines' exercises, keyed by squashed name (see [CatalogMatch.squash]). */
    private val byHand = mapOf(
        "squat" to "Barbell_Squat",
        "squats" to "Barbell_Squat",
        "benchpress" to "Barbell_Bench_Press_-_Medium_Grip",
        "barbellrow" to "Bent_Over_Barbell_Row",
        "plank" to "Plank",
        "deadlift" to "Barbell_Deadlift",
        "overheadpress" to "Standing_Military_Press",
        "latpulldown" to "Wide-Grip_Lat_Pulldown",
        "walkinglunge" to "Bodyweight_Walking_Lunge",
        "bicepscurl" to "Dumbbell_Bicep_Curl",
        "romaniandeadlift" to "Romanian_Deadlift",
        "legpress" to "Leg_Press",
        "calfraise" to "Standing_Calf_Raises",
        "pushup" to "Pushups",
        "pushups" to "Pushups",
        "bodyweightsquat" to "Bodyweight_Squat",
        "glutebridge" to "Butt_Lift_Bridge",
        "reverselunge" to "Crossover_Reverse_Lunge",
        "superman" to "Superman",
        "mountainclimber" to "Mountain_Climbers",
        "chairdip" to "Bench_Dips",
        "splitsquat" to "Split_Squats",
        "tablerow" to "Inverted_Row",
        "singlelegglutebridge" to "Single_Leg_Glute_Bridge",
        "sideplank" to "Side_Bridge",
    )

    fun find(name: String, demos: List<ExerciseDemo>, equipment: List<String> = emptyList()): ExerciseDemo? {
        val key = CatalogMatch.squash(name)
        if (key.isEmpty() || demos.isEmpty()) return null
        byHand[key]?.let { id -> demos.firstOrNull { it.id == id }?.let { return it } }
        demos.firstOrNull { CatalogMatch.squash(it.name) == key }?.let { return it }

        val wanted = words(name)
        // Equipment words already in the name (a "Bench" for "Bench press") don't tell the photos apart.
        val gear = equipment.flatMap { words(it) }.toSet() - wanted
        fun sameGear(demo: ExerciseDemo, theirs: Set<String>) =
            theirs.any { it in gear } || words(demo.equipment.orEmpty()).any { it in gear }
        return demos
            .map { demo -> demo to words(demo.name) }
            .filter { (_, theirs) -> theirs.containsAll(wanted) }
            .sortedWith(
                compareByDescending<Pair<ExerciseDemo, Set<String>>> { (demo, theirs) -> sameGear(demo, theirs) }
                    .thenBy { (_, theirs) -> (theirs - wanted).size }
                    .thenBy { (demo, _) -> demo.name.length },
            )
            .firstOrNull()
            ?.first
    }

    /** Lower case words without a plural s, so "Squats" and "squat" are the same word. */
    private fun words(text: String): Set<String> =
        text.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotEmpty() }
            .map { if (it.length > 3 && it.endsWith("s")) it.dropLast(1) else it }
            .toSet()
}

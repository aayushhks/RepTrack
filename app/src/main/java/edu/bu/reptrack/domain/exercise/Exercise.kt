package edu.bu.reptrack.domain.exercise

/** One exercise from the wger catalog, in the shape the screens use. */
data class Exercise(
    val id: Int,
    val name: String,
    /** Plain text, wger sends HTML. */
    val description: String,
    val category: String,
    val muscles: List<String>,
    val equipment: List<String>,
    val imageUrl: String?,
)

/** Exercises whose name contains [query] (ignoring case), limited to [category] when it is set. */
fun filterExercises(all: List<Exercise>, query: String, category: String?): List<Exercise> {
    val text = query.trim()
    return all.filter { exercise ->
        (category == null || exercise.category == category) &&
            (text.isEmpty() || exercise.name.contains(text, ignoreCase = true))
    }
}

/** The categories in [all], sorted, for the filter chips. */
fun categoriesOf(all: List<Exercise>): List<String> =
    all.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

/** Turns wger's HTML description into plain text, one line per paragraph or list item. */
fun htmlToPlainText(html: String): String =
    html
        .replace(Regex("(?i)<li[^>]*>"), "• ")
        .replace(Regex("(?i)</(p|li|ol|ul)>|<br\\s*/?>"), "\n")
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")

package edu.bu.reptrack.domain.exercise

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseTest {

    private fun exercise(id: Int, name: String, category: String) =
        Exercise(id, name, "", category, emptyList(), emptyList(), null)

    private val all = listOf(
        exercise(1, "Bench Press", "Chest"),
        exercise(2, "Squat", "Legs"),
        exercise(3, "Leg Press", "Legs"),
    )

    @Test
    fun search_ignoresCaseAndSpaces() {
        assertEquals(listOf(1, 3), filterExercises(all, "  PRESS ", null).map { it.id })
    }

    @Test
    fun category_limitsTheList() {
        assertEquals(listOf(2, 3), filterExercises(all, "", "Legs").map { it.id })
    }

    @Test
    fun searchAndCategory_workTogether() {
        assertEquals(listOf(3), filterExercises(all, "press", "Legs").map { it.id })
    }

    @Test
    fun categories_areSortedWithoutDuplicates() {
        assertEquals(listOf("Chest", "Legs"), categoriesOf(all))
    }

    @Test
    fun html_becomesPlainTextLines() {
        val html = "<p>Stand up.</p><ol><li>Hinge &amp; pull</li><li>Drive hips</li></ol>"
        assertEquals("Stand up.\n• Hinge & pull\n• Drive hips", htmlToPlainText(html))
    }
}

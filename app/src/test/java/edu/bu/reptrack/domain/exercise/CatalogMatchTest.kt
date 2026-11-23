package edu.bu.reptrack.domain.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogMatchTest {

    private fun exercise(id: Int, name: String, muscles: List<String> = emptyList(), equipment: List<String> = emptyList()) =
        Exercise(id, name, description = "", category = "", muscles = muscles, equipment = equipment, imageUrl = null)

    private val catalog = listOf(
        exercise(1, "Bench Press", listOf("Chest", "Triceps"), listOf("Barbell")),
        exercise(2, "Incline Bench Press", listOf("Chest", "Shoulders"), listOf("Barbell")),
        exercise(3, "Lat Pull Down (Wide Grip)", listOf("Lats"), listOf("Cable")),
        exercise(4, "Bent Over Barbell Row", listOf("Lats", "Biceps"), listOf("Barbell")),
        exercise(5, "Push-Ups", listOf("Chest", "Triceps")),
        exercise(6, "Dumbbell Bench Press", listOf("Chest", "Triceps"), listOf("Dumbbell")),
        exercise(7, "Squats", listOf("Quads")),
    )

    @Test
    fun find_sameName_ignoringCaseAndSigns() {
        assertEquals(1, CatalogMatch.find("bench press", catalog)?.id)
        assertEquals(1, CatalogMatch.find("Bench-Press", catalog)?.id)
    }

    @Test
    fun find_aLongerCatalogName_theShortestWins() {
        assertEquals(3, CatalogMatch.find("Lat pulldown", catalog)?.id)
        assertEquals(7, CatalogMatch.find("Squat", catalog)?.id)
    }

    @Test
    fun find_everyWord_inAnyOrder() {
        assertEquals(4, CatalogMatch.find("Barbell row", catalog)?.id)
    }

    @Test
    fun find_nothing() {
        assertNull(CatalogMatch.find("Hollow hold", catalog))
        assertNull(CatalogMatch.find("", catalog))
    }

    @Test
    fun sameMuscles_mostInCommonFirst_thenSameEquipment() {
        val bench = catalog.first { it.id == 1 }

        val names = CatalogMatch.sameMuscles(bench, catalog, exclude = listOf("Push-ups")).map { it.name }

        // Dumbbell bench shares chest and triceps; incline shares chest and uses a barbell.
        assertEquals(listOf("Dumbbell Bench Press", "Incline Bench Press"), names)
    }
}

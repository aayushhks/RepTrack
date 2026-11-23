package edu.bu.reptrack.domain.exercise

import edu.bu.reptrack.testing.demo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DemoMatchTest {

    private val photos = listOf(
        demo("Machine_Bench_Press", "Machine Bench Press", "machine"),
        demo("Dumbbell_Bench_Press", "Dumbbell Bench Press", "dumbbell"),
        demo("Barbell_Bench_Press_-_Medium_Grip", "Barbell Bench Press - Medium Grip", "barbell"),
        demo("Box_Squat", "Box Squat", "barbell"),
        demo("Barbell_Squat", "Barbell Squat", "barbell"),
        demo("Pushups", "Pushups", "body only"),
        demo("Incline_Push-Up", "Incline Push-Up", "body only"),
        demo("Face_Pull", "Face Pull", "cable"),
        demo("Seated_Cable_Rows", "Seated Cable Rows", "cable"),
    )

    @Test
    fun builtInExercises_getThePhotosPickedByHand() {
        assertEquals("Barbell_Bench_Press_-_Medium_Grip", DemoMatch.find("Bench press", photos)?.id)
        assertEquals("Barbell_Squat", DemoMatch.find("Squat", photos)?.id)
        assertEquals("Pushups", DemoMatch.find("Push-up", photos)?.id)
    }

    @Test
    fun theSameName_matches() {
        assertEquals("Face_Pull", DemoMatch.find("face pull", photos)?.id)
    }

    @Test
    fun otherNames_matchByWords_fewestExtraWords_andPluralsDoNotMatter() {
        assertEquals("Seated_Cable_Rows", DemoMatch.find("Seated cable row", photos)?.id)
    }

    @Test
    fun sameEquipment_wins() {
        // "Press" and "bench" are in three names; the catalog's equipment picks one.
        assertEquals("Dumbbell_Bench_Press", DemoMatch.find("Press bench", photos, listOf("Dumbbell"))?.id)
        assertEquals("Barbell_Bench_Press_-_Medium_Grip", DemoMatch.find("Press bench", photos, listOf("Barbell", "Bench"))?.id)
    }

    @Test
    fun withoutEquipment_theShortestCloseName_wins() {
        assertEquals("Machine_Bench_Press", DemoMatch.find("Press bench", photos)?.id)
    }

    @Test
    fun noPhotos_isNull() {
        assertNull(DemoMatch.find("Hollow hold", photos))
        assertNull(DemoMatch.find("Squat", emptyList()))
    }
}

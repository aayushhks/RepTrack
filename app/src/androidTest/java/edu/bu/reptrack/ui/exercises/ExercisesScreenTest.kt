package edu.bu.reptrack.ui.exercises

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.ui.theme.RepTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the Exercises tab, drawn from a fixed state. */
@RunWith(AndroidJUnit4::class)
class ExercisesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var typed: String? = null
    private var category: String? = null
    private var retried = false
    private var cleared = false
    private var opened: Int? = null

    private fun show(state: ExercisesUiState) {
        composeRule.setContent {
            RepTrackTheme {
                ExercisesScreen(
                    state = state,
                    onQueryChange = { typed = it },
                    onCategoryClick = { category = it },
                    onClearSearch = { cleared = true },
                    onRetry = { retried = true },
                    onOpenExercise = { opened = it },
                )
            }
        }
    }

    private val squat = Exercise(
        id = 1,
        name = "Squat",
        description = "Keep your back straight.",
        category = "Legs",
        muscles = listOf("Quads", "Glutes"),
        equipment = listOf("Barbell"),
        imageUrl = null,
    )
    private val bench = squat.copy(id = 2, name = "Bench Press", category = "Chest")

    private val loaded = ExercisesUiState(
        isLoading = false,
        exercises = listOf(squat, bench),
        categories = listOf("Chest", "Legs"),
        hasSaved = true,
    )

    @Test
    fun loading_showsAProgressIndicator() {
        show(ExercisesUiState(isLoading = true))
        composeRule.onNodeWithTag("exercises_loading").assertIsDisplayed()
    }

    @Test
    fun loaded_showsTheExercises() {
        show(loaded)
        composeRule.onNodeWithText("Squat").assertIsDisplayed()
        composeRule.onNodeWithText("Bench Press").assertIsDisplayed()
    }

    @Test
    fun rows_showMusclesAndEquipment_orBodyweight() {
        show(loaded.copy(exercises = listOf(squat, bench.copy(muscles = listOf("Pecs"), equipment = emptyList()))))
        composeRule.onNodeWithText("Quads, Glutes").assertIsDisplayed()
        composeRule.onNodeWithText("Pecs").assertIsDisplayed()
        composeRule.onNodeWithText("Barbell").assertIsDisplayed()
        composeRule.onNodeWithText("Bodyweight").assertIsDisplayed()
    }

    @Test
    fun tappingAnExercise_opensIt() {
        show(loaded)
        composeRule.onNodeWithTag("exercise_1").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun typingAndPickingACategory_reportToTheViewModel() {
        show(loaded)
        composeRule.onNodeWithTag("exercises_search").performTextReplacement("squ")
        assertEquals("squ", typed)
        composeRule.onNodeWithText("Legs").performClick()
        assertEquals("Legs", category)
    }

    @Test
    fun noMatches_saysSo_andClearsTheSearch() {
        show(loaded.copy(exercises = emptyList(), query = "zzz"))
        composeRule.onNodeWithTag("exercises_no_matches").assertIsDisplayed()
        composeRule.onNodeWithText("No exercises match “zzz”.").assertIsDisplayed()
        composeRule.onNodeWithText("Clear search").performClick()
        assertTrue(cleared)
    }

    @Test
    fun failedWithNothingSaved_showsTheErrorAndRetries() {
        show(ExercisesUiState(isLoading = false, refreshFailed = true))
        composeRule.onNodeWithTag("exercises_error").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()
        assertTrue(retried)
    }

    @Test
    fun failedWithSavedExercises_keepsTheListAndShowsABanner() {
        show(loaded.copy(refreshFailed = true))
        composeRule.onNodeWithTag("exercises_offline").assertIsDisplayed()
        composeRule.onNodeWithText("Squat").assertIsDisplayed()
    }
}

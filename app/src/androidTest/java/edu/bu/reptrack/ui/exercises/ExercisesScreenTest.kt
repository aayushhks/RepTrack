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

    private fun show(state: ExercisesUiState) {
        composeRule.setContent {
            RepTrackTheme {
                ExercisesScreen(
                    state = state,
                    onQueryChange = { typed = it },
                    onCategoryClick = { category = it },
                    onRetry = { retried = true },
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
    fun tappingAnExercise_showsHowToDoIt() {
        show(loaded)
        composeRule.onNodeWithTag("exercise_1").performClick()
        composeRule.onNodeWithText("Keep your back straight.").assertIsDisplayed()
        composeRule.onNodeWithText("Muscles: Quads, Glutes").assertIsDisplayed()
    }

    @Test
    fun typingAndPickingACategory_reportToTheViewModel() {
        show(loaded)
        composeRule.onNodeWithTag("exercises_search").performTextReplacement("squ")
        composeRule.onNodeWithText("Legs").performClick()
        assertEquals("squ", typed)
        assertEquals("Legs", category)
    }

    @Test
    fun noMatches_saysSo() {
        show(loaded.copy(exercises = emptyList(), query = "zzz"))
        composeRule.onNodeWithTag("exercises_no_matches").assertIsDisplayed()
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

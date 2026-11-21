package edu.bu.reptrack.ui.exercises

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.ui.theme.RepTrackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the exercise detail, drawn from a fixed state. */
@RunWith(AndroidJUnit4::class)
class ExerciseDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var wentBack = false

    private fun show(state: ExerciseDetailUiState) {
        composeRule.setContent {
            RepTrackTheme { ExerciseDetailScreen(state = state, onBack = { wentBack = true }) }
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

    @Test
    fun showsTheExercise() {
        show(ExerciseDetailUiState(isLoading = false, exercise = squat))
        composeRule.onNodeWithText("Squat").assertIsDisplayed()
        composeRule.onNodeWithText("Legs").assertIsDisplayed()
        composeRule.onNodeWithText("Quads, Glutes").assertIsDisplayed()
        composeRule.onNodeWithText("Barbell").assertIsDisplayed()
        composeRule.onNodeWithText("Keep your back straight.").assertIsDisplayed()
    }

    @Test
    fun noEquipment_saysBodyweight() {
        show(ExerciseDetailUiState(isLoading = false, exercise = squat.copy(equipment = emptyList())))
        composeRule.onNodeWithText("Bodyweight").assertIsDisplayed()
    }

    @Test
    fun unknownExercise_saysSo() {
        show(ExerciseDetailUiState(isLoading = false, exercise = null))
        composeRule.onNodeWithTag("exercise_not_found").assertIsDisplayed()
    }

    @Test
    fun backButton_goesBack() {
        show(ExerciseDetailUiState(isLoading = false, exercise = squat))
        composeRule.onNodeWithTag("exercise_detail_back").performClick()
        assertTrue(wentBack)
    }
}

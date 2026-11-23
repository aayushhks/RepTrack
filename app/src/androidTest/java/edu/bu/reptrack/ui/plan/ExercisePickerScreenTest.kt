package edu.bu.reptrack.ui.plan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.ui.theme.RepTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the catalog picker, drawn from a fixed state. */
@RunWith(AndroidJUnit4::class)
class ExercisePickerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var picked: String? = null
    private var typed: String? = null

    private val dips = Exercise(3, "Dips", "", "Chest", listOf("Chest", "Triceps"), emptyList(), null)
    private val incline = Exercise(2, "Incline Bench Press", "", "Chest", listOf("Chest"), listOf("Barbell"), null)

    private fun show(state: ExercisePickerUiState) {
        composeRule.setContent {
            RepTrackTheme {
                ExercisePickerScreen(
                    state = state,
                    onBack = {},
                    onQueryChange = { typed = it },
                    onPick = { picked = it },
                )
            }
        }
    }

    @Test
    fun swapping_showsTheSameMusclesFirst() {
        show(ExercisePickerUiState(isLoading = false, routineName = "Upper body", replacing = "Bench press", sameMuscles = listOf(dips), results = listOf(incline)))

        composeRule.onNodeWithTag("picker_title").assertTextEquals("Swap Bench press")
        composeRule.onNodeWithText("Same muscles", ignoreCase = true).assertIsDisplayed()
        composeRule.onNodeWithText("Chest, Triceps · Bodyweight").assertIsDisplayed()
        composeRule.onNodeWithTag("picker_exercise_3").performClick()

        assertEquals("Dips", picked)
    }

    @Test
    fun adding_offersTheTypedName_asTheUsersOwn() {
        show(ExercisePickerUiState(isLoading = false, routineName = "Upper body", query = "Face pull", customName = "Face pull"))

        composeRule.onNodeWithTag("picker_title").assertTextEquals("Add to Upper body")
        composeRule.onNodeWithTag("picker_custom").performClick()

        assertEquals("Face pull", picked)
    }

    @Test
    fun typing_reportsTheSearch() {
        show(ExercisePickerUiState(isLoading = false, routineName = "Upper body"))

        composeRule.onNodeWithTag("picker_search").performTextInput("row")

        assertEquals("row", typed)
    }

    @Test
    fun withoutTheCatalog_saysHowToGetIt() {
        show(ExercisePickerUiState(isLoading = false, routineName = "Upper body", catalogEmpty = true))
        composeRule.onNodeWithTag("picker_catalog_empty").assertIsDisplayed()
    }
}

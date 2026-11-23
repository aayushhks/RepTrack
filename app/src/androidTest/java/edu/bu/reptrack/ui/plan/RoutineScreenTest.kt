package edu.bu.reptrack.ui.plan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.ui.theme.RepTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for a routine and its exercise menu, drawn from a fixed state. */
@RunWith(AndroidJUnit4::class)
class RoutineScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var started = false
    private var added = false
    private var swapped: Int? = null
    private var removed: Int? = null
    private var movedDown: Int? = null
    private var reset = false
    private var howTo: String? = null

    private val bench = Exercise(73, "Bench Press", "", "Chest", listOf("Chest", "Triceps"), listOf("Barbell"), null)

    private fun state(isEdited: Boolean = false, exercises: List<RoutineExerciseUi> = listOf(
        RoutineExerciseUi("Bench press", bench),
        RoutineExerciseUi("Barbell row", null),
    )) = RoutineUiState(isLoading = false, routineId = "upper", name = "Upper body", exercises = exercises, isEdited = isEdited)

    private fun show(state: RoutineUiState) {
        composeRule.setContent {
            RepTrackTheme {
                RoutineScreen(
                    state = state,
                    onBack = {},
                    onStart = { started = true },
                    onAdd = { added = true },
                    onSwap = { swapped = it },
                    onRemove = { removed = it },
                    onMoveUp = {},
                    onMoveDown = { movedDown = it },
                    onReset = { reset = true },
                    onHowTo = { howTo = it },
                )
            }
        }
    }

    @Test
    fun showsTheExercises_withTheirCatalogDetails() {
        show(state())

        composeRule.onNodeWithTag("routine_name").assertTextEquals("UPPER BODY")
        composeRule.onNodeWithText("Chest, Triceps · Barbell").assertIsDisplayed()
        composeRule.onNodeWithText("Not in the exercise catalog").assertIsDisplayed()
    }

    @Test
    fun tappingAnExercise_opensHowToDoIt_evenWhenTheCatalogDoesNotHaveIt() {
        show(state())

        composeRule.onNodeWithTag("routine_exercise_1").performClick()
        assertEquals("Bench press", howTo)
        composeRule.onNodeWithTag("routine_exercise_2").performClick()
        assertEquals("Barbell row", howTo)
    }

    @Test
    fun menu_swapsMovesAndRemoves() {
        show(state())

        composeRule.onNodeWithTag("routine_menu_1").performClick()
        composeRule.onNodeWithTag("routine_swap_1").performClick()
        assertEquals(0, swapped)

        composeRule.onNodeWithTag("routine_menu_1").performClick()
        composeRule.onNodeWithTag("routine_down_1").performClick()
        assertEquals(0, movedDown)

        composeRule.onNodeWithTag("routine_menu_2").performClick()
        composeRule.onNodeWithTag("routine_remove_2").performClick()
        assertEquals(1, removed)
    }

    @Test
    fun theFirstExercise_cannotMoveUp_andTheLastOneCannotBeRemoved() {
        show(state(exercises = listOf(RoutineExerciseUi("Plank", null))))

        composeRule.onNodeWithTag("routine_menu_1").performClick()
        composeRule.onNodeWithTag("routine_up_1").assertIsNotEnabled()
        composeRule.onNodeWithTag("routine_remove_1").assertIsNotEnabled()
    }

    @Test
    fun startAddAndReset() {
        show(state(isEdited = true))

        composeRule.onNodeWithTag("routine_start").performClick()
        composeRule.onNodeWithTag("routine_add").performScrollTo().performClick()
        composeRule.onNodeWithTag("routine_reset").performScrollTo().performClick()

        assertTrue(started)
        assertTrue(added)
        assertTrue(reset)
    }

    @Test
    fun unchangedRoutine_hasNoReset() {
        show(state())
        composeRule.onNodeWithTag("routine_reset").assertDoesNotExist()
    }

    @Test
    fun emptyRoutine_saysSo_andCannotStart() {
        show(RoutineUiState(isLoading = false, routineId = "custom_1", name = "Push day", exercises = emptyList()))

        composeRule.onNodeWithTag("routine_empty").assertIsDisplayed()
        composeRule.onNodeWithTag("routine_start").assertIsNotEnabled()
        composeRule.onNodeWithTag("routine_add").performClick()
        assertTrue(added)
    }
}

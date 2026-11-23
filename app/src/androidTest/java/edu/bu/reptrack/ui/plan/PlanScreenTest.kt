package edu.bu.reptrack.ui.plan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.StarterPlan
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.workout.PlanDay
import edu.bu.reptrack.domain.workout.PlanDayStatus
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.domain.workout.WorkoutTemplate
import edu.bu.reptrack.ui.theme.RepTrackTheme
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the week plan, drawn from a fixed state. */
@RunWith(AndroidJUnit4::class)
class PlanScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var toggled: DayOfWeek? = null
    private var resetDays = false
    private var opened: String? = null
    private var backed = false

    private val plan = StarterPlan.suggest(FitnessGoal.LOSE_WEIGHT, TrainingPlace.GYM)
    private val monday = LocalDate.parse("2026-11-23")

    /** Full body, Monday, Wednesday and Friday. Monday done, Wednesday today, Friday planned. */
    private fun state(customDays: Boolean = false) = PlanUiState(
        isLoading = false,
        plan = plan,
        trainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        customDays = customDays,
        week = (0L until 7L).map { offset ->
            val date = monday.plusDays(offset)
            when (offset) {
                0L -> PlanDay(date, false, true, "full_body_a", "Full body A", PlanDayStatus.DONE)
                2L -> PlanDay(date, true, true, "full_body_b", "Full body B", PlanDayStatus.TODAY)
                4L -> PlanDay(date, false, true, "full_body_a", "Full body A", PlanDayStatus.PLANNED)
                else -> PlanDay(date, false, false, null, null, PlanDayStatus.REST)
            }
        },
        routines = StarterRoutines.forPlan(plan.type),
    )

    private fun show(state: PlanUiState) {
        composeRule.setContent {
            RepTrackTheme {
                PlanScreen(
                    state = state,
                    onBack = { backed = true },
                    onToggleDay = { toggled = it },
                    onResetDays = { resetDays = true },
                    onOpenRoutine = { opened = it },
                )
            }
        }
    }

    @Test
    fun showsTheDays_andTheWeek() {
        show(state())

        composeRule.onNodeWithText("Full body, 3 days a week").assertIsDisplayed()
        composeRule.onNodeWithTag("plan_day_monday").assertIsSelected()
        composeRule.onNodeWithTag("plan_day_tuesday").assertIsNotSelected()
        composeRule.onNodeWithTag("plan_day_row_wednesday").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Today", substring = true).assertIsDisplayed()
    }

    @Test
    fun tappingADay_reportsIt() {
        show(state())

        composeRule.onNodeWithTag("plan_day_saturday").performClick()

        assertEquals(DayOfWeek.SATURDAY, toggled)
    }

    @Test
    fun tappingAPlannedDay_opensItsRoutine() {
        show(state())

        composeRule.onNodeWithTag("plan_day_row_friday").performScrollTo().performClick()

        assertEquals("full_body_a", opened)
    }

    @Test
    fun routineCards_openTheRoutine() {
        show(state())

        composeRule.onNodeWithTag("plan_routine_full_body_b").performScrollTo().performClick()

        assertEquals("full_body_b", opened)
    }

    @Test
    fun ownDays_canGoBackToThePlansDays() {
        show(state(customDays = true))

        composeRule.onNodeWithTag("plan_reset_days").performClick()

        assertTrue(resetDays)
    }

    @Test
    fun back_reportsTheTap() {
        show(state())
        composeRule.onNodeWithTag("plan_back").performClick()
        assertTrue(backed)
    }

    // ---- The user's own plan ----

    private var fromSuggested = false
    private var fromScratch: String? = null
    private var added: String? = null
    private var renamed: Pair<String, String>? = null
    private var deleted: String? = null
    private var dayPicked: Pair<DayOfWeek, String?>? = null
    private var backToSuggested = false

    private val push = WorkoutTemplate("custom_1", "Push", listOf("Bench press"))
    private val legs = WorkoutTemplate("custom_2", "Legs", listOf("Squat"))

    /** Push on Monday and Friday, Legs on Wednesday. */
    private fun ownState() = state().copy(
        isCustom = true,
        routines = listOf(push, legs),
        schedule = mapOf(DayOfWeek.MONDAY to "custom_1", DayOfWeek.WEDNESDAY to "custom_2", DayOfWeek.FRIDAY to "custom_1"),
    )

    private fun showAll(state: PlanUiState) {
        composeRule.setContent {
            RepTrackTheme {
                PlanScreen(
                    state = state,
                    onBack = {},
                    onToggleDay = {},
                    onResetDays = {},
                    onOpenRoutine = { opened = it },
                    onStartFromSuggested = { fromSuggested = true },
                    onStartFromScratch = { fromScratch = it },
                    onAddRoutine = { added = it },
                    onRenameRoutine = { id, name -> renamed = id to name },
                    onDeleteRoutine = { deleted = it },
                    onSetDayRoutine = { day, id -> dayPicked = day to id },
                    onUseSuggestedPlan = { backToSuggested = true },
                )
            }
        }
    }

    @Test
    fun suggestedPlan_canBecomeYourOwn() {
        showAll(state())

        composeRule.onNodeWithTag("plan_start_from_suggested").performScrollTo().performClick()

        assertTrue(fromSuggested)
    }

    @Test
    fun startFromScratch_asksForTheFirstRoutinesName() {
        showAll(state())
        composeRule.onNodeWithTag("plan_start_from_scratch").performScrollTo().performClick()

        composeRule.onNodeWithTag("plan_name_save").assertIsNotEnabled()
        composeRule.onNodeWithTag("plan_name_input").performTextInput("Push day")
        composeRule.onNodeWithTag("plan_name_save").assertIsEnabled().performClick()

        assertEquals("Push day", fromScratch)
        composeRule.onNodeWithTag("plan_name_input").assertDoesNotExist()
    }

    @Test
    fun ownPlan_showsEachDaysRoutine_andTheSuggestedDayPickerIsGone() {
        showAll(ownState())

        composeRule.onNodeWithText("My plan, 3 days a week").assertIsDisplayed()
        composeRule.onNodeWithTag("plan_day_monday").assertDoesNotExist()
        composeRule.onNodeWithTag("plan_make_own").assertDoesNotExist()
        composeRule.onNodeWithTag("plan_schedule_wednesday").assertTextContains("Legs", substring = true)
        composeRule.onNodeWithTag("plan_schedule_tuesday").assertTextContains("Rest", substring = true)
    }

    @Test
    fun ownPlan_aDayCanGetAnotherRoutine_orRest() {
        showAll(ownState())

        composeRule.onNodeWithTag("plan_schedule_tuesday").performScrollTo().performClick()
        composeRule.onNodeWithTag("plan_pick_custom_2").performClick()
        assertEquals(DayOfWeek.TUESDAY to "custom_2", dayPicked)

        composeRule.onNodeWithTag("plan_schedule_monday").performScrollTo().performClick()
        composeRule.onNodeWithTag("plan_pick_rest").performClick()
        assertEquals(DayOfWeek.MONDAY to null, dayPicked)
    }

    @Test
    fun ownPlan_newRoutine() {
        showAll(ownState())

        composeRule.onNodeWithTag("plan_new_routine").performScrollTo().performClick()
        composeRule.onNodeWithTag("plan_name_input").performTextInput("Pull")
        composeRule.onNodeWithTag("plan_name_save").performClick()

        assertEquals("Pull", added)
    }

    @Test
    fun ownPlan_renameStartsWithTheOldName() {
        showAll(ownState())

        composeRule.onNodeWithTag("plan_routine_menu_custom_2").performScrollTo().performClick()
        composeRule.onNodeWithTag("plan_rename_custom_2").performClick()
        composeRule.onNodeWithTag("plan_name_input").assertTextContains("Legs")
        composeRule.onNodeWithTag("plan_name_input").performTextClearance()
        composeRule.onNodeWithTag("plan_name_input").performTextInput("Leg day")
        composeRule.onNodeWithTag("plan_name_save").performClick()

        assertEquals("custom_2" to "Leg day", renamed)
    }

    @Test
    fun ownPlan_deleteAsksFirst() {
        showAll(ownState())

        composeRule.onNodeWithTag("plan_routine_menu_custom_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("plan_delete_custom_1").performClick()
        assertNull(deleted)
        composeRule.onNodeWithTag("plan_delete_confirm").performClick()

        assertEquals("custom_1", deleted)
    }

    @Test
    fun ownPlan_theLastRoutineCannotBeDeleted() {
        showAll(ownState().copy(routines = listOf(push)))

        composeRule.onNodeWithTag("plan_routine_menu_custom_1").performScrollTo().performClick()

        composeRule.onNodeWithTag("plan_delete_custom_1").assertIsNotEnabled()
    }

    @Test
    fun ownPlan_canGoBackToTheSuggestedPlan_afterConfirming() {
        showAll(ownState())

        composeRule.onNodeWithTag("plan_use_suggested").performScrollTo().performClick()
        composeRule.onNodeWithTag("plan_use_suggested_confirm").performClick()

        assertTrue(backToSuggested)
    }
}

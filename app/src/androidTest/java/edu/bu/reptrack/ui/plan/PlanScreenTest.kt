package edu.bu.reptrack.ui.plan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.StarterPlan
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.workout.PlanDay
import edu.bu.reptrack.domain.workout.PlanDayStatus
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.ui.theme.RepTrackTheme
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
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
}

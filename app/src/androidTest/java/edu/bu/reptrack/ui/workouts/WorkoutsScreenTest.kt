package edu.bu.reptrack.ui.workouts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.StarterPlan
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.domain.workout.StarterRoutines
import edu.bu.reptrack.ui.theme.RepTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the Workouts tab, drawn from a fixed state. */
@RunWith(AndroidJUnit4::class)
class WorkoutsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var startedRoutine: String? = null
    private var setUpClicked = false
    private var chosenPlace: TrainingPlace? = null

    private fun show(state: WorkoutsUiState) {
        composeRule.setContent {
            RepTrackTheme {
                WorkoutsScreen(
                    state = state,
                    onPlaceSelected = { chosenPlace = it },
                    onStartWorkout = { startedRoutine = it },
                    onSetUpPlan = { setUpClicked = true },
                )
            }
        }
    }

    private fun stateFor(profile: UserProfile?): WorkoutsUiState {
        val goal = profile?.goal ?: FitnessGoal.STAY_FIT
        val place = profile?.place ?: TrainingPlace.GYM
        val plan = StarterPlan.suggest(goal, place)
        return WorkoutsUiState(
            isLoading = false,
            profile = profile,
            place = place,
            plan = plan,
            routines = StarterRoutines.forPlan(plan.type),
        )
    }

    @Test
    fun withProfile_showsPlanAndWeightGap() {
        show(stateFor(UserProfile(178f, 82f, 76f, FitnessGoal.BUILD_MUSCLE, TrainingPlace.GYM)))

        composeRule.onNodeWithTag("plan_name").assertTextEquals("Upper and lower split")
        composeRule.onNodeWithText("4 days a week").assertIsDisplayed()
        composeRule.onNodeWithText("Goal: Build muscle").assertIsDisplayed()
        composeRule.onNodeWithTag("weight_gap").assertTextEquals("6 kg to lose")
    }

    @Test
    fun startButton_sendsTheRoutineId() {
        show(stateFor(UserProfile(178f, 82f, 76f, FitnessGoal.BUILD_MUSCLE, TrainingPlace.GYM)))

        composeRule.onNodeWithTag("start_lower").performScrollTo().performClick()

        assertEquals("lower", startedRoutine)
    }

    @Test
    fun withoutProfile_offersSetUp() {
        show(stateFor(profile = null))

        composeRule.onNodeWithText("Set up your plan").performClick()

        assertTrue(setUpClicked)
    }

    @Test
    fun tappingHome_reportsThePlace() {
        show(stateFor(UserProfile(178f, 82f, 76f, FitnessGoal.BUILD_MUSCLE, TrainingPlace.GYM)))

        composeRule.onNodeWithText("Home").performClick()

        assertEquals(TrainingPlace.HOME, chosenPlace)
    }
}

package edu.bu.reptrack.ui.howto

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.domain.exercise.ExerciseDemo
import edu.bu.reptrack.ui.theme.RepTrackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the how-to page, drawn from a fixed state. The photo links go nowhere. */
@RunWith(AndroidJUnit4::class)
class HowToScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var wentBack = false
    private var retried = false

    private val squat = ExerciseDemo(
        id = "Barbell_Squat",
        name = "Barbell Squat",
        imageUrls = listOf("https://example.invalid/0.jpg", "https://example.invalid/1.jpg"),
        steps = listOf("Stand under the bar.", "Squat down, then stand back up."),
        primaryMuscles = listOf("quadriceps"),
        equipment = "barbell",
    )

    private fun show(state: HowToUiState) {
        composeRule.setContent {
            RepTrackTheme {
                HowToScreen(state = state, onBack = { wentBack = true }, onRetry = { retried = true })
            }
        }
    }

    @Test
    fun photosAndSteps_areShown() {
        show(HowToUiState(name = "Squat", isLoading = false, demo = squat))

        composeRule.onNodeWithTag("how_to_name").assertIsDisplayed()
        composeRule.onNodeWithTag("demo_photos")
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Photos of Squat, from the start position to the end position")
        composeRule.onNodeWithText("Squat down, then stand back up.").performScrollTo().assertIsDisplayed()
        // Without the catalog, the details come from the photo set.
        composeRule.onNodeWithText("Quadriceps · Barbell").assertIsDisplayed()
    }

    @Test
    fun catalogDetails_winOverThePhotoSet() {
        val catalog = Exercise(2, "Squat", "", "Legs", listOf("Quads", "Glutes"), listOf("Barbell"), null)
        show(HowToUiState(name = "Squat", isLoading = false, demo = squat, catalog = catalog))

        composeRule.onNodeWithText("Quads, Glutes · Barbell").assertIsDisplayed()
    }

    @Test
    fun whileLoading_saysSo() {
        show(HowToUiState(name = "Squat"))
        composeRule.onNodeWithTag("how_to_loading").assertIsDisplayed()
    }

    @Test
    fun noPhotos_saysSo_andShowsTheCatalogText() {
        val catalog = Exercise(9, "Hollow hold", "Lie on your back and lift your legs.", "Abs", listOf("Abs"), emptyList(), null)
        show(HowToUiState(name = "Hollow hold", isLoading = false, catalog = catalog))

        composeRule.onNodeWithTag("how_to_no_photos").assertIsDisplayed()
        composeRule.onNodeWithText("Lie on your back and lift your legs.").assertIsDisplayed()
    }

    @Test
    fun failedDownload_canTryAgain() {
        show(HowToUiState(name = "Squat", isLoading = false, loadFailed = true))

        composeRule.onNodeWithTag("how_to_error").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()
        assertTrue(retried)
    }

    @Test
    fun backButton_goesBack() {
        show(HowToUiState(name = "Squat", isLoading = false, demo = squat))
        composeRule.onNodeWithTag("how_to_back").performClick()
        assertTrue(wentBack)
    }
}

package edu.bu.reptrack.ui.run.story

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.ui.theme.RepTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the share screen, with a plain box where the saved picture would be. */
@RunWith(AndroidJUnit4::class)
class RunStoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var closed = false
    private var picked: StoryStyle? = null
    private var hideEnds: Boolean? = null
    private var instagramTapped = false
    private var moreTapped = false
    private var retried = false

    private val run = RunSummary(id = 4, startedAt = 0, finishedAt = 26 * 60_000L, distanceM = 5_020.0, durationMs = 26 * 60_000L, calories = 410)
    private val ready = RunStoryUiState(isLoading = false, run = run, hasRoute = true, imagePath = "/cache/stories/run_4.png")

    private fun show(state: RunStoryUiState, instagram: Boolean = true) {
        composeRule.setContent {
            RepTrackTheme {
                RunStoryScreen(
                    state = state,
                    instagramAvailable = instagram,
                    onClose = { closed = true },
                    onStyleSelected = { picked = it },
                    onHideEndsChange = { hideEnds = it },
                    onShareInstagram = { instagramTapped = true },
                    onShareMore = { moreTapped = true },
                    onRetry = { retried = true },
                    preview = { _, description, previewModifier ->
                        Box(previewModifier.semantics { contentDescription = description })
                    },
                )
            }
        }
    }

    @Test
    fun readyCard_isShown_andBothShareButtonsWork() {
        show(ready)

        composeRule.onNodeWithContentDescription("Story picture: Morning run, 5.02 km").assertIsDisplayed()
        composeRule.onNodeWithTag("story_instagram").performClick()
        composeRule.onNodeWithTag("story_more").performClick()

        assertTrue(instagramTapped)
        assertTrue(moreTapped)
    }

    @Test
    fun withoutInstagram_thereIsOneShareButton() {
        show(ready, instagram = false)

        composeRule.onNodeWithTag("story_instagram").assertDoesNotExist()
        composeRule.onNodeWithTag("story_share").performClick()

        assertTrue(moreTapped)
    }

    @Test
    fun looksAndHideEnds_reportTheChoice() {
        show(ready)

        composeRule.onNodeWithTag("story_style_map").assertIsSelected()
        composeRule.onNodeWithTag("story_style_lime").performClick()
        composeRule.onNodeWithTag("story_hide_ends").assertIsOn().performClick()

        assertEquals(StoryStyle.LIME, picked)
        assertEquals(false, hideEnds)
    }

    @Test
    fun whileDrawing_saysSo_andSharingWaits() {
        show(ready.copy(isDrawing = true))

        composeRule.onNodeWithTag("story_drawing").assertIsDisplayed()
        composeRule.onNodeWithTag("story_instagram").assertIsNotEnabled()
        composeRule.onNodeWithTag("story_more").assertIsNotEnabled()
    }

    @Test
    fun runWithoutARoute_hasNoMapLook_andNothingToHide() {
        show(ready.copy(hasRoute = false, options = StoryOptions(style = StoryStyle.DARK)))

        composeRule.onNodeWithTag("story_style_map").assertDoesNotExist()
        composeRule.onNodeWithTag("story_style_dark").assertIsSelected()
        composeRule.onNodeWithTag("story_hide_ends").assertDoesNotExist()
    }

    @Test
    fun mapThatCouldNotLoad_isExplained() {
        show(ready.copy(mapUnavailable = true))
        composeRule.onNodeWithTag("story_note").assertIsDisplayed()
    }

    @Test
    fun drawingFailure_offersTryAgain() {
        show(ready.copy(imagePath = null, drawFailed = true))

        composeRule.onNodeWithText("Try again").performClick()

        assertTrue(retried)
    }

    @Test
    fun close_reportsTheTap() {
        show(ready)
        composeRule.onNodeWithTag("story_close").performClick()
        assertTrue(closed)
    }
}

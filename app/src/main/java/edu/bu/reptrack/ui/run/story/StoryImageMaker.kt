package edu.bu.reptrack.ui.run.story

import edu.bu.reptrack.domain.run.DayPart
import edu.bu.reptrack.domain.run.RunDetail

/** The three looks of the story card. */
enum class StoryStyle {
    /** The route over a dark map of where it was. Needs a route and the internet. */
    MAP,

    /** The route glowing on near-black, no map. */
    DARK,

    /** Ink on the app's lime, like the run screen. */
    LIME,
}

/** What the user picked on the share screen. */
data class StoryOptions(
    val style: StoryStyle = StoryStyle.MAP,
    /** Cut the first and last part of the route, so the picture does not show where the user lives. */
    val hideEnds: Boolean = true,
)

/** Everything needed to draw one story card. */
data class StoryRequest(
    val detail: RunDetail,
    val dayPart: DayPart,
    val options: StoryOptions,
)

/** A finished story card, saved as a PNG file in the app's cache. */
data class StoryImage(
    val path: String,
    /** The Map look was asked for but the map could not load, so the card has no map. */
    val mapUnavailable: Boolean = false,
)

/**
 * Draws a run as a 1080 × 1920 story card (Instagram's 9:16 size) and saves it. An interface,
 * so the ViewModel can be tested with a fake that draws nothing.
 */
interface StoryImageMaker {
    /** Throws if the card cannot be drawn or saved. */
    suspend fun make(request: StoryRequest): StoryImage
}

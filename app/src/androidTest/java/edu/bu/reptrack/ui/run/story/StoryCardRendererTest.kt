package edu.bu.reptrack.ui.run.story

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.bu.reptrack.domain.run.RouteFrame
import edu.bu.reptrack.domain.run.RoutePoint
import edu.bu.reptrack.ui.theme.Ink
import edu.bu.reptrack.ui.theme.Lime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Draws real story cards on the device and checks pixels where the background and the route are. */
@RunWith(AndroidJUnit4::class)
class StoryCardRendererTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val renderer = StoryCardRenderer(StoryFonts.load(context))

    private val text = StoryCardText(
        brand = "REPTRACK",
        date = "THU, NOV 19 · 7:42 AM",
        title = "MORNING RUN",
        distanceLabel = "DISTANCE",
        distance = "5.02",
        distanceUnit = "KM",
        stats = listOf("TIME" to "26:04", "PACE" to "5:12 /km", "BURNED" to "410 kcal"),
        mapCredit = "© OpenStreetMap contributors",
    )

    /** A loop along the Charles River. */
    private val route = (0..60).map { i ->
        val t = i / 60.0 * 2 * PI
        RoutePoint(42.355 + 0.006 * sin(t), -71.09 + 0.013 * cos(t))
    }

    private fun frame() = RouteFrame.fit(
        points = route,
        width = StoryCardRenderer.WIDTH,
        height = StoryCardRenderer.HEIGHT,
        box = StoryCardRenderer.ROUTE_BOX,
        pixelRatio = 2.5,
        maxZoom = 17.0,
    )!!

    private fun assertColor(expected: Color, actual: Int) {
        val want = expected.toArgb()
        val close = listOf(16, 8, 0).all { shift -> abs((want shr shift and 0xff) - (actual shr shift and 0xff)) <= 12 }
        assertTrue("expected #%06X, got #%06X".format(want and 0xffffff, actual and 0xffffff), close)
    }

    @Test
    fun card_isInstagramsStorySize() {
        val card = renderer.render(text, StoryStyle.DARK, route, frame(), map = null)
        assertEquals(1_080, card.width)
        assertEquals(1_920, card.height)
    }

    @Test
    fun darkCard_drawsALimeRoute_onNearBlack() {
        val frame = frame()
        val card = renderer.render(text, StoryStyle.DARK, route, frame, map = null)

        val onRoute = frame.project(route[15])
        assertColor(Lime, card.getPixel(onRoute.x.toInt(), onRoute.y.toInt()))
        // Right of the route box and above it: only the dark background.
        val background = card.getPixel(1_070, 470)
        assertTrue(listOf(16, 8, 0).all { (background shr it and 0xff) < 40 })
    }

    @Test
    fun limeCard_isLime_withAnInkRoute() {
        val frame = frame()
        val card = renderer.render(text, StoryStyle.LIME, route, frame, map = null)

        assertColor(Lime, card.getPixel(1_070, 470))
        val onRoute = frame.project(route[15])
        assertColor(Ink, card.getPixel(onRoute.x.toInt(), onRoute.y.toInt()))
    }

    @Test
    fun mapCard_drawsTheMapPictureBehind() {
        val magenta = Color(0xFFC800C8)
        val map = Bitmap.createBitmap(432, 768, Bitmap.Config.ARGB_8888).apply { eraseColor(magenta.toArgb()) }
        val card = renderer.render(text, StoryStyle.MAP, route, frame(), map)

        // Between the two dark fades, and away from the route, the map shows as it is.
        assertColor(magenta, card.getPixel(1_070, 650))
    }

    @Test
    fun runWithoutARoute_stillGetsACard() {
        val card = renderer.render(text, StoryStyle.DARK, emptyList(), frame = null, map = null)
        assertEquals(1_920, card.height)
    }
}

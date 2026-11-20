package edu.bu.reptrack.ui.run.story

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.run.PixelBox
import edu.bu.reptrack.domain.run.RouteFrame
import edu.bu.reptrack.domain.run.RoutePoint
import edu.bu.reptrack.ui.theme.Chalk
import edu.bu.reptrack.ui.theme.Ember
import edu.bu.reptrack.ui.theme.Fog
import edu.bu.reptrack.ui.theme.Ink
import edu.bu.reptrack.ui.theme.Lime
import edu.bu.reptrack.ui.theme.Night

/** The words on the card, already in the user's language and in capitals where needed. */
data class StoryCardText(
    val brand: String,
    val date: String,
    val title: String,
    val distanceLabel: String,
    val distance: String,
    val distanceUnit: String,
    /** Label and value: time, pace, then calories (drawn in the accent color). */
    val stats: List<Pair<String, String>>,
    val mapCredit: String,
)

/** The app's two fonts, loaded from res/font. */
class StoryFonts(val condensedBold: Typeface, val body: Typeface, val bodySemiBold: Typeface) {
    companion object {
        fun load(context: Context) = StoryFonts(
            condensedBold = ResourcesCompat.getFont(context, R.font.barlow_condensed_bold) ?: Typeface.DEFAULT_BOLD,
            body = ResourcesCompat.getFont(context, R.font.archivo_regular) ?: Typeface.DEFAULT,
            bodySemiBold = ResourcesCompat.getFont(context, R.font.archivo_semibold) ?: Typeface.DEFAULT_BOLD,
        )
    }
}

/**
 * Draws the story card with android.graphics.Canvas: 1080 × 1920 pixels, Instagram's 9:16
 * story size. The name and date sit at the top, the route in the middle, the distance and
 * stats below. Everything that matters stays between y = 250 and y = 1700, clear of the bars
 * Instagram puts over the top and bottom of a story.
 */
class StoryCardRenderer(private val fonts: StoryFonts) {

    private class Palette(
        val text: Int,
        val subText: Int,
        val accent: Int,
        val route: Int,
        /** Null: no glow, a flat line (the lime look). */
        val glow: Int?,
        val calories: Int,
        val startFill: Int,
        val dotEdge: Int,
    )

    /**
     * [frame] says where the route goes; with [map], it is the same frame the map was taken
     * with, so the line sits exactly on the streets.
     */
    fun render(
        text: StoryCardText,
        style: StoryStyle,
        route: List<RoutePoint>,
        frame: RouteFrame?,
        map: Bitmap?,
    ): Bitmap {
        val card = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(card)
        val palette = paletteFor(style)
        drawBackground(canvas, style, map)
        if (frame != null && route.size >= 2) drawRoute(canvas, route, frame, palette, flat = style == StoryStyle.LIME)
        drawHeader(canvas, text, palette)
        drawStats(canvas, text, palette)
        if (style == StoryStyle.MAP && map != null) {
            canvas.drawText(text.mapCredit, MARGIN, 1_850f, textPaint(fonts.body, 24f, Fog.toArgb()).fitTo(text.mapCredit, CONTENT_WIDTH, 16f))
        }
        return card
    }

    private fun paletteFor(style: StoryStyle): Palette = when (style) {
        StoryStyle.LIME -> Palette(
            text = Ink.toArgb(),
            subText = Ink.copy(alpha = 0.72f).toArgb(),
            accent = Ink.toArgb(),
            route = Ink.toArgb(),
            glow = null,
            calories = Ink.toArgb(),
            startFill = Ink.toArgb(),
            dotEdge = Lime.toArgb(),
        )
        StoryStyle.MAP, StoryStyle.DARK -> Palette(
            text = Chalk.toArgb(),
            subText = Fog.toArgb(),
            accent = Lime.toArgb(),
            route = Lime.toArgb(),
            glow = Lime.copy(alpha = 0.55f).toArgb(),
            calories = Ember.toArgb(),
            startFill = Chalk.toArgb(),
            dotEdge = Ink.toArgb(),
        )
    }

    private fun drawBackground(canvas: Canvas, style: StoryStyle, map: Bitmap?) {
        when {
            style == StoryStyle.LIME -> canvas.drawColor(Lime.toArgb())
            style == StoryStyle.MAP && map != null -> {
                canvas.drawBitmap(map, null, Rect(0, 0, WIDTH, HEIGHT), Paint(Paint.FILTER_BITMAP_FLAG))
                // Dark fades at the top and bottom, so the words read on any map.
                canvas.drawRect(0f, 0f, WIDTH.toFloat(), 620f, fade(0f, 620f, Night, 0.92f, 0.55f, 0f))
                canvas.drawRect(0f, 1_000f, WIDTH.toFloat(), HEIGHT.toFloat(), fade(1_000f, HEIGHT.toFloat(), Night, 0f, 0.82f, 0.96f))
            }
            else -> {
                canvas.drawColor(Night.toArgb())
                // A soft lime glow behind the route.
                val glow = Paint().apply {
                    shader = RadialGradient(
                        ROUTE_BOX.centerX,
                        ROUTE_BOX.centerY,
                        640f,
                        Lime.copy(alpha = 0.16f).toArgb(),
                        Color.Transparent.toArgb(),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), glow)
            }
        }
    }

    /** A vertical fade in [color], from the first alpha at [top] through the middle one to the last at [bottom]. */
    private fun fade(top: Float, bottom: Float, color: Color, vararg alphas: Float) = Paint().apply {
        shader = LinearGradient(
            0f,
            top,
            0f,
            bottom,
            alphas.map { color.copy(alpha = it).toArgb() }.toIntArray(),
            null,
            Shader.TileMode.CLAMP,
        )
    }

    private fun drawRoute(canvas: Canvas, route: List<RoutePoint>, frame: RouteFrame, palette: Palette, flat: Boolean) {
        val path = Path()
        route.forEachIndexed { index, point ->
            val spot = frame.project(point)
            if (index == 0) path.moveTo(spot.x, spot.y) else path.lineTo(spot.x, spot.y)
        }
        palette.glow?.let { glow ->
            canvas.drawPath(path, linePaint(glow, 40f).apply { maskFilter = BlurMaskFilter(22f, BlurMaskFilter.Blur.NORMAL) })
        }
        canvas.drawPath(path, linePaint(palette.route, if (flat) 16f else 14f))

        val start = frame.project(route.first())
        val end = frame.project(route.last())
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        // Start: a white dot. Finish: a bigger dot in the route color.
        canvas.drawCircle(start.x, start.y, 18f, fill.apply { color = palette.startFill })
        canvas.drawCircle(start.x, start.y, 18f, edge.apply { color = palette.dotEdge; strokeWidth = 6f })
        canvas.drawCircle(end.x, end.y, 22f, fill.apply { color = palette.route })
        canvas.drawCircle(end.x, end.y, 22f, edge.apply { color = palette.dotEdge; strokeWidth = 8f })
    }

    private fun linePaint(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun drawHeader(canvas: Canvas, text: StoryCardText, palette: Palette) {
        val brand = textPaint(fonts.condensedBold, 54f, palette.accent, letterSpacing = 0.06f)
        canvas.drawText(text.brand, MARGIN, 300f, brand)
        val dateRoom = CONTENT_WIDTH - brand.measureText(text.brand) - 40f
        val date = textPaint(fonts.bodySemiBold, 30f, palette.subText, letterSpacing = 0.08f, align = Paint.Align.RIGHT)
            .fitTo(text.date, dateRoom, 18f)
        canvas.drawText(text.date, WIDTH - MARGIN, 298f, date)
        canvas.drawText(text.title, MARGIN, 425f, textPaint(fonts.condensedBold, 118f, palette.text).fitTo(text.title, CONTENT_WIDTH, 56f))
    }

    private fun drawStats(canvas: Canvas, text: StoryCardText, palette: Palette) {
        canvas.drawText(text.distanceLabel, MARGIN, 1_250f, overline(palette))

        val number = textPaint(fonts.condensedBold, 260f, palette.text).fitTo(text.distance, 720f, 120f)
        canvas.drawText(text.distance, MARGIN - 8f, 1_478f, number)
        val unitX = MARGIN - 8f + number.measureText(text.distance) + 18f
        canvas.drawText(text.distanceUnit, unitX, 1_478f, textPaint(fonts.condensedBold, 96f, palette.accent, letterSpacing = 0.02f))

        text.stats.forEachIndexed { index, (label, value) ->
            val x = MARGIN + index * STAT_COLUMN
            val color = if (index == text.stats.lastIndex) palette.calories else palette.text
            canvas.drawText(label, x, 1_560f, overline(palette).fitTo(label, STAT_COLUMN - 20f, 18f))
            canvas.drawText(value, x, 1_640f, textPaint(fonts.condensedBold, 80f, color).fitTo(value, STAT_COLUMN - 20f, 40f))
        }
    }

    private fun overline(palette: Palette) = textPaint(fonts.bodySemiBold, 30f, palette.subText, letterSpacing = 0.16f)

    private fun textPaint(
        typeface: Typeface,
        size: Float,
        color: Int,
        letterSpacing: Float = 0f,
        align: Paint.Align = Paint.Align.LEFT,
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = size
        this.color = color
        this.letterSpacing = letterSpacing
        textAlign = align
    }

    /** Makes the text smaller until it fits [maxWidth], for long names in other languages. */
    private fun Paint.fitTo(text: String, maxWidth: Float, minSize: Float): Paint {
        while (measureText(text) > maxWidth && textSize > minSize) textSize -= 2f
        return this
    }

    companion object {
        const val WIDTH = 1_080
        const val HEIGHT = 1_920
        private const val MARGIN = 80f
        private const val CONTENT_WIDTH = WIDTH - 2 * MARGIN
        private const val STAT_COLUMN = 320f

        /** Where the route is fitted, between the name and the distance. */
        val ROUTE_BOX = PixelBox(left = MARGIN, top = 480f, right = WIDTH - MARGIN, bottom = 1_150f)
    }
}

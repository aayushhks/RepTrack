package edu.bu.reptrack.ui.run.story

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.run.RouteFrame
import edu.bu.reptrack.domain.run.RouteShape
import edu.bu.reptrack.domain.run.RunFormat
import edu.bu.reptrack.ui.run.MapStyles
import edu.bu.reptrack.ui.run.formatRunStart
import edu.bu.reptrack.ui.run.runName
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter

/**
 * The real [StoryImageMaker]: trims the route if asked, takes a picture of the map behind
 * it, draws the card and saves it as a PNG in the app's cache, in the folder the FileProvider
 * shares (res/xml/share_paths.xml).
 */
class AndroidStoryImageMaker(context: Context) : StoryImageMaker {
    private val appContext = context.applicationContext

    override suspend fun make(request: StoryRequest): StoryImage {
        val detail = request.detail
        val route = if (request.options.hideEnds) RouteShape.hideEnds(detail.route) else detail.route
        val frame = if (route.size >= 2) {
            RouteFrame.fit(
                points = route,
                width = StoryCardRenderer.WIDTH,
                height = StoryCardRenderer.HEIGHT,
                box = StoryCardRenderer.ROUTE_BOX,
                pixelRatio = MAP_PIXEL_RATIO,
                maxZoom = MapStyles.MAX_ZOOM,
            )
        } else {
            null
        }
        val wantsMap = request.options.style == StoryStyle.MAP && frame != null
        val map = if (wantsMap) MapPictures.take(appContext, frame, MAP_PIXEL_RATIO, MapStyles.DARK) else null

        return withContext(Dispatchers.Default) {
            val card = StoryCardRenderer(StoryFonts.load(appContext))
                .render(cardText(request), request.options.style, route, frame, map)
            val file = withContext(Dispatchers.IO) { save(card, detail.summary.id) }
            card.recycle()
            StoryImage(path = file.absolutePath, mapUnavailable = wantsMap && map == null)
        }
    }

    private fun cardText(request: StoryRequest): StoryCardText {
        val run = request.detail.summary
        val locale = Locale.getDefault()
        fun text(id: Int) = appContext.getString(id).uppercase(locale)
        return StoryCardText(
            brand = text(R.string.story_brand),
            date = formatRunStart(run.startedAt).uppercase(locale),
            title = appContext.runName(request.dayPart, run.isWalk).uppercase(locale),
            distanceLabel = text(R.string.run_detail_distance),
            distance = RunFormat.km(run.distanceM),
            distanceUnit = text(R.string.run_detail_km),
            stats = listOf(
                text(R.string.run_time) to RunFormat.duration(run.durationMs),
                text(R.string.run_pace) to appContext.getString(R.string.run_pace_value, RunFormat.pace(run.paceSecondsPerKm)),
                text(R.string.progress_week_calories) to appContext.getString(R.string.run_calories_value, run.calories),
            ),
            mapCredit = appContext.getString(R.string.story_map_credit),
        )
    }

    private fun save(card: Bitmap, runId: Long): File {
        val folder = File(appContext.cacheDir, STORY_FOLDER).apply { mkdirs() }
        // Keep only the newest pictures: one may still be open in the app it was shared to.
        folder.listFiles()?.sortedByDescending { it.lastModified() }?.drop(KEEP_FILES - 1)?.forEach { it.delete() }
        // A new name each time, so the preview and the sharing apps never read a half-written file.
        val file = File(folder, "run_${runId}_${System.currentTimeMillis()}.png")
        file.outputStream().use { out ->
            check(card.compress(Bitmap.CompressFormat.PNG, 100, out)) { "The story picture could not be saved" }
        }
        return file
    }

    companion object {
        /** Must match the path in res/xml/share_paths.xml. */
        const val STORY_FOLDER = "stories"
        private const val KEEP_FILES = 3

        /** Each map pixel is 2.5 card pixels, so street names read like on a phone screen. */
        private const val MAP_PIXEL_RATIO = 2.5
    }
}

/** Takes a picture of a map with MapLibre's MapSnapshotter, without showing a map on screen. */
private object MapPictures {
    /** Offline the map never finishes, so the card is drawn without it after this long. */
    private const val TIMEOUT_MS = 10_000L

    /**
     * A picture of the map at exactly the camera of [frame], or null if it could not load. The
     * snapshotter has to be used on the main thread.
     */
    suspend fun take(context: Context, frame: RouteFrame, pixelRatio: Double, styleUri: String): Bitmap? =
        withContext(Dispatchers.Main) {
            withTimeoutOrNull(TIMEOUT_MS) {
                suspendCancellableCoroutine<Bitmap?> { continuation ->
                    MapLibre.getInstance(context)
                    val camera = CameraPosition.Builder()
                        .target(LatLng(frame.centerLatitude, frame.centerLongitude))
                        .zoom(frame.zoom(pixelRatio))
                        .build()
                    val options = MapSnapshotter.Options(
                        (frame.width / pixelRatio).roundToInt(),
                        (frame.height / pixelRatio).roundToInt(),
                    )
                        .withPixelRatio(pixelRatio.toFloat())
                        .withStyleBuilder(Style.Builder().fromUri(styleUri))
                        .withCameraPosition(camera)
                        .withLogo(false)
                        // The card prints the credit itself, in its own font.
                        .withAttribution(false)
                    val snapshotter = MapSnapshotter(context, options)
                    val mainThread = Handler(Looper.getMainLooper())
                    continuation.invokeOnCancellation { mainThread.post { snapshotter.cancel() } }
                    snapshotter.start(
                        { snapshot -> if (continuation.isActive) continuation.resume(snapshot.bitmap) },
                        { _ -> if (continuation.isActive) continuation.resume(null) },
                    )
                }
            }
        }
}

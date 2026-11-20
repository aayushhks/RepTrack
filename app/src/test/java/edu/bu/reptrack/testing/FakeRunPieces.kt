package edu.bu.reptrack.testing

import edu.bu.reptrack.data.location.LocationTracker
import edu.bu.reptrack.data.run.RunRepository
import edu.bu.reptrack.domain.run.GeoPoint
import edu.bu.reptrack.domain.run.RoutePoint
import edu.bu.reptrack.domain.run.RunDetail
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.service.RunServiceController
import edu.bu.reptrack.ui.run.story.StoryImage
import edu.bu.reptrack.ui.run.story.StoryImageMaker
import edu.bu.reptrack.ui.run.story.StoryRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/** A [LocationTracker] that tests drive by hand with [send] and [fail]. */
class FakeLocationTracker : LocationTracker {
    private val points = MutableSharedFlow<GeoPoint>(extraBufferCapacity = 64)
    private var failWith: Throwable? = null

    /** True while someone is collecting, like real GPS being on. */
    var isListening = false
        private set

    override fun locations(): Flow<GeoPoint> = points
        .onStart {
            failWith?.let { throw it }
            isListening = true
        }
        .onCompletion { isListening = false }

    fun send(point: GeoPoint) {
        check(points.tryEmit(point))
    }

    /** The next run gets this error as soon as it starts listening. */
    fun failNextWith(error: Throwable) {
        failWith = error
    }
}

/** In-memory [RunRepository] for tests. */
class FakeRunRepository : RunRepository {
    private val runs = MutableStateFlow<List<RunSummary>>(emptyList())
    private val routes = MutableStateFlow<Map<Long, List<RoutePoint>>>(emptyMap())
    private var nextId = 1L

    val saved: List<RunSummary> get() = runs.value

    /** The route saved with each run, by run id. */
    val savedRoutes: Map<Long, List<RoutePoint>> get() = routes.value

    /** Makes saving throw, like a full disk, to test the error state. */
    var failSaves = false

    /** Makes reading one run throw, like a broken database, to test the error state. */
    var failReads = false

    override suspend fun saveRun(run: RunSummary, route: List<RoutePoint>): Long {
        if (failSaves) throw java.io.IOException("disk full")
        val id = nextId++
        routes.value = routes.value + (id to route)
        runs.value = (runs.value + run.copy(id = id)).sortedByDescending { it.finishedAt }
        return id
    }

    override fun observeRuns(): Flow<List<RunSummary>> = runs

    override fun observeRun(runId: Long): Flow<RunDetail?> = combine(runs, routes) { list, routeMap ->
        if (failReads) throw java.io.IOException("database broken")
        list.firstOrNull { it.id == runId }?.let { RunDetail(it, routeMap[runId].orEmpty()) }
    }
}

/** A [StoryImageMaker] that remembers what it was asked to draw and returns a made-up path. */
class FakeStoryImageMaker : StoryImageMaker {
    val requests = mutableListOf<StoryRequest>()

    /** Makes drawing throw, to test the error state. */
    var fail = false

    /** Pretends the map could not load. */
    var mapUnavailable = false

    /** Holds every drawing until [finish] is called, to see the "drawing" state. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun make(request: StoryRequest): StoryImage {
        requests += request
        gate?.await()
        if (fail) throw java.io.IOException("out of memory")
        return StoryImage(path = "/cache/stories/story_${requests.size}.png", mapUnavailable = mapUnavailable)
    }

    fun finish() {
        gate?.complete(Unit)
        gate = null
    }
}

/** Counts service starts instead of starting a real Android service. */
class FakeRunServiceController : RunServiceController {
    var starts = 0
        private set

    override fun start() {
        starts++
    }
}

/** A fix at a point some meters north of a fixed start. 0.001 degrees of latitude is about 111 m. */
fun pointNorth(meters: Double, timeMillis: Long, accuracyM: Float = 5f) = GeoPoint(
    latitude = 42.35 + meters / 111_195.0,
    longitude = -71.10,
    accuracyM = accuracyM,
    timeMillis = timeMillis,
)

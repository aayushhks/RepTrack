package edu.bu.reptrack.data.run

import edu.bu.reptrack.testing.FakeLocationTracker
import edu.bu.reptrack.testing.pointNorth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RunRecorderTest {

    private val tracker = FakeLocationTracker()
    private var clock = 1_000L

    // The recorder runs in the test's background scope, like the app-wide scope in the app.
    private fun TestScope.newRecorder() =
        RunRecorder(tracker, backgroundScope + UnconfinedTestDispatcher(testScheduler)) { clock }

    @Test
    fun nothingIsRecorded_untilStart() = runTest {
        val recorder = newRecorder()
        assertNull(recorder.active.value)
        assertFalse(tracker.isListening)
    }

    @Test
    fun start_listensAndAddsUpTheFixes() = runTest {
        val recorder = newRecorder()
        recorder.start()

        assertTrue(tracker.isListening)
        assertEquals(1_000L, recorder.active.value?.startedAt)

        tracker.send(pointNorth(0.0, 0))
        tracker.send(pointNorth(30.0, 10_000))

        assertEquals(30.0, recorder.active.value!!.progress.distanceM, 0.2)
    }

    @Test
    fun startingTwice_keepsTheFirstRun() = runTest {
        val recorder = newRecorder()
        recorder.start()
        clock = 9_000L
        recorder.start()

        assertEquals(1_000L, recorder.active.value?.startedAt)
    }

    @Test
    fun stop_returnsTheRun_andTurnsLocationOff() = runTest {
        val recorder = newRecorder()
        recorder.start()
        tracker.send(pointNorth(0.0, 0))
        tracker.send(pointNorth(40.0, 12_000))

        val run = recorder.stop()

        assertNotNull(run)
        assertEquals(40.0, run!!.progress.distanceM, 0.2)
        assertNull(recorder.active.value)
        assertFalse(tracker.isListening)
    }

    @Test
    fun locationError_isReported_andTheRunIsKept() = runTest {
        tracker.failNextWith(SecurityException("permission removed"))
        val recorder = newRecorder()
        recorder.start()

        assertTrue(recorder.active.value!!.locationFailed)
    }
}

package edu.bu.reptrack.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import edu.bu.reptrack.domain.run.GeoPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * [LocationTracker] backed by Google Play services' fused location provider, which mixes
 * GPS, Wi-Fi and cell signals. High accuracy, a fix about every 2 seconds, only while collected.
 */
class FusedLocationTracker(context: Context) : LocationTracker {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    // The run screen asks for the permission before a run starts. If it was taken away since,
    // requestLocationUpdates throws a SecurityException and the Flow ends with that error.
    @SuppressLint("MissingPermission")
    override fun locations(): Flow<GeoPoint> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations
                    // The first result can be an old cached place. Only fixes from the last few seconds count.
                    .filter { it.ageMillis() <= MAX_FIX_AGE_MS }
                    .forEach { trySend(it.toGeoPoint()) }
            }
        }
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { error -> close(error) }
        } catch (error: SecurityException) {
            close(error)
        }
        // Runs when the run ends: turns GPS off so it does not drain the battery.
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private companion object {
        const val UPDATE_INTERVAL_MS = 2_000L
        const val FASTEST_INTERVAL_MS = 1_000L
        const val MAX_FIX_AGE_MS = 10_000L
    }
}

private const val NANOS_PER_MILLI = 1_000_000L

/** How old the fix is, measured on the since-boot clock so a wrong phone time cannot fool it. */
private fun Location.ageMillis(): Long =
    (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / NANOS_PER_MILLI

// Real fixes always carry an accuracy. Mock fixes from the emulator may not, so those are trusted.
private fun Location.toGeoPoint() = GeoPoint(
    latitude = latitude,
    longitude = longitude,
    accuracyM = if (hasAccuracy()) accuracy else 0f,
    timeMillis = elapsedRealtimeNanos / NANOS_PER_MILLI,
)

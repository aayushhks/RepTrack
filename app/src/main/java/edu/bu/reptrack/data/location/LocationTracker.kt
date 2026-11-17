package edu.bu.reptrack.data.location

import edu.bu.reptrack.domain.run.GeoPoint
import kotlinx.coroutines.flow.Flow

/** The phone's location as a Flow. An interface, so the run recorder can be tested with fake fixes. */
interface LocationTracker {
    /** Starts location updates when collected and stops them when the collector stops. */
    fun locations(): Flow<GeoPoint>
}

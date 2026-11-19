package edu.bu.reptrack.data.weight

import edu.bu.reptrack.domain.weight.WeightEntry
import kotlinx.coroutines.flow.Flow

/** Saves and reads weigh-ins. Screens talk to this, not to Room directly. */
interface WeightRepository {
    /** Every weigh-in, oldest first. Emits again whenever one is added or deleted. */
    fun observeWeights(): Flow<List<WeightEntry>>

    /** Saves a weigh-in and returns its id. */
    suspend fun addWeight(weightKg: Float, loggedAt: Long): Long

    suspend fun deleteWeight(id: Long)

    /** The newest weigh-in, or null if there is none. */
    suspend fun latestWeight(): WeightEntry?
}

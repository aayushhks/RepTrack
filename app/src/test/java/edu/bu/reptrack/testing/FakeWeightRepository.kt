package edu.bu.reptrack.testing

import edu.bu.reptrack.data.weight.WeightRepository
import edu.bu.reptrack.domain.weight.WeightEntry
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/** In-memory [WeightRepository] for tests. Same order as Room: oldest first. */
class FakeWeightRepository : WeightRepository {
    private val entries = MutableStateFlow<List<WeightEntry>>(emptyList())
    private var nextId = 1L

    val saved: List<WeightEntry> get() = entries.value

    /** Makes reads or writes throw, like a full or broken database, to test the error states. */
    var failReads = false
    var failWrites = false

    override fun observeWeights(): Flow<List<WeightEntry>> =
        if (failReads) flow { throw IOException("database unavailable") } else entries

    override suspend fun addWeight(weightKg: Float, loggedAt: Long): Long {
        if (failWrites) throw IOException("disk full")
        val id = nextId++
        entries.value = (entries.value + WeightEntry(id, loggedAt, weightKg))
            .sortedWith(compareBy({ it.loggedAt }, { it.id }))
        return id
    }

    override suspend fun deleteWeight(id: Long) {
        if (failWrites) throw IOException("disk full")
        entries.value = entries.value.filterNot { it.id == id }
    }

    override suspend fun latestWeight(): WeightEntry? = entries.value.lastOrNull()
}

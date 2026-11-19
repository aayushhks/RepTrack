package edu.bu.reptrack.testing

import edu.bu.reptrack.data.weight.WeightRepository
import edu.bu.reptrack.domain.weight.WeightEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [WeightRepository] for tests. Same order as Room: oldest first. */
class FakeWeightRepository : WeightRepository {
    private val entries = MutableStateFlow<List<WeightEntry>>(emptyList())
    private var nextId = 1L

    val saved: List<WeightEntry> get() = entries.value

    override fun observeWeights(): Flow<List<WeightEntry>> = entries

    override suspend fun addWeight(weightKg: Float, loggedAt: Long): Long {
        val id = nextId++
        entries.value = (entries.value + WeightEntry(id, loggedAt, weightKg))
            .sortedWith(compareBy({ it.loggedAt }, { it.id }))
        return id
    }

    override suspend fun deleteWeight(id: Long) {
        entries.value = entries.value.filterNot { it.id == id }
    }

    override suspend fun latestWeight(): WeightEntry? = entries.value.lastOrNull()
}

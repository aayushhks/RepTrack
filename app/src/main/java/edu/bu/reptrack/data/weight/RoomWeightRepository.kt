package edu.bu.reptrack.data.weight

import edu.bu.reptrack.domain.weight.WeightEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [WeightRepository] backed by Room. */
class RoomWeightRepository(private val dao: WeightDao) : WeightRepository {

    override fun observeWeights(): Flow<List<WeightEntry>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun addWeight(weightKg: Float, loggedAt: Long): Long =
        dao.insert(WeightEntity(loggedAt = loggedAt, weightKg = weightKg))

    override suspend fun deleteWeight(id: Long) = dao.delete(id)

    override suspend fun latestWeight(): WeightEntry? = dao.latest()?.toDomain()
}

private fun WeightEntity.toDomain() = WeightEntry(id = id, loggedAt = loggedAt, weightKg = weightKg)

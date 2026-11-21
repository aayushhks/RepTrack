package edu.bu.reptrack.data.exercise

import edu.bu.reptrack.domain.exercise.Exercise
import edu.bu.reptrack.domain.exercise.htmlToPlainText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [ExerciseRepository] that downloads from wger and keeps a copy in Room, which is the only
 * thing the screens read. Each page is saved as it arrives.
 */
class RoomExerciseRepository(
    private val dao: ExerciseDao,
    private val api: ExerciseApi,
    private val pageSize: Int = PAGE_SIZE,
    private val maxPages: Int = MAX_PAGES,
) : ExerciseRepository {

    override fun observeExercises(): Flow<List<Exercise>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refresh() {
        for (page in 0 until maxPages) {
            val result = api.fetchPage(offset = page * pageSize, limit = pageSize)
            dao.upsertAll(result.results.mapNotNull { it.toEntity() })
            if (result.next == null) return
        }
    }

    companion object {
        // Each page is about 700 KB of JSON, so we keep a few hundred exercises, not all of them.
        const val PAGE_SIZE = 50
        const val MAX_PAGES = 4
    }
}

private const val SEPARATOR = "|"

/** Null when the exercise has no English name, so it would show up blank. */
internal fun WgerExerciseDto.toEntity(): ExerciseEntity? {
    val english = translations.firstOrNull { it.language == WGER_ENGLISH && !it.name.isNullOrBlank() }
        ?: return null
    val image = images.firstOrNull { it.isMain } ?: images.firstOrNull()
    return ExerciseEntity(
        id = id,
        name = english.name.orEmpty().trim(),
        description = htmlToPlainText(english.description.orEmpty()),
        category = category?.name.orEmpty(),
        muscles = muscles.mapNotNull { it.nameEn?.takeIf(String::isNotBlank) ?: it.name?.takeIf(String::isNotBlank) }
            .joinToString(SEPARATOR),
        equipment = equipment.mapNotNull { it.name?.takeIf(String::isNotBlank) }.joinToString(SEPARATOR),
        imageUrl = image?.thumbnails?.small ?: image?.image,
    )
}

private fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    description = description,
    category = category,
    muscles = muscles.split(SEPARATOR).filter { it.isNotEmpty() },
    equipment = equipment.split(SEPARATOR).filter { it.isNotEmpty() },
    imageUrl = imageUrl,
)

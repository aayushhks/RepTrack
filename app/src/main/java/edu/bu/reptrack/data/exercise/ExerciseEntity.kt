package edu.bu.reptrack.data.exercise

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Table row for one cached wger exercise. The id is wger's own, so a refresh replaces the row. */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val category: String,
    /** Muscle names joined with "|". */
    val muscles: String,
    /** Equipment names joined with "|". */
    val equipment: String,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
)

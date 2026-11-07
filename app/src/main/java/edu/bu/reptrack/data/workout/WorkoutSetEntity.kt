package edu.bu.reptrack.data.workout

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Table row for one logged set. Deleting a session deletes its sets (CASCADE).
 * The index on session_id keeps "sets of this session" and the CASCADE fast.
 */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_id"])],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
    @ColumnInfo(name = "set_number") val setNumber: Int,
    @ColumnInfo(name = "weight_kg") val weightKg: Float,
    val reps: Int,
    @ColumnInfo(name = "logged_at") val loggedAt: Long,
)

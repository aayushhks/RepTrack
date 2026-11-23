package edu.bu.reptrack.data.plan

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One exercise of a routine the user changed, at its place in the list. Routines that were never
 * changed have no rows: they use the built-in list.
 */
@Entity(tableName = "routine_exercises", primaryKeys = ["routine_id", "position"])
data class RoutineExerciseEntity(
    @ColumnInfo(name = "routine_id") val routineId: String,
    val position: Int,
    val name: String,
)

/** A day the user trains, 1 = Monday to 7 = Sunday, as in java.time. No rows: the plan's default days. */
@Entity(tableName = "training_days")
data class TrainingDayEntity(
    @PrimaryKey @ColumnInfo(name = "day_of_week") val dayOfWeek: Int,
)

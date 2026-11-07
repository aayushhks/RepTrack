package edu.bu.reptrack.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import edu.bu.reptrack.data.workout.WorkoutDao
import edu.bu.reptrack.data.workout.WorkoutSessionEntity
import edu.bu.reptrack.data.workout.WorkoutSetEntity

/**
 * The app's Room database. Version 1. The schema is exported to app/schemas so future
 * versions can be checked and migrated. If a table changes, raise the version and add a migration.
 */
@Database(
    entities = [WorkoutSessionEntity::class, WorkoutSetEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RepTrackDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        const val FILE_NAME = "reptrack.db"
    }
}

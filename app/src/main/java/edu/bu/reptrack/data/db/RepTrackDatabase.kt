package edu.bu.reptrack.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import edu.bu.reptrack.data.exercise.ExerciseDao
import edu.bu.reptrack.data.exercise.ExerciseEntity
import edu.bu.reptrack.data.plan.CustomRoutineEntity
import edu.bu.reptrack.data.plan.PlanDao
import edu.bu.reptrack.data.plan.RoutineExerciseEntity
import edu.bu.reptrack.data.plan.ScheduleDayEntity
import edu.bu.reptrack.data.plan.TrainingDayEntity
import edu.bu.reptrack.data.run.RunDao
import edu.bu.reptrack.data.run.RunEntity
import edu.bu.reptrack.data.run.RunRouteEntity
import edu.bu.reptrack.data.weight.WeightDao
import edu.bu.reptrack.data.weight.WeightEntity
import edu.bu.reptrack.data.workout.WorkoutDao
import edu.bu.reptrack.data.workout.WorkoutSessionEntity
import edu.bu.reptrack.data.workout.WorkoutSetEntity

/**
 * The app's Room database. The schema of every version is exported to app/schemas, so Room
 * can check and migrate it. If a table changes, raise the version and add a migration.
 *
 * Version 2 adds the runs table. It is an automatic migration: Room compares the saved
 * schemas of version 1 and 2 and creates the new table, keeping every saved workout.
 * Version 3 adds the weights table the same way, version 4 the run_routes table and
 * version 5 the exercises table, the saved copy of the wger catalog, and version 6 the
 * routine_exercises and training_days tables, the user's changes to their plan. Version 7
 * adds custom_routines and plan_schedule, for a plan the user makes themselves.
 */
@Database(
    entities = [
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        RunEntity::class,
        WeightEntity::class,
        RunRouteEntity::class,
        ExerciseEntity::class,
        RoutineExerciseEntity::class,
        TrainingDayEntity::class,
        CustomRoutineEntity::class,
        ScheduleDayEntity::class,
    ],
    version = 7,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
    ],
)
abstract class RepTrackDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    abstract fun runDao(): RunDao

    abstract fun weightDao(): WeightDao

    abstract fun exerciseDao(): ExerciseDao

    abstract fun planDao(): PlanDao

    companion object {
        const val FILE_NAME = "reptrack.db"
    }
}

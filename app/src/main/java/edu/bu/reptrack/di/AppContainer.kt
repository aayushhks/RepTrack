package edu.bu.reptrack.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import edu.bu.reptrack.data.auth.AuthRepository
import edu.bu.reptrack.data.auth.FirebaseAuthRepository
import edu.bu.reptrack.data.db.RepTrackDatabase
import edu.bu.reptrack.data.profile.DataStoreProfileRepository
import edu.bu.reptrack.data.profile.ProfileRepository
import edu.bu.reptrack.data.profile.profileDataStore
import edu.bu.reptrack.data.workout.RoomWorkoutRepository
import edu.bu.reptrack.data.workout.WorkoutRepository

/**
 * Manual dependency injection: one place that creates the app's repositories.
 * ViewModels get their dependencies from here through their factories.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val profileRepository: ProfileRepository =
        DataStoreProfileRepository(appContext.profileDataStore)

    // Built once for the whole app. Room opens the file on the first query, not here.
    private val database: RepTrackDatabase =
        Room.databaseBuilder(appContext, RepTrackDatabase::class.java, RepTrackDatabase.FILE_NAME).build()

    val workoutRepository: WorkoutRepository = RoomWorkoutRepository(database.workoutDao())

    val authRepository: AuthRepository = FirebaseAuthRepository(FirebaseAuth.getInstance())
}

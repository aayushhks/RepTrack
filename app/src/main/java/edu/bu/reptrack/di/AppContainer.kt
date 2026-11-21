package edu.bu.reptrack.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import edu.bu.reptrack.data.auth.AuthRepository
import edu.bu.reptrack.data.auth.FirebaseAuthRepository
import edu.bu.reptrack.data.db.RepTrackDatabase
import edu.bu.reptrack.data.exercise.ExerciseRepository
import edu.bu.reptrack.data.exercise.RoomExerciseRepository
import edu.bu.reptrack.data.exercise.WgerApi
import edu.bu.reptrack.data.exercise.wgerHttpClient
import edu.bu.reptrack.data.location.FusedLocationTracker
import edu.bu.reptrack.data.profile.DataStoreProfileRepository
import edu.bu.reptrack.data.profile.ProfileRepository
import edu.bu.reptrack.data.profile.profileDataStore
import edu.bu.reptrack.data.run.RoomRunRepository
import edu.bu.reptrack.data.run.RunRecorder
import edu.bu.reptrack.data.run.RunRepository
import edu.bu.reptrack.data.weight.RoomWeightRepository
import edu.bu.reptrack.data.weight.WeightRepository
import edu.bu.reptrack.data.workout.RoomWorkoutRepository
import edu.bu.reptrack.data.workout.WorkoutRepository
import edu.bu.reptrack.service.AndroidRunServiceController
import edu.bu.reptrack.service.RunServiceController
import edu.bu.reptrack.ui.run.story.AndroidStoryImageMaker
import edu.bu.reptrack.ui.run.story.StoryImageMaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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

    val exerciseRepository: ExerciseRepository =
        RoomExerciseRepository(database.exerciseDao(), WgerApi(wgerHttpClient()))

    val runRepository: RunRepository = RoomRunRepository(database.runDao())

    val weightRepository: WeightRepository = RoomWeightRepository(database.weightDao())

    // One recorder for the whole app, in a scope that lives as long as the app, so a run keeps
    // going when the run screen is closed or the phone is rotated.
    val runRecorder: RunRecorder = RunRecorder(
        tracker = FusedLocationTracker(appContext),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    val runServiceController: RunServiceController = AndroidRunServiceController(appContext)

    /** Draws a run as a story picture, with a map from OpenFreeMap behind the route. */
    val storyImageMaker: StoryImageMaker = AndroidStoryImageMaker(appContext)
}

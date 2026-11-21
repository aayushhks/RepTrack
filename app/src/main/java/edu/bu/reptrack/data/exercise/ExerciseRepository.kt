package edu.bu.reptrack.data.exercise

import edu.bu.reptrack.domain.exercise.Exercise
import kotlinx.coroutines.flow.Flow

/** The exercise catalog. Screens read the saved copy and ask for a refresh when they want it. */
interface ExerciseRepository {
    /** The saved exercises by name. Emits again after every refresh. */
    fun observeExercises(): Flow<List<Exercise>>

    /**
     * Downloads the catalog from wger and saves it. Throws if the network or the server fails.
     * What was saved before stays, so the app keeps working offline.
     */
    suspend fun refresh()
}

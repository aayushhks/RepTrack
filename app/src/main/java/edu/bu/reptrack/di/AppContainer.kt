package edu.bu.reptrack.di

import android.content.Context
import edu.bu.reptrack.data.profile.DataStoreProfileRepository
import edu.bu.reptrack.data.profile.ProfileRepository
import edu.bu.reptrack.data.profile.profileDataStore

/**
 * Manual dependency injection: one place that creates the app's repositories.
 * ViewModels get their dependencies from here through their factories.
 */
class AppContainer(context: Context) {
    val profileRepository: ProfileRepository =
        DataStoreProfileRepository(context.applicationContext.profileDataStore)
}

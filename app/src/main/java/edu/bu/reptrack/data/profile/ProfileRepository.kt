package edu.bu.reptrack.data.profile

import edu.bu.reptrack.domain.profile.UserProfile
import kotlinx.coroutines.flow.Flow

/** Reads and saves the user's profile. Screens talk to this, not to DataStore directly. */
interface ProfileRepository {
    /** The saved profile, or null if setup was skipped or never done. */
    val profile: Flow<UserProfile?>

    /** True once the user finished or skipped the setup screen. */
    val isSetupDone: Flow<Boolean>

    /** True once the user chose "Use without an account" on the sign-in screen. */
    val skippedSignIn: Flow<Boolean>

    suspend fun saveProfile(profile: UserProfile)

    suspend fun skipSetup()

    suspend fun skipSignIn()
}

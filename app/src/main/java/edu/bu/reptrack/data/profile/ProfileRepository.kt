package edu.bu.reptrack.data.profile

import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.domain.settings.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Reads and saves the user's profile. Screens talk to this, not to DataStore directly. */
interface ProfileRepository {
    /** The saved profile, or null if setup was skipped or never done. */
    val profile: Flow<UserProfile?>

    /** True once the user finished or skipped the setup screen. */
    val isSetupDone: Flow<Boolean>

    /** True once the user chose "Use without an account" on the sign-in screen. */
    val skippedSignIn: Flow<Boolean>

    /** Dark, light or follow the phone. Dark until the user changes it. */
    val themeMode: Flow<ThemeMode>

    suspend fun saveProfile(profile: UserProfile)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun skipSetup()

    suspend fun skipSignIn()
}

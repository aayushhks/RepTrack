package edu.bu.reptrack.fakes

import edu.bu.reptrack.data.profile.ProfileRepository
import edu.bu.reptrack.domain.profile.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [ProfileRepository] so UI tests never touch the real DataStore file. */
class FakeProfileRepository : ProfileRepository {
    private val profileFlow = MutableStateFlow<UserProfile?>(null)
    private val setupDoneFlow = MutableStateFlow(false)

    override val profile: Flow<UserProfile?> = profileFlow
    override val isSetupDone: Flow<Boolean> = setupDoneFlow

    val savedProfile: UserProfile? get() = profileFlow.value
    val setupDone: Boolean get() = setupDoneFlow.value

    override suspend fun saveProfile(profile: UserProfile) {
        profileFlow.value = profile
        setupDoneFlow.value = true
    }

    override suspend fun skipSetup() {
        setupDoneFlow.value = true
    }
}

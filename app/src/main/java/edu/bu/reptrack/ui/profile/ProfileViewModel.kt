package edu.bu.reptrack.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.auth.AuthRepository
import edu.bu.reptrack.data.auth.AuthUser
import edu.bu.reptrack.data.profile.ProfileRepository
import edu.bu.reptrack.data.workout.WorkoutRepository
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.domain.workout.startOfWeekMillis
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the Profile tab needs to draw itself. */
data class ProfileUiState(
    val isLoading: Boolean = true,
    /** Null when the user is using the app without an account. */
    val user: AuthUser? = null,
    /** Null when the user skipped goal setup. */
    val profile: UserProfile? = null,
    val workoutsThisWeek: Int = 0,
    val volumeThisWeekKg: Float = 0f,
    val showSignOutDialog: Boolean = false,
    /** True once sign-out finished, so the screen can go to sign in. */
    val signedOut: Boolean = false,
)

/**
 * Profile tab. Combines the account (Firebase), the saved stats (DataStore) and this week's
 * finished workouts (Room). All three are Flows, so the tab stays up to date by itself.
 */
class ProfileViewModel(
    profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    workoutRepository: WorkoutRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private data class LocalState(val showSignOutDialog: Boolean = false, val signedOut: Boolean = false)

    private val local = MutableStateFlow(LocalState())

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.currentUser,
        profileRepository.profile,
        workoutRepository.observeHistory(),
        local,
    ) { user, profile, history, local ->
        val weekStart = startOfWeekMillis(now(), zone)
        val thisWeek = history.filter { (it.session.finishedAt ?: 0L) >= weekStart }
        ProfileUiState(
            isLoading = false,
            user = user,
            profile = profile,
            workoutsThisWeek = thisWeek.size,
            volumeThisWeekKg = thisWeek.sumOf { it.totalVolumeKg.toDouble() }.toFloat(),
            showSignOutDialog = local.showSignOutDialog,
            signedOut = local.signedOut,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    /** Asks first, so a stray tap does not sign the user out. */
    fun requestSignOut() {
        local.update { it.copy(showSignOutDialog = true) }
    }

    fun dismissSignOut() {
        local.update { it.copy(showSignOutDialog = false) }
    }

    /** Signs out of Firebase. Workouts and stats stay on the phone. */
    fun signOut() {
        local.update { it.copy(showSignOutDialog = false) }
        viewModelScope.launch {
            authRepository.signOut()
            local.update { it.copy(signedOut = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RepTrackApplication).container
                ProfileViewModel(container.profileRepository, container.authRepository, container.workoutRepository)
            }
        }
    }
}

package edu.bu.reptrack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.auth.AuthRepository
import edu.bu.reptrack.data.profile.ProfileRepository
import edu.bu.reptrack.domain.auth.FirstScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** App-level state: decides whether the app opens on sign in, goal setup or the tabs. */
class AppViewModel(
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {

    /** Null while DataStore is still being read, then true or false. */
    val isSetupDone: StateFlow<Boolean?> = profileRepository.isSetupDone
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Null until Firebase and DataStore have both answered. The app reads this once, to pick
     * its first screen, so later changes (like signing in) do not rebuild the navigation graph.
     */
    val firstScreen: StateFlow<FirstScreen?> = combine(
        authRepository.currentUser,
        profileRepository.skippedSignIn,
        profileRepository.isSetupDone,
    ) { user, skippedSignIn, setupDone ->
        FirstScreen.decide(signedIn = user != null, skippedSignIn = skippedSignIn, setupDone = setupDone)
    }
        .map<FirstScreen, FirstScreen?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RepTrackApplication
                AppViewModel(app.container.authRepository, app.container.profileRepository)
            }
        }
    }
}

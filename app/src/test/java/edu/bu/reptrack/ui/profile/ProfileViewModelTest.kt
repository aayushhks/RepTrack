package edu.bu.reptrack.ui.profile

import edu.bu.reptrack.data.auth.AuthUser
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import edu.bu.reptrack.testing.FakeAuthRepository
import edu.bu.reptrack.testing.FakeProfileRepository
import edu.bu.reptrack.testing.FakeWorkoutRepository
import edu.bu.reptrack.testing.MainDispatcherRule
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Times in UTC. Monday Nov 9, 2026 is the start of the test week.
    private val sundayBefore = 1_794_168_000_000L // Sun Nov 8, 20:00
    private val tuesday = 1_794_304_800_000L // Tue Nov 10, 10:00
    private val wednesday = 1_794_398_400_000L // Wed Nov 11, 12:00

    private var clock = wednesday
    private val profiles = FakeProfileRepository()
    private val workouts = FakeWorkoutRepository(now = { clock })
    private val stats = UserProfile(178f, 82f, 76f, FitnessGoal.BUILD_MUSCLE, TrainingPlace.GYM)

    private fun newViewModel(auth: FakeAuthRepository) =
        ProfileViewModel(profiles, auth, workouts, now = { clock }, zone = ZoneOffset.UTC)

    // uiState only updates while someone collects it, like the screen does.
    private fun TestScope.startCollecting(viewModel: ProfileViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private suspend fun finishWorkout(at: Long, weightKg: Float, reps: Int) {
        clock = at
        val id = workouts.startSession("upper", "Upper body")
        workouts.addSet(id, "Bench press", weightKg, reps)
        workouts.finishSession(id)
    }

    @Test
    fun signedIn_showsEmailAndStats() = runTest {
        profiles.saveProfile(stats)
        val viewModel = newViewModel(FakeAuthRepository(AuthUser("u1", "aayush@bu.edu")))
        startCollecting(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("aayush@bu.edu", state.user?.email)
        assertEquals(stats, state.profile)
    }

    @Test
    fun withoutAnAccount_userIsNull() = runTest {
        val viewModel = newViewModel(FakeAuthRepository(initialUser = null))
        startCollecting(viewModel)

        assertNull(viewModel.uiState.value.user)
    }

    @Test
    fun thisWeek_countsOnlyWorkoutsFinishedSinceMonday() = runTest {
        finishWorkout(at = sundayBefore, weightKg = 100f, reps = 10)
        finishWorkout(at = tuesday, weightKg = 60f, reps = 10)
        finishWorkout(at = tuesday, weightKg = 50f, reps = 10)
        clock = wednesday

        val viewModel = newViewModel(FakeAuthRepository())
        startCollecting(viewModel)

        assertEquals(2, viewModel.uiState.value.workoutsThisWeek)
        assertEquals(1100f, viewModel.uiState.value.volumeThisWeekKg)
    }

    @Test
    fun signOut_asksFirst_thenSignsOut() = runTest {
        val auth = FakeAuthRepository(AuthUser("u1", "aayush@bu.edu"))
        val viewModel = newViewModel(auth)
        startCollecting(viewModel)

        viewModel.requestSignOut()
        assertTrue(viewModel.uiState.value.showSignOutDialog)

        viewModel.signOut()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showSignOutDialog)
        assertTrue(state.signedOut)
        assertNull(state.user)
    }

    @Test
    fun cancel_keepsYouSignedIn() = runTest {
        val viewModel = newViewModel(FakeAuthRepository(AuthUser("u1", "aayush@bu.edu")))
        startCollecting(viewModel)

        viewModel.requestSignOut()
        viewModel.dismissSignOut()

        assertFalse(viewModel.uiState.value.showSignOutDialog)
        assertEquals("aayush@bu.edu", viewModel.uiState.value.user?.email)
    }
}

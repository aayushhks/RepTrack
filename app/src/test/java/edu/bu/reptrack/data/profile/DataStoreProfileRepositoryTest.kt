package edu.bu.reptrack.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Runs the real DataStore against a temp file, so no emulator is needed. */
class DataStoreProfileRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun newDataStore(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tempFolder.root, "profile_test.preferences_pb") },
        )

    private val profile = UserProfile(178f, 82f, 76f, FitnessGoal.BUILD_MUSCLE, TrainingPlace.HOME)

    @Test
    fun newInstall_hasNoProfile_andSetupNotDone() = runTest {
        val repository = DataStoreProfileRepository(newDataStore(backgroundScope))

        assertNull(repository.profile.first())
        assertFalse(repository.isSetupDone.first())
    }

    @Test
    fun saveProfile_canBeReadBack_andMarksSetupDone() = runTest {
        val repository = DataStoreProfileRepository(newDataStore(backgroundScope))

        repository.saveProfile(profile)

        assertEquals(profile, repository.profile.first())
        assertTrue(repository.isSetupDone.first())
    }

    @Test
    fun skipSetup_marksDone_withoutAProfile() = runTest {
        val repository = DataStoreProfileRepository(newDataStore(backgroundScope))

        repository.skipSetup()

        assertTrue(repository.isSetupDone.first())
        assertNull(repository.profile.first())
    }

    @Test
    fun unknownGoalName_fallsBackToStayFit() = runTest {
        val dataStore = newDataStore(backgroundScope)
        // Simulates a value saved by an older version of the app.
        dataStore.edit { prefs ->
            prefs[floatPreferencesKey("height_cm")] = 170f
            prefs[floatPreferencesKey("weight_kg")] = 70f
            prefs[floatPreferencesKey("goal_weight_kg")] = 68f
            prefs[stringPreferencesKey("goal")] = "RUN_A_MARATHON"
        }

        val saved = DataStoreProfileRepository(dataStore).profile.first()

        assertEquals(FitnessGoal.STAY_FIT, saved?.goal)
        assertEquals(TrainingPlace.GYM, saved?.place)
    }
}

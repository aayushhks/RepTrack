package edu.bu.reptrack.data.profile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import edu.bu.reptrack.domain.profile.FitnessGoal
import edu.bu.reptrack.domain.profile.TrainingPlace
import edu.bu.reptrack.domain.profile.UserProfile
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** One DataStore file for the profile, kept in the app's private storage. */
val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "profile")

/** [ProfileRepository] backed by Preferences DataStore. */
class DataStoreProfileRepository(
    private val dataStore: DataStore<Preferences>,
) : ProfileRepository {

    private object Keys {
        val HEIGHT_CM = floatPreferencesKey("height_cm")
        val WEIGHT_KG = floatPreferencesKey("weight_kg")
        val GOAL_WEIGHT_KG = floatPreferencesKey("goal_weight_kg")
        val GOAL = stringPreferencesKey("goal")
        val PLACE = stringPreferencesKey("place")
        val SETUP_DONE = booleanPreferencesKey("setup_done")
    }

    // A read error (for example a corrupted file) falls back to empty values instead of crashing.
    private val safeData: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    override val isSetupDone: Flow<Boolean> = safeData.map { it[Keys.SETUP_DONE] ?: false }

    override val profile: Flow<UserProfile?> = safeData.map { prefs ->
        val height = prefs[Keys.HEIGHT_CM] ?: return@map null
        val weight = prefs[Keys.WEIGHT_KG] ?: return@map null
        val goalWeight = prefs[Keys.GOAL_WEIGHT_KG] ?: return@map null
        UserProfile(
            heightCm = height,
            weightKg = weight,
            goalWeightKg = goalWeight,
            goal = enumOrDefault(prefs[Keys.GOAL], FitnessGoal.STAY_FIT),
            place = enumOrDefault(prefs[Keys.PLACE], TrainingPlace.GYM),
        )
    }

    override suspend fun saveProfile(profile: UserProfile) {
        dataStore.edit { prefs ->
            prefs[Keys.HEIGHT_CM] = profile.heightCm
            prefs[Keys.WEIGHT_KG] = profile.weightKg
            prefs[Keys.GOAL_WEIGHT_KG] = profile.goalWeightKg
            prefs[Keys.GOAL] = profile.goal.name
            prefs[Keys.PLACE] = profile.place.name
            prefs[Keys.SETUP_DONE] = true
        }
    }

    override suspend fun skipSetup() {
        dataStore.edit { prefs -> prefs[Keys.SETUP_DONE] = true }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}

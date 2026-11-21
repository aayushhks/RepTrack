package edu.bu.reptrack.data.exercise

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** When the exercise catalog was last downloaded, so we do not download it on every launch. */
interface RefreshStamp {
    /** Milliseconds since 1970, or null if it was never downloaded. */
    suspend fun lastRefresh(): Long?

    suspend fun setLastRefresh(millis: Long)
}

/** One DataStore file for the catalog's download time. */
val Context.exerciseDataStore: DataStore<Preferences> by preferencesDataStore(name = "exercises")

/** [RefreshStamp] backed by Preferences DataStore. */
class DataStoreRefreshStamp(private val dataStore: DataStore<Preferences>) : RefreshStamp {
    private val key = longPreferencesKey("last_refresh")

    override suspend fun lastRefresh(): Long? = dataStore.data.first()[key]

    override suspend fun setLastRefresh(millis: Long) {
        dataStore.edit { it[key] = millis }
    }
}

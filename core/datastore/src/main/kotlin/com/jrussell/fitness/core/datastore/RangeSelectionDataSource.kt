package com.jrussell.fitness.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Each screen's remembered date range, as the text `:core:data` encodes, in the same
 * Preferences DataStore as [UserPreferencesDataSource] (key `range_<screen>`). Like that class,
 * nothing here can stop the app: a missing, wrongly typed or unreadable value reads as `null`
 * (the screen's default), and a failed write is dropped.
 */
class RangeSelectionDataSource(private val dataStore: DataStore<Preferences>) {
    /** The stored text for [screen], or `null`; emits again when it changes. */
    fun selection(screen: String): Flow<String?> {
        val key: Preferences.Key<String> = keyFor(screen)
        return dataStore.data
            .catch { error: Throwable -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { preferences: Preferences -> preferences.asMap()[key] as? String }
            .distinctUntilChanged()
    }

    suspend fun setSelection(screen: String, encoded: String) {
        try {
            dataStore.edit { preferences: MutablePreferences -> preferences[keyFor(screen)] = encoded }
        } catch (
            // A remembered range failing to save must not crash the app; the screen keeps working.
            @Suppress("SwallowedException") ignored: IOException,
        ) {
            // Intentionally empty; see above.
        }
    }

    private fun keyFor(screen: String): Preferences.Key<String> {
        return stringPreferencesKey("range_$screen")
    }
}

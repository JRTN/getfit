package com.jrussell.fitness.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import kotlinx.coroutines.CoroutineScope

/**
 * Creates the DataStore that holds [UserPreferences][com.jrussell.fitness.core.model.UserPreferences].
 * Create exactly one per file per process (`:app` provides it as a singleton).
 *
 * A file that can't be parsed (truncated, or written by an incompatible version, e.g. after a
 * backup restore) is replaced with empty preferences, so the app starts with defaults instead of
 * failing on every launch.
 *
 * @param scope where DataStore does its I/O: `:app` passes one on the injected IO dispatcher,
 *   tests pass their own.
 * @param produceFile the preferences file, e.g. `context.preferencesDataStoreFile("user_preferences")`.
 */
fun createUserPreferencesDataStore(scope: CoroutineScope, produceFile: () -> File): DataStore<Preferences> {
    return PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope,
        produceFile = produceFile,
    )
}

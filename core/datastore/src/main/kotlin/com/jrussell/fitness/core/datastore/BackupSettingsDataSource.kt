package com.jrussell.fitness.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The daily backup's stored state: the folder (URI and display name), the last success time, and
 * the reason the last attempt failed, as `:core:data` names it. `null` means not set.
 */
data class BackupSettings(
    val folderUri: String?,
    val folderName: String?,
    val lastSuccessMillis: Long?,
    val lastFailure: String?,
)

/**
 * [BackupSettings] in the same Preferences DataStore as the UI preferences. As with the other
 * data sources there, a missing, wrongly typed or unreadable value reads as `null`, and a failed
 * write is dropped rather than crashing a backup.
 */
class BackupSettingsDataSource(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<BackupSettings> = dataStore.data
        .catch { error: Throwable -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences: Preferences ->
            val stored: Map<Preferences.Key<*>, Any> = preferences.asMap()
            BackupSettings(
                folderUri = stored[FOLDER_URI] as? String,
                folderName = stored[FOLDER_NAME] as? String,
                lastSuccessMillis = stored[LAST_SUCCESS] as? Long,
                lastFailure = stored[LAST_FAILURE] as? String,
            )
        }
        .distinctUntilChanged()

    /** Remembers a newly chosen folder; its history (last success or failure) starts over. */
    suspend fun setFolder(uri: String, name: String) {
        editIgnoringIoErrors { preferences: MutablePreferences ->
            preferences[FOLDER_URI] = uri
            preferences[FOLDER_NAME] = name
            preferences.remove(LAST_SUCCESS)
            preferences.remove(LAST_FAILURE)
        }
    }

    /** A backup succeeded at [atMillis]; any earlier failure is cleared. */
    suspend fun recordSuccess(atMillis: Long) {
        editIgnoringIoErrors { preferences: MutablePreferences ->
            preferences[LAST_SUCCESS] = atMillis
            preferences.remove(LAST_FAILURE)
        }
    }

    /** A backup failed for [reason]; the last success stays as it was. */
    suspend fun recordFailure(reason: String) {
        editIgnoringIoErrors { preferences: MutablePreferences -> preferences[LAST_FAILURE] = reason }
    }

    private suspend fun editIgnoringIoErrors(transform: (MutablePreferences) -> Unit) {
        try {
            dataStore.edit { preferences: MutablePreferences -> transform(preferences) }
        } catch (
            // Losing a status update must not turn a finished backup into a crash.
            @Suppress("SwallowedException") ignored: IOException,
        ) {
            // Intentionally empty; see above.
        }
    }

    private companion object {
        val FOLDER_URI: Preferences.Key<String> = stringPreferencesKey("backup_folder_uri")
        val FOLDER_NAME: Preferences.Key<String> = stringPreferencesKey("backup_folder_name")
        val LAST_SUCCESS: Preferences.Key<Long> = longPreferencesKey("backup_last_success_ms")
        val LAST_FAILURE: Preferences.Key<String> = stringPreferencesKey("backup_last_failure")
    }
}

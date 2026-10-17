package com.jrussell.fitness.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jrussell.fitness.core.model.ColorPalette
import com.jrussell.fitness.core.model.ThemeMode
import com.jrussell.fitness.core.model.UserPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Reads and writes [UserPreferences] in a Preferences DataStore file.
 *
 * Nothing about this file can stop the app from starting:
 * - enums are stored by `name`, and a name the app doesn't recognise (renamed in a later version,
 *   or a restored backup from a newer one) falls back to that field's default;
 * - a read that fails with an I/O error emits the defaults;
 * - a write that fails with an I/O error is dropped (the choice simply isn't saved);
 * - an unparseable file is replaced by [createUserPreferencesDataStore]'s corruption handler.
 */
class UserPreferencesDataSource(private val dataStore: DataStore<Preferences>) {
    /** The current preferences; emits again whenever they change. */
    val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { error: Throwable -> emitEmptyOnIoError(error) }
        .map { preferences: Preferences ->
            UserPreferences(
                themeMode = enumOrDefault(preferences[THEME_MODE], UserPreferences.DEFAULT.themeMode),
                palette = enumOrDefault(preferences[COLOR_PALETTE], UserPreferences.DEFAULT.palette),
            )
        }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        editIgnoringIoErrors { preferences: MutablePreferences -> preferences[THEME_MODE] = themeMode.name }
    }

    suspend fun setPalette(palette: ColorPalette) {
        editIgnoringIoErrors { preferences: MutablePreferences -> preferences[COLOR_PALETTE] = palette.name }
    }

    private suspend fun FlowCollector<Preferences>.emitEmptyOnIoError(error: Throwable) {
        if (error is IOException) {
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    private suspend fun editIgnoringIoErrors(transform: (MutablePreferences) -> Unit) {
        try {
            dataStore.edit { preferences: MutablePreferences -> transform(preferences) }
        } catch (
            // A UI preference failing to save must not crash the app; the old value stays in effect.
            @Suppress("SwallowedException") ignored: IOException,
        ) {
            // Intentionally empty; see above.
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T {
        return enumValues<T>().firstOrNull { value: T -> value.name == name } ?: default
    }

    private companion object {
        val THEME_MODE: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        val COLOR_PALETTE: Preferences.Key<String> = stringPreferencesKey("color_palette")
    }
}

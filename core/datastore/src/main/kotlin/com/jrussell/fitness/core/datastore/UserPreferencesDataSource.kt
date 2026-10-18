package com.jrussell.fitness.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jrussell.fitness.core.model.ColorPalette
import com.jrussell.fitness.core.model.MassUnit
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
 * - enums are stored by `name`, and a name the app doesn't recognize (renamed in a later version,
 *   or a restored backup from a newer one) falls back to that field's default;
 * - a value stored with the wrong type (only possible if a later version changes a key's type)
 *   is treated as absent, so it also falls back to the default;
 * - a read that fails with an I/O error emits the defaults;
 * - a write that fails with an I/O error is dropped (the choice simply isn't saved);
 * - an unparseable file is replaced by [createUserPreferencesDataStore]'s corruption handler.
 *
 * @param defaultMassUnit the weight unit used until the user picks one (`:app` passes the
 *   locale's customary unit: pounds in the US).
 */
class UserPreferencesDataSource(
    private val dataStore: DataStore<Preferences>,
    private val defaultMassUnit: MassUnit = UserPreferences.DEFAULT.massUnit,
) {
    /** The current preferences; emits again whenever they change. */
    val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { error: Throwable -> emitEmptyOnIoError(error) }
        .map { preferences: Preferences ->
            UserPreferences(
                themeMode = enumOrDefault(storedString(preferences, THEME_MODE), UserPreferences.DEFAULT.themeMode),
                palette = enumOrDefault(storedString(preferences, COLOR_PALETTE), UserPreferences.DEFAULT.palette),
                massUnit = enumOrDefault(storedString(preferences, MASS_UNIT), defaultMassUnit),
            )
        }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        editIgnoringIoErrors { preferences: MutablePreferences -> preferences[THEME_MODE] = themeMode.name }
    }

    suspend fun setPalette(palette: ColorPalette) {
        editIgnoringIoErrors { preferences: MutablePreferences -> preferences[COLOR_PALETTE] = palette.name }
    }

    suspend fun setMassUnit(unit: MassUnit) {
        editIgnoringIoErrors { preferences: MutablePreferences -> preferences[MASS_UNIT] = unit.name }
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

    /** The string stored under [key], or null when it is absent or was stored with another type. */
    private fun storedString(preferences: Preferences, key: Preferences.Key<String>): String? {
        return preferences.asMap()[key] as? String
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T {
        return enumValues<T>().firstOrNull { value: T -> value.name == name } ?: default
    }

    private companion object {
        val THEME_MODE: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        val COLOR_PALETTE: Preferences.Key<String> = stringPreferencesKey("color_palette")
        val MASS_UNIT: Preferences.Key<String> = stringPreferencesKey("mass_unit")
    }
}

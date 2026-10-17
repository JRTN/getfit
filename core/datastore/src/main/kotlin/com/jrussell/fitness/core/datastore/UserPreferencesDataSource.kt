package com.jrussell.fitness.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jrussell.fitness.core.model.ColorPalette
import com.jrussell.fitness.core.model.ThemeMode
import com.jrussell.fitness.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reads and writes [UserPreferences] in a Preferences DataStore file.
 *
 * Enums are stored by `name`. A stored name the app doesn't recognise (renamed in a later
 * version, or a restored backup from a newer one) falls back to that field's default instead of
 * failing, so a bad value can never stop the app from starting.
 */
class UserPreferencesDataSource(private val dataStore: DataStore<Preferences>) {
    /** The current preferences; emits again whenever they change. */
    val userPreferences: Flow<UserPreferences> = dataStore.data.map { preferences: Preferences ->
        UserPreferences(
            themeMode = enumOrDefault(preferences[THEME_MODE], UserPreferences.DEFAULT.themeMode),
            palette = enumOrDefault(preferences[COLOR_PALETTE], UserPreferences.DEFAULT.palette),
        )
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences -> preferences[THEME_MODE] = themeMode.name }
    }

    suspend fun setPalette(palette: ColorPalette) {
        dataStore.edit { preferences -> preferences[COLOR_PALETTE] = palette.name }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T {
        return enumValues<T>().firstOrNull { value: T -> value.name == name } ?: default
    }

    private companion object {
        val THEME_MODE: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        val COLOR_PALETTE: Preferences.Key<String> = stringPreferencesKey("color_palette")
    }
}

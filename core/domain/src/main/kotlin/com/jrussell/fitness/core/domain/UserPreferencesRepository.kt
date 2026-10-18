package com.jrussell.fitness.core.domain

import com.jrussell.fitness.core.model.ColorPalette
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.model.ThemeMode
import com.jrussell.fitness.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/** The user's UI preferences (theme mode, palette, weight unit). Implemented in `:core:data`. */
interface UserPreferencesRepository {
    /** The current preferences; emits again whenever they change. Never fails on a bad stored value. */
    val userPreferences: Flow<UserPreferences>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setPalette(palette: ColorPalette)

    suspend fun setMassUnit(unit: MassUnit)
}

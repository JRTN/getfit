package com.jrussell.fitness.core.testing

import com.jrussell.fitness.core.domain.UserPreferencesRepository
import com.jrussell.fitness.core.model.ColorPalette
import com.jrussell.fitness.core.model.ThemeMode
import com.jrussell.fitness.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [UserPreferencesRepository] for tests. Starts from [initial]. */
class FakeUserPreferencesRepository(initial: UserPreferences = UserPreferences.DEFAULT) : UserPreferencesRepository {
    private val state: MutableStateFlow<UserPreferences> = MutableStateFlow(initial)

    override val userPreferences: Flow<UserPreferences> = state

    /** The latest value, for assertions. */
    val current: UserPreferences
        get() {
            return state.value
        }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        state.update { preferences: UserPreferences -> preferences.copy(themeMode = themeMode) }
    }

    override suspend fun setPalette(palette: ColorPalette) {
        state.update { preferences: UserPreferences -> preferences.copy(palette = palette) }
    }
}

package com.jrussell.fitness.core.data.preferences

import com.jrussell.fitness.core.datastore.UserPreferencesDataSource
import com.jrussell.fitness.core.domain.UserPreferencesRepository
import com.jrussell.fitness.core.model.ColorPalette
import com.jrussell.fitness.core.model.ThemeMode
import com.jrussell.fitness.core.model.UserPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** [UserPreferencesRepository] backed by the Preferences DataStore in `:core:datastore`. */
class DefaultUserPreferencesRepository @Inject constructor(private val dataSource: UserPreferencesDataSource) :
    UserPreferencesRepository {
    override val userPreferences: Flow<UserPreferences> = dataSource.userPreferences

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataSource.setThemeMode(themeMode)
    }

    override suspend fun setPalette(palette: ColorPalette) {
        dataSource.setPalette(palette)
    }
}

package com.jrussell.fitness.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jrussell.fitness.core.model.ColorPalette
import com.jrussell.fitness.core.model.ThemeMode
import com.jrussell.fitness.core.model.UserPreferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesDataSourceTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val file: File by lazy { File(temporaryFolder.root, "user_preferences.preferences_pb") }

    @Test
    fun userPreferences_noFile_emitsDefaults() = runTest {
        val dataSource: UserPreferencesDataSource = UserPreferencesDataSource(dataStoreIn(backgroundScope))

        assertThat(dataSource.userPreferences.first()).isEqualTo(UserPreferences.DEFAULT)
    }

    @Test
    fun setThemeMode_emitsUpdatedValue() = runTest {
        val dataSource: UserPreferencesDataSource = UserPreferencesDataSource(dataStoreIn(backgroundScope))

        dataSource.userPreferences.test {
            assertThat(awaitItem()).isEqualTo(UserPreferences.DEFAULT)
            dataSource.setThemeMode(ThemeMode.DARK)
            assertThat(awaitItem()).isEqualTo(UserPreferences(ThemeMode.DARK, ColorPalette.DEFAULT))
        }
    }

    @Test
    fun setPalette_persistsAcrossNewDataStoreInstance() = runTest {
        val firstJob: Job = Job()
        val first: UserPreferencesDataSource = UserPreferencesDataSource(dataStoreIn(scopeWith(firstJob)))
        first.setPalette(ColorPalette.HIGH_CONTRAST)
        firstJob.cancelAndJoin()

        val second: UserPreferencesDataSource = UserPreferencesDataSource(dataStoreIn(backgroundScope))

        assertThat(second.userPreferences.first().palette).isEqualTo(ColorPalette.HIGH_CONTRAST)
    }

    @Test
    fun userPreferences_unknownStoredValue_fallsBackToDefault() = runTest {
        val dataStore: DataStore<Preferences> = dataStoreIn(backgroundScope)
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("color_palette")] = "SEPIA"
            preferences[stringPreferencesKey("theme_mode")] = "DARK"
        }

        val result: UserPreferences = UserPreferencesDataSource(dataStore).userPreferences.first()

        assertThat(result).isEqualTo(UserPreferences(ThemeMode.DARK, ColorPalette.DEFAULT))
    }

    private fun TestScope.scopeWith(job: Job): CoroutineScope {
        return CoroutineScope(UnconfinedTestDispatcher(testScheduler) + job)
    }

    private fun dataStoreIn(scope: CoroutineScope): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }
}

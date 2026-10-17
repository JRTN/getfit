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
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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

    @Test
    fun userPreferences_corruptFile_emitsDefaults() = runTest {
        file.writeBytes(byteArrayOf(0x13, 0x37, 0x00, 0x7F, 0x42))

        val dataStore: DataStore<Preferences> = createUserPreferencesDataStore(scope = backgroundScope) { file }

        assertThat(UserPreferencesDataSource(dataStore).userPreferences.first()).isEqualTo(UserPreferences.DEFAULT)
    }

    @Test
    fun userPreferences_readFailure_emitsDefaults() = runTest {
        val dataSource: UserPreferencesDataSource = UserPreferencesDataSource(FailingDataStore())

        assertThat(dataSource.userPreferences.first()).isEqualTo(UserPreferences.DEFAULT)
    }

    @Test
    fun setPalette_writeFailure_doesNotThrow() = runTest {
        val dataSource: UserPreferencesDataSource = UserPreferencesDataSource(FailingDataStore())

        dataSource.setPalette(ColorPalette.HIGH_CONTRAST)
        dataSource.setThemeMode(ThemeMode.DARK)
    }

    /** A DataStore whose disk always fails, as on an I/O error. */
    private class FailingDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("disk read failed") }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            throw IOException("disk write failed")
        }
    }

    private fun TestScope.scopeWith(job: Job): CoroutineScope {
        return CoroutineScope(UnconfinedTestDispatcher(testScheduler) + job)
    }

    private fun dataStoreIn(scope: CoroutineScope): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }
}

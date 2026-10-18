package com.jrussell.fitness.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupSettingsDataSourceTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val file: File by lazy { File(temporaryFolder.root, "user_preferences.preferences_pb") }

    @Test
    fun nothingStored_isAllNull() = runTest {
        assertThat(BackupSettingsDataSource(dataStoreIn(backgroundScope)).settings.first())
            .isEqualTo(BackupSettings(null, null, null, null))
    }

    @Test
    fun setFolder_storesUriAndName_andStartsHistoryOver() = runTest {
        val dataSource: BackupSettingsDataSource = BackupSettingsDataSource(dataStoreIn(backgroundScope))
        dataSource.recordSuccess(1_000)
        dataSource.recordFailure("FOLDER_UNAVAILABLE")

        dataSource.setFolder("content://tree/1", "GetFit backups")

        assertThat(dataSource.settings.first())
            .isEqualTo(BackupSettings("content://tree/1", "GetFit backups", null, null))
    }

    @Test
    fun recordFailure_keepsLastSuccess() = runTest {
        val dataSource: BackupSettingsDataSource = BackupSettingsDataSource(dataStoreIn(backgroundScope))
        dataSource.recordSuccess(1_000)

        dataSource.recordFailure("WRITE_FAILED")

        assertThat(dataSource.settings.first()).isEqualTo(BackupSettings(null, null, 1_000, "WRITE_FAILED"))
    }

    @Test
    fun recordSuccess_clearsFailure() = runTest {
        val dataSource: BackupSettingsDataSource = BackupSettingsDataSource(dataStoreIn(backgroundScope))
        dataSource.recordFailure("WRITE_FAILED")

        dataSource.recordSuccess(2_000)

        assertThat(dataSource.settings.first()).isEqualTo(BackupSettings(null, null, 2_000, null))
    }

    @Test
    fun wrongTypedValue_isNull() = runTest {
        val dataStore: DataStore<Preferences> = dataStoreIn(backgroundScope)
        dataStore.edit { preferences -> preferences[stringPreferencesKey("backup_last_success_ms")] = "yesterday" }

        assertThat(BackupSettingsDataSource(dataStore).settings.first().lastSuccessMillis).isEqualTo(null)
    }

    private fun dataStoreIn(scope: CoroutineScope): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }
}

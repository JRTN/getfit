package com.jrussell.fitness.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RangeSelectionDataSourceTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val file: File by lazy { File(temporaryFolder.root, "user_preferences.preferences_pb") }

    @Test
    fun absent_isNull() = runTest {
        assertThat(RangeSelectionDataSource(dataStoreIn(backgroundScope)).selection("home").first()).isNull()
    }

    @Test
    fun set_isKeptPerScreen() = runTest {
        val dataSource: RangeSelectionDataSource = RangeSelectionDataSource(dataStoreIn(backgroundScope))

        dataSource.setSelection("home", "TODAY")
        dataSource.setSelection("body", "FROM:2026-06-01")

        assertThat(dataSource.selection("home").first()).isEqualTo("TODAY")
        assertThat(dataSource.selection("body").first()).isEqualTo("FROM:2026-06-01")
    }

    @Test
    fun wrongTypedValue_isNull() = runTest {
        val dataStore: DataStore<Preferences> = dataStoreIn(backgroundScope)
        dataStore.edit { preferences -> preferences[intPreferencesKey("range_body")] = 3 }

        assertThat(RangeSelectionDataSource(dataStore).selection("body").first()).isNull()
    }

    @Test
    fun diskFailures_readAsNullAndWritesAreDropped() = runTest {
        val dataSource: RangeSelectionDataSource = RangeSelectionDataSource(FailingDataStore())

        dataSource.setSelection("body", "ALL")

        assertThat(dataSource.selection("body").first()).isNull()
    }

    private class FailingDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("disk read failed") }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            throw IOException("disk write failed")
        }
    }

    private fun dataStoreIn(scope: CoroutineScope): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }
}

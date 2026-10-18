package com.jrussell.fitness.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jrussell.fitness.core.datastore.RangeSelectionDataSource
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.domain.range.RangeScreen
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultDateRangeRepositoryTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val file: File by lazy { File(temporaryFolder.root, "user_preferences.preferences_pb") }

    @Test
    fun nothingSaved_usesEachScreensDefault() = runTest {
        val repository: DefaultDateRangeRepository = repositoryOver(dataStoreIn(backgroundScope))

        assertThat(repository.selection(RangeScreen.HOME).first()).isEqualTo(DateRangeSelection.Today)
        assertThat(repository.selection(RangeScreen.BODY).first()).isEqualTo(DateRangeSelection.All)
    }

    @Test
    fun saved_isRememberedSeparatelyPerScreen() = runTest {
        val repository: DefaultDateRangeRepository = repositoryOver(dataStoreIn(backgroundScope))

        repository.setSelection(RangeScreen.BODY, DateRangeSelection.From(LocalDate(2026, 6, 1)))

        assertThat(
            repository.selection(RangeScreen.BODY).first(),
        ).isEqualTo(DateRangeSelection.From(LocalDate(2026, 6, 1)))
        assertThat(repository.selection(RangeScreen.HOME).first()).isEqualTo(DateRangeSelection.Today)
    }

    @Test
    fun unreadableSaved_fallsBackToTheDefault() = runTest {
        val dataStore: DataStore<Preferences> = dataStoreIn(backgroundScope)
        dataStore.edit { preferences -> preferences[stringPreferencesKey("range_body")] = "NEXT_TUESDAY" }

        assertThat(repositoryOver(dataStore).selection(RangeScreen.BODY).first()).isEqualTo(DateRangeSelection.All)
    }

    @Test
    fun savedToday_onAScreenWithoutToday_readsAsTheDefault() = runTest {
        val dataStore: DataStore<Preferences> = dataStoreIn(backgroundScope)
        dataStore.edit { preferences -> preferences[stringPreferencesKey("range_body")] = "TODAY" }

        assertThat(repositoryOver(dataStore).selection(RangeScreen.BODY).first()).isEqualTo(DateRangeSelection.All)
    }

    private fun repositoryOver(dataStore: DataStore<Preferences>): DefaultDateRangeRepository {
        return DefaultDateRangeRepository(RangeSelectionDataSource(dataStore))
    }

    private fun dataStoreIn(scope: CoroutineScope): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }
}

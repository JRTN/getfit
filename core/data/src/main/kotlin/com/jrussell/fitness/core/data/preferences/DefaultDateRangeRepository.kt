package com.jrussell.fitness.core.data.preferences

import com.jrussell.fitness.core.datastore.RangeSelectionDataSource
import com.jrussell.fitness.core.domain.range.DateRangeRepository
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.domain.range.RangeScreen
import com.jrussell.fitness.core.domain.range.decodeDateRangeSelection
import com.jrussell.fitness.core.domain.range.encodeDateRangeSelection
import com.jrussell.fitness.core.domain.range.offers
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [DateRangeRepository] over the Preferences DataStore. An unreadable saved range, or one the
 * screen no longer offers, reads as the default.
 */
class DefaultDateRangeRepository @Inject constructor(private val dataSource: RangeSelectionDataSource) :
    DateRangeRepository {
    override fun selection(screen: RangeScreen): Flow<DateRangeSelection> {
        return dataSource.selection(screen.storageName())
            .map { text: String? ->
                decodeDateRangeSelection(text)?.takeIf(screen::offers) ?: screen.defaultSelection
            }
    }

    override suspend fun setSelection(screen: RangeScreen, selection: DateRangeSelection) {
        dataSource.setSelection(screen.storageName(), encodeDateRangeSelection(selection))
    }

    /** The key suffix for [this] screen; fixed text, so renaming the enum never loses a saved range. */
    private fun RangeScreen.storageName(): String {
        return when (this) {
            RangeScreen.HOME -> "home"
            RangeScreen.BODY -> "body"
        }
    }
}

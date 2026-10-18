package com.jrussell.fitness.core.testing

import com.jrussell.fitness.core.domain.range.DateRangeRepository
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.domain.range.RangeScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [DateRangeRepository] for tests: each screen starts on its default, or on [initial]. */
class FakeDateRangeRepository(initial: Map<RangeScreen, DateRangeSelection> = emptyMap()) : DateRangeRepository {
    private val state: MutableStateFlow<Map<RangeScreen, DateRangeSelection>> = MutableStateFlow(initial)

    override fun selection(screen: RangeScreen): Flow<DateRangeSelection> {
        return state.map { saved: Map<RangeScreen, DateRangeSelection> -> saved[screen] ?: screen.defaultSelection }
    }

    override suspend fun setSelection(screen: RangeScreen, selection: DateRangeSelection) {
        state.update { saved: Map<RangeScreen, DateRangeSelection> -> saved + (screen to selection) }
    }

    /** The saved range for [screen], or `null` if nothing was saved, for assertions. */
    fun saved(screen: RangeScreen): DateRangeSelection? {
        return state.value[screen]
    }
}

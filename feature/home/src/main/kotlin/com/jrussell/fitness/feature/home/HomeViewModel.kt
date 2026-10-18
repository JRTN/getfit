package com.jrussell.fitness.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrussell.fitness.core.domain.DayBoundaryRepository
import com.jrussell.fitness.core.domain.UserPreferencesRepository
import com.jrussell.fitness.core.domain.metric.Bucket
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.domain.metric.MetricIds
import com.jrussell.fitness.core.domain.metric.MetricRegistry
import com.jrussell.fitness.core.domain.metric.RangeChange
import com.jrussell.fitness.core.domain.metric.rangeChange
import com.jrussell.fitness.core.domain.observeToday
import com.jrussell.fitness.core.domain.range.DateRangeRepository
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.domain.range.RangeScreen
import com.jrussell.fitness.core.domain.range.cardRange
import com.jrussell.fitness.core.domain.range.chartRange
import com.jrussell.fitness.core.domain.time.WallClock
import com.jrussell.fitness.core.model.Mass
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange

/**
 * Home's state: the selected range (remembered across restarts, separately from other screens')
 * and one card per domain with data. Read-only by design
 * (decision #38): Home summarizes, the tabs edit. Values are converted to the user's unit here.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val metrics: MetricRegistry,
    private val preferences: UserPreferencesRepository,
    private val ranges: DateRangeRepository,
    private val dayBoundary: DayBoundaryRepository,
    private val clock: WallClock,
) : ViewModel() {

    /** `null` until the first read, so Home never flashes an empty state before data arrives. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState?> = observeToday(clock, dayBoundary).flatMapLatest { day: LocalDate ->
        stateFor(day)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    fun onRangeSelected(selection: DateRangeSelection) {
        viewModelScope.launch {
            ranges.setSelection(RangeScreen.HOME, selection)
        }
    }

    private fun stateFor(today: LocalDate): Flow<HomeUiState> {
        val history: LocalDateRange = HISTORY_START..today
        return combine(
            ranges.selection(RangeScreen.HOME),
            preferences.userPreferences,
            metrics.series(MetricIds.BODY_WEIGHT, history, Bucket.DAY),
        ) { selected: DateRangeSelection, prefs: UserPreferences, weights: List<DataPoint> ->
            HomeUiState(
                today = today,
                selection = selected,
                body = bodyCard(selected, today, prefs.massUnit, weights),
            )
        }
    }

    private fun bodyCard(
        selection: DateRangeSelection,
        today: LocalDate,
        unit: MassUnit,
        weights: List<DataPoint>,
    ): BodyCard? {
        val latest: DataPoint = weights.lastOrNull() ?: return null
        val inUnit: List<DataPoint> = weights.map { point: DataPoint -> point.copy(value = point.value.inUnit(unit)) }
        val latestValue: Double = latest.value.inUnit(unit)
        return if (selection == DateRangeSelection.Today) {
            val week: LocalDateRange = selection.chartRange(today)
            BodyCard.Today(
                latest = latestValue,
                latestDate = latest.date,
                perWeek = rangeChange(inUnit, week)?.perWeek ?: 0.0,
                since = week.start,
                unit = unit,
            )
        } else {
            val range: LocalDateRange = selection.cardRange(today, earliest = weights.first().date)
            BodyCard.Range(
                change =
                rangeChange(inUnit, range) ?: RangeChange(start = latestValue, end = latestValue, perWeek = 0.0),
                since = range.start,
                unit = unit,
            )
        }
    }

    /** This many kilograms in [unit] (linear, so it also converts differences and rates). */
    private fun Double.inUnit(unit: MassUnit): Double {
        return Mass.ofKilograms(this).inUnit(unit)
    }

    private companion object {
        /** Reads all history so "latest" never depends on the visible range. */
        val HISTORY_START: LocalDate = LocalDate.fromEpochDays(0)
        const val STOP_TIMEOUT_MILLIS: Long = 5_000L
    }
}

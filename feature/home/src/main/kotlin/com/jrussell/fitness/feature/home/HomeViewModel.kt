package com.jrussell.fitness.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrussell.fitness.core.domain.DayBoundaryRepository
import com.jrussell.fitness.core.domain.UserPreferencesRepository
import com.jrussell.fitness.core.domain.localDateFor
import com.jrussell.fitness.core.domain.metric.Bucket
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.domain.metric.MetricIds
import com.jrussell.fitness.core.domain.metric.MetricRegistry
import com.jrussell.fitness.core.domain.metric.RangeChange
import com.jrussell.fitness.core.domain.metric.rangeChange
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.domain.range.cardRange
import com.jrussell.fitness.core.domain.time.WallClock
import com.jrussell.fitness.core.model.Mass
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.toLocalDateTime

/**
 * Home's state: the selected range and one card per domain with data. Read-only by design
 * (decision #38): Home summarizes, the tabs edit. Values are converted to the user's unit here.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val metrics: MetricRegistry,
    private val preferences: UserPreferencesRepository,
    private val dayBoundary: DayBoundaryRepository,
    private val clock: WallClock,
) : ViewModel() {
    private val selection: MutableStateFlow<DateRangeSelection> = MutableStateFlow(DateRangeSelection.Today)

    /** `null` until the first read, so Home never flashes an empty state before data arrives. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState?> = today().flatMapLatest { day: LocalDate -> stateFor(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    fun onRangeSelected(selection: DateRangeSelection) {
        this.selection.value = selection
    }

    /** Today's date after the user's day start (a 01:30 visit with a 04:00 start is still yesterday). */
    private fun today(): Flow<LocalDate> {
        val calendarDate: LocalDate = clock.now().toLocalDateTime(clock.timeZone()).date
        return dayBoundary.observeDayStartMinutes(calendarDate)
            .map { dayStartMinutes: Int -> localDateFor(clock.now(), clock.timeZone(), dayStartMinutes) }
    }

    private fun stateFor(today: LocalDate): Flow<HomeUiState> {
        val history: LocalDateRange = HISTORY_START..today
        return combine(
            selection,
            preferences.userPreferences,
            metrics.series(MetricIds.BODY_WEIGHT, history, Bucket.DAY),
            metrics.series(MetricIds.BODY_TREND_WEIGHT, history, Bucket.DAY),
        ) { selected: DateRangeSelection, prefs: UserPreferences, weights: List<DataPoint>, trend: List<DataPoint> ->
            HomeUiState(
                today = today,
                selection = selected,
                body = bodyCard(selected, today, prefs.massUnit, weights, trend),
            )
        }
    }

    private fun bodyCard(
        selection: DateRangeSelection,
        today: LocalDate,
        unit: MassUnit,
        weights: List<DataPoint>,
        trend: List<DataPoint>,
    ): BodyCard? {
        val latest: DataPoint = weights.lastOrNull() ?: return null
        return if (selection == DateRangeSelection.Today) {
            BodyCard.Today(
                latest = latest.value.inUnit(unit),
                latestDate = latest.date,
                trend = (trend.lastOrNull()?.value ?: latest.value).inUnit(unit),
                unit = unit,
            )
        } else {
            val change: RangeChange? = rangeChange(trend, selection.cardRange(today))
            BodyCard.Range(
                change = change?.let { kilograms: RangeChange ->
                    RangeChange(
                        start = kilograms.start.inUnit(unit),
                        end = kilograms.end.inUnit(unit),
                        perWeek = kilograms.perWeek.inUnit(unit),
                    )
                },
                unit = unit,
            )
        }
    }

    /** This many kilograms in [unit] (linear, so it also converts differences and rates). */
    private fun Double.inUnit(unit: MassUnit): Double {
        return Mass.ofKilograms(this).inUnit(unit)
    }

    private companion object {
        /** Reads all history so trend weight and "latest" never depend on the visible range. */
        val HISTORY_START: LocalDate = LocalDate.fromEpochDays(0)
        const val STOP_TIMEOUT_MILLIS: Long = 5_000L
    }
}

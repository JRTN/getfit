package com.jrussell.fitness.feature.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrussell.fitness.core.domain.DayBoundaryRepository
import com.jrussell.fitness.core.domain.ManualMeasurement
import com.jrussell.fitness.core.domain.MeasurementRepository
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.domain.UserPreferencesRepository
import com.jrussell.fitness.core.domain.metric.Bucket
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.domain.metric.MetricIds
import com.jrussell.fitness.core.domain.metric.MetricRegistry
import com.jrussell.fitness.core.domain.metric.linearTrend
import com.jrussell.fitness.core.domain.observeToday
import com.jrussell.fitness.core.domain.range.DateRangeRepository
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.domain.range.RangeScreen
import com.jrussell.fitness.core.domain.range.chartRange
import com.jrussell.fitness.core.domain.time.WallClock
import com.jrussell.fitness.core.model.Mass
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.model.Measurement
import com.jrussell.fitness.core.model.MeasurementTypeKeys
import com.jrussell.fitness.core.model.UserPreferences
import com.jrussell.fitness.core.model.ids.MeasurementId
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * The Body tab: log weigh-ins (add, history, delete) and chart them. Log and Trends share one
 * date range, remembered across restarts ([RangeScreen.BODY], All by default); it decides the
 * history shown, the latest-weight card's rate and what the chart spans. Typed
 * weights are parsed in the user's unit and stored in kilograms; everything shown is converted
 * back to the user's unit here, so switching units never touches stored data.
 */
@HiltViewModel
class BodyViewModel @Inject constructor(
    private val measurements: MeasurementRepository,
    private val metrics: MetricRegistry,
    private val preferences: UserPreferencesRepository,
    private val ranges: DateRangeRepository,
    private val dayBoundary: DayBoundaryRepository,
    private val clock: WallClock,
) : ViewModel() {
    private val entry: MutableStateFlow<WeightEntryState?> = MutableStateFlow(null)

    /** The save in progress, if any; further saves are ignored until it finishes. */
    private var saving: Job? = null

    /** `null` until the first read, so the tab never flashes an empty state before data arrives. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BodyUiState?> = observeToday(clock, dayBoundary).flatMapLatest { day: LocalDate ->
        stateFor(day)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    fun onRangeSelected(selection: DateRangeSelection) {
        viewModelScope.launch {
            ranges.setSelection(RangeScreen.BODY, selection)
        }
    }

    /** Opens the add-weight dialog with no error showing. */
    fun onAddWeight() {
        entry.value = WeightEntryState()
    }

    fun onDismissEntry() {
        entry.value = null
    }

    /** The user changed the typed weight: hide the previous error until they save again. */
    fun onEntryEdited() {
        entry.value = entry.value?.copy(error = null)
    }

    /**
     * Saves [text] as a weigh-in in the user's unit. [pickedDate] and [pickedTime] are `null`
     * unless the user changed them in the dialog. A picked date files the reading under that day
     * even if the time is before the day start; otherwise the day-start rule decides. A time in
     * the future is saved as now. An invalid weight leaves the dialog open with a field error. A call
     * made while a save is still running is ignored.
     */
    fun onSaveWeight(
        text: String,
        pickedDate: LocalDate?,
        pickedTime: LocalTime?,
        locale: Locale = Locale.getDefault(),
    ) {
        // Keyboard Done followed by Save (or a double tap) must not store the weigh-in twice.
        if (saving?.isActive == true) {
            return
        }
        saving = viewModelScope.launch {
            val unit: MassUnit = preferences.userPreferences.first().massUnit
            when (val parsed: WeightInputResult = parseWeightInput(text, unit, locale)) {
                is WeightInputResult.Invalid -> entry.value = WeightEntryState(error = parsed.reason)
                is WeightInputResult.Valid -> save(parsed, unit, pickedDate, pickedTime)
            }
        }
    }

    fun onDelete(id: MeasurementId) {
        viewModelScope.launch {
            // A NotFound failure means it is already gone, which is what the user asked for.
            measurements.delete(id)
        }
    }

    private suspend fun save(parsed: WeightInputResult.Valid, unit: MassUnit, date: LocalDate?, time: LocalTime?) {
        val zone: TimeZone = clock.timeZone()
        val now: Instant = clock.now()
        val takenAt: Instant = if (date == null && time == null) {
            now
        } else {
            val nowLocal: LocalDateTime = now.toLocalDateTime(zone)
            minOf(LocalDateTime(date ?: nowLocal.date, time ?: nowLocal.time).toInstant(zone), now)
        }
        val outcome: Outcome<MeasurementId> = measurements.addManual(
            ManualMeasurement(
                typeKey = MeasurementTypeKeys.WEIGHT,
                value = parsed.mass.kilograms,
                takenAt = takenAt,
                timeZone = zone,
                enteredValue = parsed.entered,
                enteredUnit = unit.storageSymbol(),
                note = null,
                localDateOverride = date,
            ),
        )
        if (outcome is Outcome.Success) {
            entry.value = null
        }
    }

    private fun stateFor(today: LocalDate): Flow<BodyUiState> {
        val allTime: LocalDateRange = HISTORY_START..today
        val controls: Flow<Controls> = combine(
            ranges.selection(RangeScreen.BODY),
            entry,
            preferences.userPreferences,
        ) { selected: DateRangeSelection, dialog: WeightEntryState?, prefs: UserPreferences ->
            Controls(selected, dialog, prefs.massUnit)
        }
        return combine(
            controls,
            measurements.observeMeasurements(MeasurementTypeKeys.WEIGHT, HISTORY_START, today),
            metrics.series(MetricIds.BODY_WEIGHT, allTime, Bucket.DAY),
        ) { controls: Controls, readings: List<Measurement>, weights: List<DataPoint> ->
            val unit: MassUnit = controls.unit
            // One range for the whole tab: the Log's history and rate and the Trends chart.
            val range: LocalDateRange = controls.selection.chartRange(today, earliest = weights.firstOrNull()?.date)
            val inRange: List<DataPoint> = weights.inRange(range, unit)
            BodyUiState(
                today = today,
                unit = unit,
                latest = weights.lastOrNull()?.let { point: DataPoint ->
                    LatestWeight(
                        value = point.value.inUnit(unit),
                        date = point.date,
                        perWeek = linearTrend(inRange)?.perWeek ?: 0.0,
                        since = range.start,
                    )
                },
                history = readings
                    .filter { reading: Measurement -> reading.localDate in range }
                    .sortedWith(compareByDescending<Measurement> { it.localDate }.thenByDescending { it.startTime })
                    .map { reading: Measurement -> reading.toRow(unit) },
                selection = controls.selection,
                chartRange = range,
                chartWeighIns = inRange,
                entry = controls.entry,
            )
        }
    }

    private data class Controls(val selection: DateRangeSelection, val entry: WeightEntryState?, val unit: MassUnit)

    private companion object {
        /** Reads all history so "latest" and the All range never depend on the visible range. */
        val HISTORY_START: LocalDate = LocalDate.fromEpochDays(0)
        const val STOP_TIMEOUT_MILLIS: Long = 5_000L
    }
}

private fun Measurement.toRow(unit: MassUnit): WeightRow {
    return WeightRow(id, localDate, startTime.toLocalDateTime(timeZone).time, value.inUnit(unit))
}

private fun List<DataPoint>.inRange(range: LocalDateRange, unit: MassUnit): List<DataPoint> {
    return filter { point: DataPoint -> point.date in range }
        .map { point: DataPoint -> point.copy(value = point.value.inUnit(unit)) }
}

/** This many kilograms in [unit]. */
private fun Double.inUnit(unit: MassUnit): Double {
    return Mass.ofKilograms(this).inUnit(unit)
}

/** The unit recorded with a typed value (`Measurement.enteredUnit`); not translated. */
private fun MassUnit.storageSymbol(): String {
    return when (this) {
        MassUnit.POUND -> "lb"
        MassUnit.KILOGRAM -> "kg"
    }
}

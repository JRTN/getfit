package com.jrussell.fitness.feature.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrussell.fitness.core.domain.DayBoundaryRepository
import com.jrussell.fitness.core.domain.ManualMeasurement
import com.jrussell.fitness.core.domain.MeasurementRepository
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.domain.UserPreferencesRepository
import com.jrussell.fitness.core.domain.localDateFor
import com.jrussell.fitness.core.domain.metric.Bucket
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.domain.metric.MetricIds
import com.jrussell.fitness.core.domain.metric.MetricRegistry
import com.jrussell.fitness.core.domain.range.DateRangeSelection
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * The Body tab: log weigh-ins (add, history, delete) and chart them with trend weight. Typed
 * weights are parsed in the user's unit and stored in kilograms; everything shown is converted
 * back to the user's unit here, so switching units never touches stored data.
 */
@HiltViewModel
class BodyViewModel @Inject constructor(
    private val measurements: MeasurementRepository,
    private val metrics: MetricRegistry,
    private val preferences: UserPreferencesRepository,
    private val dayBoundary: DayBoundaryRepository,
    private val clock: WallClock,
) : ViewModel() {
    private val selection: MutableStateFlow<DateRangeSelection> = MutableStateFlow(DateRangeSelection.Today)
    private val entry: MutableStateFlow<WeightEntryState?> = MutableStateFlow(null)

    /** `null` until the first read, so the tab never flashes an empty state before data arrives. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BodyUiState?> = today().flatMapLatest { day: LocalDate -> stateFor(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    fun onRangeSelected(selection: DateRangeSelection) {
        this.selection.value = selection
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
     * the future is saved as now. An invalid weight leaves the dialog open with a field error.
     */
    fun onSaveWeight(
        text: String,
        pickedDate: LocalDate?,
        pickedTime: LocalTime?,
        locale: Locale = Locale.getDefault(),
    ) {
        viewModelScope.launch {
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

    /** Today's date after the user's day start (a 01:30 visit with a 04:00 start is still yesterday). */
    private fun today(): Flow<LocalDate> {
        val calendarDate: LocalDate = clock.now().toLocalDateTime(clock.timeZone()).date
        return dayBoundary.observeDayStartMinutes(calendarDate)
            .map { dayStartMinutes: Int -> localDateFor(clock.now(), clock.timeZone(), dayStartMinutes) }
    }

    private fun stateFor(today: LocalDate): Flow<BodyUiState> {
        val allTime: LocalDateRange = HISTORY_START..today
        val controls: Flow<Controls> = combine(selection, entry, preferences.userPreferences) {
                selected: DateRangeSelection,
                dialog: WeightEntryState?,
                prefs: UserPreferences,
            ->
            Controls(selected, dialog, prefs.massUnit)
        }
        return combine(
            controls,
            measurements.observeMeasurements(
                MeasurementTypeKeys.WEIGHT,
                today.minus(DatePeriod(days = HISTORY_DAYS - 1)),
                today,
            ),
            metrics.series(MetricIds.BODY_WEIGHT, allTime, Bucket.DAY),
            metrics.series(MetricIds.BODY_TREND_WEIGHT, allTime, Bucket.DAY),
        ) { controls: Controls, readings: List<Measurement>, weights: List<DataPoint>, trend: List<DataPoint> ->
            val unit: MassUnit = controls.unit
            val chartRange: LocalDateRange = controls.selection.chartRange(today)
            BodyUiState(
                today = today,
                unit = unit,
                latest = weights.lastOrNull()?.let { point: DataPoint ->
                    LatestWeight(point.value.inUnit(unit), point.date)
                },
                history = readings
                    .sortedWith(compareByDescending<Measurement> { it.localDate }.thenByDescending { it.startTime })
                    .map { reading: Measurement -> reading.toRow(unit) },
                selection = controls.selection,
                chartRange = chartRange,
                chartWeighIns = weights.inRange(chartRange, unit),
                chartTrend = trend.inRange(chartRange, unit),
                entry = controls.entry,
            )
        }
    }

    private data class Controls(val selection: DateRangeSelection, val entry: WeightEntryState?, val unit: MassUnit)

    private companion object {
        /** Reads all history so trend weight and "latest" never depend on the visible range. */
        val HISTORY_START: LocalDate = LocalDate.fromEpochDays(0)

        /** How far back the weigh-in list goes. */
        const val HISTORY_DAYS: Int = 90
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

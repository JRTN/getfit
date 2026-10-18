package com.jrussell.fitness.feature.body

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isCloseTo
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.jrussell.fitness.core.domain.ManualMeasurement
import com.jrussell.fitness.core.domain.MeasurementRepository
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.domain.metric.MetricId
import com.jrussell.fitness.core.domain.metric.MetricIds
import com.jrussell.fitness.core.model.Mass
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.model.Measurement
import com.jrussell.fitness.core.model.UserPreferences
import com.jrussell.fitness.core.model.ids.MeasurementId
import com.jrussell.fitness.core.testing.FakeDayBoundaryRepository
import com.jrussell.fitness.core.testing.FakeMeasurementRepository
import com.jrussell.fitness.core.testing.FakeMetricRegistry
import com.jrussell.fitness.core.testing.FakeUserPreferencesRepository
import com.jrussell.fitness.core.testing.FakeWallClock
import com.jrussell.fitness.core.testing.MainDispatcherRule
import java.util.Locale
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Rule
import org.junit.Test

class BodyViewModelTest {
    @get:Rule
    val mainDispatcherRule: MainDispatcherRule = MainDispatcherRule()

    private val toronto: TimeZone = TimeZone.of("America/Toronto")
    private val today: LocalDate = LocalDate(2026, 10, 17)
    private val clock: FakeWallClock = FakeWallClock(Instant.parse("2026-10-17T12:00:00Z"), toronto)
    private val measurements: RecordingMeasurements =
        RecordingMeasurements(FakeMeasurementRepository(dayStartMinutes = 240))
    private val weights: MutableStateFlow<List<DataPoint>> = MutableStateFlow(emptyList())
    private val metrics: FakeMetricRegistry = FakeMetricRegistry(
        series = mutableMapOf<MetricId, MutableStateFlow<List<DataPoint>>>(
            MetricIds.BODY_WEIGHT to weights,
        ),
    )
    private val preferences: FakeUserPreferencesRepository =
        FakeUserPreferencesRepository(UserPreferences.DEFAULT.copy(massUnit = MassUnit.POUND))

    @Test
    fun onSaveWeight_valid_addsKilogramsWithEnteredUnit() = runTest {
        val viewModel: BodyViewModel = loadedViewModel()

        viewModel.onSaveWeight("181.7", pickedDate = null, pickedTime = null, locale = Locale.US)

        val saved: Measurement = measurements.delegate.measurements.single()
        assertThat(saved.value).isCloseTo(82.417733629, 1e-9)
        assertThat(saved.enteredValue).isEqualTo(181.7)
        assertThat(saved.enteredUnit).isEqualTo("lb")
        assertThat(saved.startTime).isEqualTo(clock.now)
        assertThat(saved.timeZone).isEqualTo(toronto)
        assertThat(loaded(viewModel).entry).isNull()
    }

    @Test
    fun onSaveWeight_invalid_setsFieldErrorAndSavesNothing() = runTest {
        val viewModel: BodyViewModel = loadedViewModel()
        viewModel.onAddWeight()

        viewModel.onSaveWeight("9999", pickedDate = null, pickedTime = null, locale = Locale.US)

        assertThat(measurements.delegate.measurements).isEmpty()
        assertThat(loaded(viewModel).entry?.error).isEqualTo(WeightInputError.OUT_OF_RANGE)
    }

    @Test
    fun onSaveWeight_pickedDate_passesOverride() = runTest {
        val viewModel: BodyViewModel = loadedViewModel()
        val picked: LocalDate = LocalDate(2026, 10, 10)

        viewModel.onSaveWeight("181.7", pickedDate = picked, pickedTime = LocalTime(1, 30), locale = Locale.US)

        val entry: ManualMeasurement = measurements.added.single()
        assertThat(entry.localDateOverride).isEqualTo(picked)
        assertThat(entry.takenAt).isEqualTo(LocalDateTime(picked, LocalTime(1, 30)).toInstant(toronto))
        // The 04:00 day start would have filed 01:30 under Oct 9; the picked date wins.
        assertThat(measurements.delegate.measurements.single().localDate).isEqualTo(picked)
    }

    @Test
    fun onSaveWeight_defaultNow_passesNoOverride() = runTest {
        val viewModel: BodyViewModel = loadedViewModel()

        viewModel.onSaveWeight("181.7", pickedDate = null, pickedTime = null, locale = Locale.US)

        val entry: ManualMeasurement = measurements.added.single()
        assertThat(entry.localDateOverride).isNull()
        assertThat(entry.takenAt).isEqualTo(clock.now)
    }

    @Test
    fun history_rendersInPreferredUnit() = runTest {
        val viewModel: BodyViewModel = loadedViewModel()
        viewModel.onSaveWeight("181.7", pickedDate = null, pickedTime = null, locale = Locale.US)

        val inPounds: BodyUiState = loaded(viewModel)
        assertThat(inPounds.history).hasSize(1)
        assertThat(inPounds.history.single().value).isCloseTo(181.7, 1e-9)
        assertThat(inPounds.history.single().date).isEqualTo(today)
        assertThat(inPounds.history.single().time).isEqualTo(LocalTime(8, 0))

        preferences.setMassUnit(MassUnit.KILOGRAM)

        val inKilograms: BodyUiState = viewModel.uiState.filterNotNull().first { it.unit == MassUnit.KILOGRAM }
        assertThat(inKilograms.history.single().value).isCloseTo(82.417733629, 1e-9)
        assertThat(measurements.delegate.measurements.single().value).isCloseTo(82.417733629, 1e-9)
    }

    @Test
    fun trends_todaySelection_chartsLastSevenDays() = runTest {
        weights.value = listOf(point(10, 9, 83.0), point(10, 11, 82.5), point(10, 17, 82.0))

        val state: BodyUiState = loaded(loadedViewModel())

        assertThat(state.chartRange).isEqualTo(LocalDate(2026, 10, 11)..today)
        assertThat(state.chartWeighIns.map { it.date }).containsExactly(LocalDate(2026, 10, 11), today)
        assertThat(state.chartWeighIns.last().value).isCloseTo(Mass.ofKilograms(82.0).toPounds(), 1e-9)
        assertThat(state.latest?.date).isEqualTo(today)
    }

    @Test
    fun latest_ratePerWeek_isBestFitOverLastThirtyDays() = runTest {
        weights.value = listOf(point(9, 1, 95.0), point(10, 3, 83.0), point(10, 17, 82.0))

        val latest: LatestWeight? = loaded(loadedViewModel()).latest

        // Sep 1 is outside Sep 18..Oct 17; Oct 3 → Oct 17 is -1 kg in 14 days, -0.5 kg per week.
        assertThat(latest?.perWeek).isNotNull().isCloseTo(Mass.ofKilograms(-0.5).toPounds(), 1e-9)
    }

    @Test
    fun latest_singleWeighIn_rateIsZero() = runTest {
        weights.value = listOf(point(10, 17, 82.0))

        assertThat(loaded(loadedViewModel()).latest?.perWeek).isEqualTo(0.0)
    }

    @Test
    fun today_movesToTheNextDayOnceTheDayStartPasses() = runTest {
        val viewModel: BodyViewModel = loadedViewModel()
        assertThat(loaded(viewModel).today).isEqualTo(today)

        // 05:00 in Toronto on Oct 18, past the 04:00 day start.
        clock.now = Instant.parse("2026-10-18T09:00:00Z")

        val next: BodyUiState = viewModel.uiState.filterNotNull().first { it.today != today }
        assertThat(next.today).isEqualTo(LocalDate(2026, 10, 18))
    }

    @Test
    fun emptyHistory_hasNoLatestAndEmptyChart() = runTest {
        val state: BodyUiState = loaded(loadedViewModel())

        assertThat(state.latest).isNull()
        assertThat(state.history).isEmpty()
        assertThat(state.chartWeighIns).isEmpty()
        assertThat(state.today).isEqualTo(today)
    }

    @Test
    fun onDelete_removesTheReading() = runTest {
        val viewModel: BodyViewModel = loadedViewModel()
        viewModel.onSaveWeight("181.7", pickedDate = null, pickedTime = null, locale = Locale.US)
        val id: MeasurementId = loaded(viewModel).history.single().id

        viewModel.onDelete(id)

        assertThat(measurements.delegate.measurements).isEmpty()
        assertThat(viewModel.uiState.filterNotNull().first { it.history.isEmpty() }.history).isEmpty()
    }

    @Test
    fun onSaveWeight_futureTimeToday_clampsToNow() = runTest {
        val viewModel: BodyViewModel = loadedViewModel()

        viewModel.onSaveWeight("181.7", pickedDate = today, pickedTime = LocalTime(23, 0), locale = Locale.US)

        assertThat(measurements.added.single().takenAt).isEqualTo(clock.now)
    }

    private fun loadedViewModel(): BodyViewModel {
        return BodyViewModel(measurements, metrics, preferences, FakeDayBoundaryRepository(240), clock)
    }

    private suspend fun loaded(viewModel: BodyViewModel): BodyUiState {
        return viewModel.uiState.filterNotNull().first()
    }

    private fun point(month: Int, day: Int, kilograms: Double): DataPoint {
        return DataPoint(LocalDate(2026, month, day), kilograms, coverage = null)
    }

    /** Records every entry handed to [addManual] so tests can check what the ViewModel sent. */
    private class RecordingMeasurements(val delegate: FakeMeasurementRepository) : MeasurementRepository by delegate {
        val added: MutableList<ManualMeasurement> = mutableListOf()

        override suspend fun addManual(entry: ManualMeasurement): Outcome<MeasurementId> {
            added += entry
            return delegate.addManual(entry)
        }
    }
}

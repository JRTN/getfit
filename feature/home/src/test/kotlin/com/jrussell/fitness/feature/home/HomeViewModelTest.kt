package com.jrussell.fitness.feature.home

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.domain.metric.MetricId
import com.jrussell.fitness.core.domain.metric.MetricIds
import com.jrussell.fitness.core.domain.metric.RangeChange
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.model.UserPreferences
import com.jrussell.fitness.core.testing.FakeDayBoundaryRepository
import com.jrussell.fitness.core.testing.FakeMetricRegistry
import com.jrussell.fitness.core.testing.FakeUserPreferencesRepository
import com.jrussell.fitness.core.testing.FakeWallClock
import com.jrussell.fitness.core.testing.MainDispatcherRule
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule: MainDispatcherRule = MainDispatcherRule()

    private val today: LocalDate = LocalDate(2026, 10, 17)
    private val clock: FakeWallClock = FakeWallClock(Instant.parse("2026-10-17T12:00:00Z"), TimeZone.UTC)
    private val weights: MutableStateFlow<List<DataPoint>> = MutableStateFlow(emptyList())
    private val metrics: FakeMetricRegistry = FakeMetricRegistry(
        series = mutableMapOf<MetricId, MutableStateFlow<List<DataPoint>>>(
            MetricIds.BODY_WEIGHT to weights,
        ),
    )
    private val preferences: FakeUserPreferencesRepository =
        FakeUserPreferencesRepository(UserPreferences.DEFAULT.copy(massUnit = MassUnit.KILOGRAM))

    @Test
    fun noWeighIns_hidesBodyCard() = runTest {
        val state: HomeUiState = loaded(newViewModel())

        assertThat(state.body).isNull()
        assertThat(state.today).isEqualTo(today)
    }

    @Test
    fun today_showsLatestAndWeeklyRate() = runTest {
        weights.value = listOf(point(10, 1, 99.0), point(10, 15, 96.0), point(10, 17, 95.0))

        val body: BodyCard? = loaded(newViewModel()).body

        assertThat(body).isNotNull().isInstanceOf<BodyCard.Today>()
        val card: BodyCard.Today = body as BodyCard.Today
        assertThat(card.latest).isCloseTo(95.0, 1e-9)
        assertThat(card.latestDate).isEqualTo(today)
        // Today's rate is the trend over the last 7 days, like Today's charts; Oct 1 is outside.
        assertThat(card.perWeek).isCloseTo(-3.5, 1e-9)
    }

    @Test
    fun today_singleWeighIn_rateIsZero() = runTest {
        weights.value = listOf(point(10, 17, 95.0))

        assertThat((loaded(newViewModel()).body as BodyCard.Today).perWeek).isEqualTo(0.0)
    }

    @Test
    fun rangeThirtyDays_showsChange() = runTest {
        weights.value = listOf(point(9, 1, 100.0), point(9, 19, 99.0), point(10, 3, 97.0), point(10, 17, 98.0))

        val card: BodyCard.Range = rangeCard(DateRangeSelection.LastDays(30))

        assertThat(card.change.start).isCloseTo(99.0, 1e-9)
        assertThat(card.change.end).isCloseTo(98.0, 1e-9)
        assertThat(card.change.perWeek).isCloseTo(-0.25, 1e-9)
    }

    @Test
    fun rangeWithSingleWeighIn_rateIsZero() = runTest {
        weights.value = listOf(point(10, 17, 98.0))

        assertThat(rangeCard(DateRangeSelection.LastDays(7)).change).isEqualTo(RangeChange(98.0, 98.0, 0.0))
    }

    @Test
    fun rangeWithNoWeighInsInside_showsLatestWithZeroRate() = runTest {
        weights.value = listOf(point(9, 1, 100.0))

        assertThat(rangeCard(DateRangeSelection.LastDays(7)).change).isEqualTo(RangeChange(100.0, 100.0, 0.0))
    }

    @Test
    fun today_movesToTheNextDayAfterMidnight() = runTest {
        val viewModel: HomeViewModel = newViewModel()
        assertThat(loaded(viewModel).today).isEqualTo(today)

        clock.now = Instant.parse("2026-10-18T00:01:00Z")

        assertThat(viewModel.uiState.filterNotNull().first { it.today != today }.today)
            .isEqualTo(LocalDate(2026, 10, 18))
    }

    @Test
    fun values_followPreferredUnit() = runTest {
        preferences.setMassUnit(MassUnit.POUND)
        weights.value = listOf(point(10, 17, 95.0))

        val card: BodyCard.Today = loaded(newViewModel()).body as BodyCard.Today

        assertThat(card.unit).isEqualTo(MassUnit.POUND)
        assertThat(card.latest).isCloseTo(209.43914, 1e-5)
    }

    private fun newViewModel(): HomeViewModel {
        return HomeViewModel(metrics, preferences, FakeDayBoundaryRepository(), clock)
    }

    private suspend fun rangeCard(selection: DateRangeSelection): BodyCard.Range {
        val viewModel: HomeViewModel = newViewModel()
        viewModel.onRangeSelected(selection)
        return viewModel.uiState.filterNotNull().first { it.selection == selection }.body as BodyCard.Range
    }

    private suspend fun loaded(viewModel: HomeViewModel): HomeUiState {
        return viewModel.uiState.filterNotNull().first()
    }

    private fun point(month: Int, day: Int, kilograms: Double): DataPoint {
        return DataPoint(LocalDate(2026, month, day), kilograms, coverage = null)
    }
}

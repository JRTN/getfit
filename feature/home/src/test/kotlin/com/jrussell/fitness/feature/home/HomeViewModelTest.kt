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
    private val trend: MutableStateFlow<List<DataPoint>> = MutableStateFlow(emptyList())
    private val metrics: FakeMetricRegistry = FakeMetricRegistry(
        series = mutableMapOf<MetricId, MutableStateFlow<List<DataPoint>>>(
            MetricIds.BODY_WEIGHT to weights,
            MetricIds.BODY_TREND_WEIGHT to trend,
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
    fun today_showsLatestAndTrend() = runTest {
        weights.value = listOf(point(10, 15, 96.0), point(10, 17, 95.0))
        trend.value = listOf(point(10, 15, 96.2), point(10, 17, 96.0))

        val body: BodyCard? = loaded(newViewModel()).body

        assertThat(body).isNotNull().isInstanceOf<BodyCard.Today>()
        val card: BodyCard.Today = body as BodyCard.Today
        assertThat(card.latest).isCloseTo(95.0, 1e-9)
        assertThat(card.latestDate).isEqualTo(today)
        assertThat(card.trend).isCloseTo(96.0, 1e-9)
    }

    @Test
    fun rangeThirtyDays_showsChange() = runTest {
        weights.value = listOf(point(9, 1, 100.0), point(10, 17, 98.0))
        trend.value = listOf(point(9, 1, 100.0), point(10, 17, 98.0))
        val viewModel: HomeViewModel = newViewModel()

        viewModel.onRangeSelected(DateRangeSelection.LastDays(30))
        val state: HomeUiState = viewModel.uiState.filterNotNull().first {
            it.selection ==
                DateRangeSelection.LastDays(30)
        }

        val card: BodyCard.Range = state.body as BodyCard.Range
        assertThat(card.change?.start).isNotNull().isCloseTo(100.0, 1e-9)
        assertThat(card.change?.end).isNotNull().isCloseTo(98.0, 1e-9)
        assertThat(card.change?.perWeek).isNotNull().isCloseTo(-2.0 / 46 * 7, 1e-9)
    }

    @Test
    fun rangeWithSingleWeighIn_showsNotEnoughData() = runTest {
        weights.value = listOf(point(10, 17, 98.0))
        trend.value = listOf(point(10, 17, 98.0))
        val viewModel: HomeViewModel = newViewModel()

        viewModel.onRangeSelected(DateRangeSelection.LastDays(7))
        val state: HomeUiState = viewModel.uiState.filterNotNull().first {
            it.selection ==
                DateRangeSelection.LastDays(7)
        }

        assertThat((state.body as BodyCard.Range).change).isNull()
    }

    @Test
    fun values_followPreferredUnit() = runTest {
        preferences.setMassUnit(MassUnit.POUND)
        weights.value = listOf(point(10, 17, 95.0))
        trend.value = listOf(point(10, 17, 95.0))

        val card: BodyCard.Today = loaded(newViewModel()).body as BodyCard.Today

        assertThat(card.unit).isEqualTo(MassUnit.POUND)
        assertThat(card.latest).isCloseTo(209.43914, 1e-5)
    }

    private fun newViewModel(): HomeViewModel {
        return HomeViewModel(metrics, preferences, FakeDayBoundaryRepository(), clock)
    }

    private suspend fun loaded(viewModel: HomeViewModel): HomeUiState {
        return viewModel.uiState.filterNotNull().first()
    }

    private fun point(month: Int, day: Int, kilograms: Double): DataPoint {
        return DataPoint(LocalDate(2026, month, day), kilograms, coverage = null)
    }
}

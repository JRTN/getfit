package com.jrussell.fitness.core.data.metric

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.jrussell.fitness.core.data.repository.RepositoryTestHarness
import com.jrussell.fitness.core.domain.ManualMeasurement
import com.jrussell.fitness.core.domain.metric.Bucket
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.domain.metric.MetricIds
import com.jrussell.fitness.core.domain.metric.linearTrend
import com.jrussell.fitness.core.model.MeasurementTypeKeys
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The weight series over the real database: a weigh-in saved while a screen is watching reaches
 * the series (and so the chart and the rate) without reopening anything.
 */
@RunWith(RobolectricTestRunner::class)
class BodyMetricProviderLiveTest {
    private val harness: RepositoryTestHarness = RepositoryTestHarness()
    private val provider: BodyMetricProvider = BodyMetricProvider(harness.measurementRepository)

    @After
    fun close() {
        harness.close()
    }

    @Test
    fun series_weighInsSavedWhileWatching_updateTheSeriesAndRate() = runTest {
        weighIn("2026-10-10T07:00", 98.0)

        provider.series(MetricIds.BODY_WEIGHT, LocalDate(2026, 10, 1)..LocalDate(2026, 10, 31), Bucket.DAY).test {
            assertThat(awaitPoints(this, count = 1).single().value).isEqualTo(98.0)

            weighIn("2026-10-17T07:00", 97.0)
            val two: List<DataPoint> = awaitPoints(this, count = 2)
            assertThat(linearTrend(two)?.perWeek).isNotNull().isCloseTo(-1.0, 1e-9)

            weighIn("2026-10-24T07:00", 97.0)
            val three: List<DataPoint> = awaitPoints(this, count = 3)
            assertThat(linearTrend(three)?.perWeek).isNotNull().isCloseTo(-0.5, 1e-9)

            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Skips intermediate emissions until the series has [count] points. */
    private suspend fun awaitPoints(turbine: ReceiveTurbine<List<DataPoint>>, count: Int): List<DataPoint> {
        var points: List<DataPoint> = turbine.awaitItem()
        while (points.size != count) {
            points = turbine.awaitItem()
        }
        return points
    }

    private suspend fun weighIn(localDateTime: String, kilograms: Double) {
        harness.measurementRepository.addManual(
            ManualMeasurement(
                typeKey = MeasurementTypeKeys.WEIGHT,
                value = kilograms,
                takenAt = LocalDateTime.parse(localDateTime).toInstant(TimeZone.UTC),
                timeZone = TimeZone.UTC,
                enteredValue = null,
                enteredUnit = null,
                note = null,
            ),
        )
    }
}

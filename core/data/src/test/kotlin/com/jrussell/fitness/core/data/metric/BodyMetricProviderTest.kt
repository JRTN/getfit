package com.jrussell.fitness.core.data.metric

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isCloseTo
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.jrussell.fitness.core.domain.ManualMeasurement
import com.jrussell.fitness.core.domain.metric.Bucket
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.domain.metric.MetricIds
import com.jrussell.fitness.core.domain.metric.trendWeight
import com.jrussell.fitness.core.model.MeasurementTypeKeys
import com.jrussell.fitness.core.testing.FakeMeasurementRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test

class BodyMetricProviderTest {
    private val repository: FakeMeasurementRepository = FakeMeasurementRepository()
    private val provider: BodyMetricProvider = BodyMetricProvider(repository)

    @Test
    fun series_sameDay_usesLast() = runTest {
        weighIn("2026-10-17T08:00", 82.0)
        weighIn("2026-10-17T20:00", 83.0)
        val day: LocalDate = LocalDate(2026, 10, 17)

        val points: List<DataPoint> = provider.series(MetricIds.BODY_WEIGHT, day..day, Bucket.DAY).first()

        assertThat(points).containsExactly(DataPoint(day, 83.0, coverage = null))
    }

    @Test
    fun trend_rangeStartsMidHistory_isWarm() = runTest {
        val weights: List<Double> =
            listOf(84.0, 83.6, 83.9, 83.1, 83.4, 82.8, 83.0, 82.5, 82.9, 82.2, 82.6, 82.0, 82.3, 81.9, 82.1)
        weights.forEachIndexed { index: Int, weight: Double ->
            weighIn("2026-10-${(index + 1).toString().padStart(2, '0')}T07:00", weight)
        }
        val fullHistory: List<DataPoint> = trendWeight(
            weights.mapIndexed { index: Int, weight: Double ->
                DataPoint(LocalDate(2026, 10, index + 1), weight, null)
            },
        )

        val points: List<DataPoint> =
            provider.series(
                MetricIds.BODY_TREND_WEIGHT,
                LocalDate(2026, 10, 10)..LocalDate(2026, 10, 15),
                Bucket.DAY,
            ).first()

        assertThat(points.first().date).isEqualTo(LocalDate(2026, 10, 10))
        assertThat(points.first().value).isCloseTo(fullHistory[9].value, 1e-12)
        assertThat(points.size).isEqualTo(6)
    }

    @Test
    fun series_emptyHistory_isEmpty() = runTest {
        val range = LocalDate(2026, 10, 1)..LocalDate(2026, 10, 31)

        assertThat(provider.series(MetricIds.BODY_WEIGHT, range, Bucket.DAY).first()).isEmpty()
        assertThat(provider.series(MetricIds.BODY_TREND_WEIGHT, range, Bucket.DAY).first()).isEmpty()
    }

    private suspend fun weighIn(localDateTime: String, kilograms: Double) {
        repository.addManual(
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

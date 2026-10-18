package com.jrussell.fitness.core.domain.metric

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

class TrendWeightTest {
    @Test
    fun trendWeight_firstPoint_equalsWeight() {
        val trend: List<DataPoint> = trendWeight(listOf(point(0, 83.2)))

        assertThat(trend.single().value).isEqualTo(83.2)
    }

    @Test
    fun trendWeight_consecutiveDays_usesAlpha() {
        val trend: List<DataPoint> = trendWeight(listOf(point(0, 100.0), point(1, 101.0)))

        assertThat(trend.map { p: DataPoint -> p.value }[0]).isCloseTo(100.0, 1e-12)
        assertThat(trend.map { p: DataPoint -> p.value }[1]).isCloseTo(100.1, 1e-12)
    }

    @Test
    fun trendWeight_gap_usesEffectiveAlpha() {
        val trend: List<DataPoint> = trendWeight(listOf(point(0, 100.0), point(5, 105.0)))

        assertThat(trend[1].value).isCloseTo(102.04755, 1e-5)
        assertThat(trend[1].date).isEqualTo(START.plusDays(5))
    }

    @Test
    fun trendWeight_empty_isEmpty() {
        assertThat(trendWeight(emptyList())).isEmpty()
    }

    private fun point(dayOffset: Int, value: Double): DataPoint {
        return DataPoint(START.plusDays(dayOffset), value, coverage = null)
    }

    private fun LocalDate.plusDays(days: Int): LocalDate {
        return LocalDate.fromEpochDays(toEpochDays() + days)
    }

    private companion object {
        val START: LocalDate = LocalDate(2026, 10, 1)
    }
}

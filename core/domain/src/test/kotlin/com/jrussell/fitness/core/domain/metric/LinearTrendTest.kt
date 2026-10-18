package com.jrussell.fitness.core.domain.metric

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

class LinearTrendTest {
    @Test
    fun linearTrend_twoPoints_passesThroughBoth() {
        val trend: LinearTrend? = linearTrend(listOf(point(10, 10, 216.0), point(10, 17, 212.4)))

        assertThat(trend).isNotNull()
        assertThat(trend!!.valueAt(LocalDate(2026, 10, 10))).isCloseTo(216.0, 1e-9)
        assertThat(trend.valueAt(LocalDate(2026, 10, 17))).isCloseTo(212.4, 1e-9)
        assertThat(trend.perWeek).isCloseTo(-3.6, 1e-9)
    }

    @Test
    fun linearTrend_threePoints_isLeastSquaresFit() {
        val trend: LinearTrend? = linearTrend(listOf(point(10, 1, 100.0), point(10, 2, 99.0), point(10, 3, 99.5)))

        assertThat(trend).isNotNull()
        assertThat(trend!!.perDay).isCloseTo(-0.25, 1e-9)
        assertThat(trend.valueAt(LocalDate(2026, 10, 1))).isCloseTo(99.75, 1e-9)
        assertThat(trend.valueAt(LocalDate(2026, 10, 3))).isCloseTo(99.25, 1e-9)
    }

    @Test
    fun linearTrend_unevenGaps_weighsByDate() {
        // Same three values, but the middle weigh-in sits next to the last: x = 0, 6, 7.
        val trend: LinearTrend? = linearTrend(listOf(point(10, 1, 100.0), point(10, 7, 99.0), point(10, 8, 99.5)))

        assertThat(trend?.perDay).isNotNull().isCloseTo(-9.0 / 86.0, 1e-9)
    }

    @Test
    fun linearTrend_singlePoint_isNull() {
        assertThat(linearTrend(listOf(point(10, 5, 100.0)))).isNull()
    }

    @Test
    fun linearTrend_empty_isNull() {
        assertThat(linearTrend(emptyList())).isNull()
    }

    private fun point(month: Int, day: Int, value: Double): DataPoint {
        return DataPoint(LocalDate(2026, month, day), value, coverage = null)
    }
}

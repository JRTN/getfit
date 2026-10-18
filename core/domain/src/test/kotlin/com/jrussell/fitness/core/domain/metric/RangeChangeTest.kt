package com.jrussell.fitness.core.domain.metric

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

class RangeChangeTest {
    @Test
    fun rangeChange_usesPointBeforeRangeStart() {
        val trend: List<DataPoint> = listOf(point(9, 30, 101.0), point(10, 8, 100.0))

        val change: RangeChange? = rangeChange(trend, LocalDate(2026, 10, 1)..LocalDate(2026, 10, 15))

        assertThat(change).isNotNull()
        assertThat(change!!.start).isCloseTo(101.0, 1e-12)
        assertThat(change.end).isCloseTo(100.0, 1e-12)
    }

    @Test
    fun rangeChange_perWeek() {
        val trend: List<DataPoint> = listOf(point(10, 1, 100.0), point(10, 15, 99.0))

        val change: RangeChange? = rangeChange(trend, LocalDate(2026, 10, 1)..LocalDate(2026, 10, 15))

        assertThat(change?.perWeek).isNotNull().isCloseTo(-0.5, 1e-12)
    }

    @Test
    fun rangeChange_singlePoint_isNull() {
        assertThat(rangeChange(listOf(point(10, 5, 100.0)), LocalDate(2026, 10, 1)..LocalDate(2026, 10, 15))).isNull()
    }

    @Test
    fun rangeChange_noPoints_isNull() {
        assertThat(rangeChange(emptyList(), LocalDate(2026, 10, 1)..LocalDate(2026, 10, 15))).isNull()
    }

    private fun point(month: Int, day: Int, value: Double): DataPoint {
        return DataPoint(LocalDate(2026, month, day), value, coverage = null)
    }
}

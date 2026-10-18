package com.jrussell.fitness.core.domain.metric

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

class RangeChangeTest {
    @Test
    fun rangeChange_firstAndLastWeighInsInRange() {
        val weighIns: List<DataPoint> = listOf(point(9, 30, 105.0), point(10, 2, 101.0), point(10, 8, 100.0))

        val change: RangeChange? = rangeChange(weighIns, LocalDate(2026, 10, 1)..LocalDate(2026, 10, 15))

        assertThat(change).isNotNull()
        assertThat(change!!.start).isCloseTo(101.0, 1e-12)
        assertThat(change.end).isCloseTo(100.0, 1e-12)
    }

    @Test
    fun rangeChange_perWeek_isBestFitSlope() {
        val weighIns: List<DataPoint> =
            listOf(point(10, 1, 100.0), point(10, 2, 98.0), point(10, 3, 98.0), point(10, 4, 99.0))

        val change: RangeChange? = rangeChange(weighIns, LocalDate(2026, 10, 1)..LocalDate(2026, 10, 15))

        // The first and last alone would say -2.33 per week; the fit through all four says -0.3 per day.
        assertThat(change?.perWeek).isNotNull().isCloseTo(-2.1, 1e-9)
    }

    @Test
    fun rangeChange_singleWeighIn_isZeroPerWeek() {
        val change: RangeChange? = rangeChange(
            listOf(point(10, 5, 100.0)),
            LocalDate(2026, 10, 1)..LocalDate(2026, 10, 15),
        )

        assertThat(change).isEqualTo(RangeChange(start = 100.0, end = 100.0, perWeek = 0.0))
    }

    @Test
    fun rangeChange_noWeighInsInRange_isNull() {
        assertThat(rangeChange(listOf(point(9, 20, 100.0)), LocalDate(2026, 10, 1)..LocalDate(2026, 10, 15))).isNull()
    }

    private fun point(month: Int, day: Int, value: Double): DataPoint {
        return DataPoint(LocalDate(2026, month, day), value, coverage = null)
    }
}

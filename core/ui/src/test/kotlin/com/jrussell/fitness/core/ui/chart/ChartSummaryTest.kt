package com.jrussell.fitness.core.ui.chart

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.jrussell.fitness.core.domain.metric.DataPoint
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import org.junit.Test

class ChartSummaryTest {
    private val thirtyDays: LocalDateRange = LocalDate(2026, 9, 18)..LocalDate(2026, 10, 17)

    @Test
    fun summary_thirtyDays_firstLastPerWeek() {
        val points: List<DataPoint> = listOf(point(9, 19, 212.4), point(10, 3, 211.0), point(10, 17, 209.9))

        val summary: ChartSummaryData? = chartSummaryData(points, thirtyDays)

        assertThat(summary).isNotNull()
        assertThat(summary!!.spanDays).isEqualTo(30)
        assertThat(summary.first).isEqualTo(212.4)
        assertThat(summary.last).isEqualTo(209.9)
        assertThat(summary.perWeek).isNotNull().isCloseTo(-0.625, 1e-9)
    }

    @Test
    fun summary_noPoints_isNull() {
        assertThat(chartSummaryData(emptyList(), thirtyDays)).isNull()
    }

    @Test
    fun summary_singlePoint_hasNullPerWeek() {
        val summary: ChartSummaryData? = chartSummaryData(listOf(point(10, 17, 209.9)), thirtyDays)

        assertThat(summary?.perWeek).isNull()
        assertThat(summary?.last).isEqualTo(209.9)
    }

    private fun point(month: Int, day: Int, value: Double): DataPoint {
        return DataPoint(LocalDate(2026, month, day), value, coverage = null)
    }
}

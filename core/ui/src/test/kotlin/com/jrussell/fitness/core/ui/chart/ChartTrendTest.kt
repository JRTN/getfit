package com.jrussell.fitness.core.ui.chart

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isCloseTo
import assertk.assertions.isEmpty
import com.jrussell.fitness.core.domain.metric.DataPoint
import kotlinx.datetime.LocalDate
import org.junit.Test

class ChartTrendTest {
    @Test
    fun trendLine_runsFromFirstToLastDateAlongTheBestFit() {
        val points: List<DataPoint> = listOf(point(10, 11, 82.5), point(10, 14, 81.0), point(10, 17, 82.0))

        val line: List<DataPoint> = trendLine(points)

        // Best fit through the three: -1/12 per day, 81.8333 at the middle date.
        assertThat(line.map { it.date }).containsExactly(LocalDate(2026, 10, 11), LocalDate(2026, 10, 17))
        assertThat(line.first().value).isCloseTo(82.0 + 1.0 / 12.0, 1e-9)
        assertThat(line.last().value).isCloseTo(82.0 - 5.0 / 12.0, 1e-9)
    }

    @Test
    fun trendLine_singleDate_isEmpty() {
        assertThat(trendLine(listOf(point(10, 17, 82.0)))).isEmpty()
    }

    private fun point(month: Int, day: Int, value: Double): DataPoint {
        return DataPoint(LocalDate(2026, month, day), value, coverage = null)
    }
}

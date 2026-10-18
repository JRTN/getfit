package com.jrussell.fitness.core.ui.chart

import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.domain.metric.LinearTrend
import com.jrussell.fitness.core.domain.metric.linearTrend
import kotlinx.datetime.LocalDate

/**
 * A chart's trend line for [points] (ascending by date): the least-squares line ([linearTrend]),
 * drawn from the first date to the last so it never extends past the data. Empty when the points
 * fall on fewer than two dates.
 */
internal fun trendLine(points: List<DataPoint>): List<DataPoint> {
    val trend: LinearTrend = linearTrend(points) ?: return emptyList()
    return listOf(points.first().date, points.last().date).map { date: LocalDate ->
        DataPoint(date, trend.valueAt(date), coverage = null)
    }
}

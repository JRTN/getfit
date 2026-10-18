package com.jrussell.fitness.core.ui.chart

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.ui.R
import kotlin.math.abs
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.daysUntil

private const val DAYS_PER_WEEK: Double = 7.0

/** A change per week smaller than this (in display units) is read as "steady". */
private const val STEADY_THRESHOLD_PER_WEEK: Double = 0.05

/**
 * The numbers behind a chart's spoken summary ("Weight, 30 days, 212.4 to 209.9 pounds,
 * decreasing 0.6 pounds per week").
 *
 * @property spanDays the length of the charted range, in days.
 * @property perWeek the average change per week between the first and last points; `null` with
 *   fewer than two distinct dates.
 */
data class ChartSummaryData(val spanDays: Int, val first: Double, val last: Double, val perWeek: Double?)

/** The summary numbers for [points] charted over [range]; `null` when there are no points. */
fun chartSummaryData(points: List<DataPoint>, range: LocalDateRange): ChartSummaryData? {
    val firstPoint: DataPoint = points.firstOrNull() ?: return null
    val lastPoint: DataPoint = points.last()
    val daysBetween: Int = firstPoint.date.daysUntil(lastPoint.date)
    val perWeek: Double? = if (daysBetween > 0) {
        (lastPoint.value - firstPoint.value) / daysBetween * DAYS_PER_WEEK
    } else {
        null
    }
    return ChartSummaryData(
        spanDays = range.start.daysUntil(range.endInclusive) + 1,
        first = firstPoint.value,
        last = lastPoint.value,
        perWeek = perWeek,
    )
}

/** The spoken summary of [series] (the chart's primary series), in the spec's format. */
@Composable
internal fun spokenSummary(
    title: String,
    series: ChartSeries,
    range: LocalDateRange,
    formatValue: (Double) -> String,
): String? {
    val data: ChartSummaryData = chartSummaryData(series.points, range) ?: return null
    val span: String = pluralStringResource(R.plurals.chart_days, data.spanDays, data.spanDays)
    val perWeek: Double? = data.perWeek
    return if (perWeek == null) {
        stringResource(R.string.chart_summary_single, title, span, formatValue(data.last), series.unitSpoken)
    } else {
        val trend: String = when {
            abs(perWeek) < STEADY_THRESHOLD_PER_WEEK -> stringResource(R.string.chart_trend_steady)
            perWeek > 0 -> stringResource(R.string.chart_trend_increasing, formatValue(perWeek), series.unitSpoken)
            else -> stringResource(R.string.chart_trend_decreasing, formatValue(abs(perWeek)), series.unitSpoken)
        }
        stringResource(
            R.string.chart_summary,
            title,
            span,
            formatValue(data.first),
            formatValue(data.last),
            series.unitSpoken,
            trend,
        )
    }
}

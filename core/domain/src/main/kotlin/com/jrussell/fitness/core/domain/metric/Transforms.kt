package com.jrussell.fitness.core.domain.metric

import kotlin.math.pow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

/** Default trend-weight smoothing factor per day (spec, "Trend weight"). */
const val DEFAULT_TREND_ALPHA: Double = 0.1

private const val DAYS_PER_WEEK: Double = 7.0

/**
 * Aggregates [daily] points (one per date, ascending) into [bucket]s using [aggregation].
 * A WEEK is ISO Monday–Sunday and is dated its Monday; a MONTH is dated its first day. A bucket's
 * coverage is the mean of its known coverages, or `null` if none is known.
 */
fun bucketize(daily: List<DataPoint>, bucket: Bucket, aggregation: Aggregation): List<DataPoint> {
    if (bucket == Bucket.DAY) {
        return daily
    }
    return daily.groupBy { point: DataPoint -> bucketStart(point.date, bucket) }
        .map { (start: LocalDate, points: List<DataPoint>) ->
            val coverages: List<Double> = points.mapNotNull { point: DataPoint -> point.coverage }
            DataPoint(
                date = start,
                value = aggregate(points.map { point: DataPoint -> point.value }, aggregation),
                coverage = if (coverages.isEmpty()) null else coverages.average(),
            )
        }
        .sortedBy { point: DataPoint -> point.date }
}

/**
 * Trend weight (decision #28): an exponential moving average whose smoothing factor adjusts for
 * gaps, `T_t = T_prev + α_eff · (w_t − T_prev)` with `α_eff = 1 − (1 − α)^Δd`, Δd = days since the
 * previous weigh-in. The first trend value equals the first weight. Input and output have one
 * point per weigh-in date, ascending.
 */
fun trendWeight(daily: List<DataPoint>, alpha: Double = DEFAULT_TREND_ALPHA): List<DataPoint> {
    val trend: MutableList<DataPoint> = mutableListOf()
    daily.forEach { point: DataPoint ->
        val previous: DataPoint? = trend.lastOrNull()
        val value: Double = if (previous == null) {
            point.value
        } else {
            val gapDays: Int = previous.date.daysUntil(point.date)
            val effectiveAlpha: Double = 1.0 - (1.0 - alpha).pow(gapDays)
            previous.value + effectiveAlpha * (point.value - previous.value)
        }
        trend.add(DataPoint(point.date, value, coverage = null))
    }
    return trend
}

/** Where a metric started and ended over a range, and its average rate per week. */
data class RangeChange(val start: Double, val end: Double, val perWeek: Double)

/**
 * The CHANGE summary of [trend] over [range]. The start is the last point on or before the range
 * start (so a range beginning between weigh-ins still has a start), else the first point inside
 * the range; the end is the last point on or before the range end. `null` when fewer than two
 * distinct dates are available.
 */
fun rangeChange(trend: List<DataPoint>, range: LocalDateRange): RangeChange? {
    val startPoint: DataPoint? = trend.lastOrNull { point: DataPoint -> point.date <= range.start }
        ?: trend.firstOrNull { point: DataPoint -> point.date in range }
    val endPoint: DataPoint? = trend.lastOrNull { point: DataPoint -> point.date <= range.endInclusive }
    val spanDays: Int = if (startPoint != null && endPoint != null) startPoint.date.daysUntil(endPoint.date) else 0
    return if (startPoint != null && endPoint != null && spanDays > 0) {
        RangeChange(
            start = startPoint.value,
            end = endPoint.value,
            perWeek = (endPoint.value - startPoint.value) / spanDays * DAYS_PER_WEEK,
        )
    } else {
        null
    }
}

private fun bucketStart(date: LocalDate, bucket: Bucket): LocalDate {
    return when (bucket) {
        Bucket.DAY -> date
        Bucket.WEEK -> date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        Bucket.MONTH -> LocalDate(date.year, date.month, 1)
    }
}

private fun aggregate(values: List<Double>, aggregation: Aggregation): Double {
    return when (aggregation) {
        Aggregation.SUM -> values.sum()
        Aggregation.MEAN -> values.average()
        Aggregation.LAST -> values.last()
        Aggregation.MIN -> values.min()
        Aggregation.MAX -> values.max()
    }
}

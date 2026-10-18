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

/**
 * A straight line fitted through weigh-ins (or any daily points) by least squares, with x in days.
 * Its slope is "the trend": how fast the value is changing. [valueAtAnchor] is the line's value
 * on [anchor], the first point's date.
 */
data class LinearTrend(val anchor: LocalDate, val valueAtAnchor: Double, val perDay: Double) {
    val perWeek: Double get() = perDay * DAYS_PER_WEEK

    /** The line's value on [date]. */
    fun valueAt(date: LocalDate): Double {
        return valueAtAnchor + perDay * anchor.daysUntil(date)
    }
}

/**
 * The least-squares line through [points] (ascending by date). Points are placed by their date,
 * so uneven gaps between weigh-ins count. `null` with fewer than two distinct dates.
 */
fun linearTrend(points: List<DataPoint>): LinearTrend? {
    val anchor: LocalDate = points.firstOrNull()?.date ?: return null
    // Centered sums, so large day numbers never cost precision.
    val xs: List<Double> = points.map { point: DataPoint -> anchor.daysUntil(point.date).toDouble() }
    val meanX: Double = xs.average()
    val meanY: Double = points.map { point: DataPoint -> point.value }.average()
    val spreadX: Double = xs.sumOf { x: Double -> (x - meanX) * (x - meanX) }
    val covariance: Double = points.indices.sumOf { index: Int ->
        (xs[index] - meanX) * (points[index].value - meanY)
    }
    // No spread in x means every point is on one date: there is no slope to fit.
    return if (spreadX == 0.0) {
        null
    } else {
        val perDay: Double = covariance / spreadX
        LinearTrend(anchor = anchor, valueAtAnchor = meanY - perDay * meanX, perDay = perDay)
    }
}

/** Where a metric started and ended over a range, and its trend per week. */
data class RangeChange(val start: Double, val end: Double, val perWeek: Double)

/**
 * The CHANGE summary of [points] (ascending by date) over [range]: the first and last points
 * inside the range, and the slope of the best-fit line through them ([linearTrend]). A single
 * point (or a single date) has a trend of zero. `null` when no point falls inside the range.
 */
fun rangeChange(points: List<DataPoint>, range: LocalDateRange): RangeChange? {
    val inside: List<DataPoint> = points.filter { point: DataPoint -> point.date in range }
    val first: DataPoint = inside.firstOrNull() ?: return null
    return RangeChange(start = first.value, end = inside.last().value, perWeek = linearTrend(inside)?.perWeek ?: 0.0)
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

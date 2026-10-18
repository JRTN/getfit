package com.jrussell.fitness.core.domain.metric

import com.jrussell.fitness.core.model.MeasurementUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange

/**
 * Identifies one chartable number, e.g. `body.weight` or `nutrition.nutrient.protein`. Every
 * chart, table and summary refers to data by metric ID, never by table or repository.
 */
@JvmInline
value class MetricId(val value: String)

/** How several values combine into one (a day's readings into a day, days into a week). */
enum class Aggregation {
    SUM,
    MEAN,
    LAST,
    MIN,
    MAX,
}

/** The time bucket a series is aggregated into. */
enum class Bucket {
    DAY,
    WEEK,
    MONTH,
}

/** Which part of the app a metric belongs to (decides where its single-domain charts live). */
enum class MetricCategory {
    NUTRITION,
    HYDRATION,
    TRAINING,
    BODY,
    SLEEP,
    ENERGY,
    ADHERENCE,
    PROJECTION,
}

/**
 * What a metric is: its name (a string resource), unit, how a day's values combine, and whether
 * up is good (`null` when neither direction is better, e.g. body weight).
 */
data class MetricDescriptor(
    val id: MetricId,
    val displayNameResource: Int,
    val canonicalUnit: MeasurementUnit,
    val dailyAggregation: Aggregation,
    val category: MetricCategory,
    val higherIsBetter: Boolean?,
)

/**
 * One value of a series. [coverage] is the fraction of the value backed by known data (nutrition,
 * where unknown nutrients are never zero); `null` where it does not apply.
 */
data class DataPoint(val date: LocalDate, val value: Double, val coverage: Double?)

/** Publishes some metrics' series. Providers are contributed to the registry by Hilt multibinding. */
interface MetricSeriesProvider {
    fun descriptors(): List<MetricDescriptor>

    /** The metric's points in [range], aggregated into [bucket]s; emits again when data changes. */
    fun series(metricId: MetricId, range: LocalDateRange, bucket: Bucket): Flow<List<DataPoint>>
}

/** Every metric in the app, routed to the provider that owns it. */
interface MetricRegistry {
    fun allDescriptors(): List<MetricDescriptor>

    /** The descriptor for [metricId], or `null` if no provider publishes it. */
    fun descriptor(metricId: MetricId): MetricDescriptor?

    /** As [MetricSeriesProvider.series]; an unknown [metricId] emits an empty list. */
    fun series(metricId: MetricId, range: LocalDateRange, bucket: Bucket): Flow<List<DataPoint>>
}

/** Well-known metric IDs. */
object MetricIds {
    /** Weigh-ins: one per day, the day's last reading. */
    val BODY_WEIGHT: MetricId = MetricId("body.weight")

    /** Trend weight: gap-adjusted exponential moving average of [BODY_WEIGHT] (decision #28). */
    val BODY_TREND_WEIGHT: MetricId = MetricId("body.trend_weight")
}

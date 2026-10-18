package com.jrussell.fitness.core.data.metric

import com.jrussell.fitness.core.data.R
import com.jrussell.fitness.core.domain.MeasurementRepository
import com.jrussell.fitness.core.domain.metric.Aggregation
import com.jrussell.fitness.core.domain.metric.Bucket
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.domain.metric.MetricCategory
import com.jrussell.fitness.core.domain.metric.MetricDescriptor
import com.jrussell.fitness.core.domain.metric.MetricId
import com.jrussell.fitness.core.domain.metric.MetricIds
import com.jrussell.fitness.core.domain.metric.MetricSeriesProvider
import com.jrussell.fitness.core.domain.metric.bucketize
import com.jrussell.fitness.core.domain.metric.trendWeight
import com.jrussell.fitness.core.model.Measurement
import com.jrussell.fitness.core.model.MeasurementTypeKeys
import com.jrussell.fitness.core.model.StandardUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange

/**
 * Publishes body metrics from manual and imported measurements:
 * - `body.weight`: one point per day, the day's last reading (`MeasurementType.dailyAggregation` LAST);
 * - `body.trend_weight`: the gap-adjusted moving average of those daily weights, computed over
 *   all history and then sliced to the range, so a range's first trend value is never a cold start.
 *
 * Values are kilograms; display units are converted by the UI.
 */
class BodyMetricProvider @Inject constructor(private val measurements: MeasurementRepository) : MetricSeriesProvider {
    private val descriptors: List<MetricDescriptor> = listOf(
        bodyDescriptor(MetricIds.BODY_WEIGHT, R.string.metric_body_weight),
        bodyDescriptor(MetricIds.BODY_TREND_WEIGHT, R.string.metric_body_trend_weight),
    )

    override fun descriptors(): List<MetricDescriptor> {
        return descriptors
    }

    override fun series(metricId: MetricId, range: LocalDateRange, bucket: Bucket): Flow<List<DataPoint>> {
        return when (metricId) {
            MetricIds.BODY_WEIGHT -> dailyWeights(range.start, range.endInclusive)
                .map { daily: List<DataPoint> -> bucketize(daily, bucket, Aggregation.LAST) }
            MetricIds.BODY_TREND_WEIGHT -> dailyWeights(HISTORY_START, range.endInclusive)
                .map { daily: List<DataPoint> ->
                    val inRange: List<DataPoint> = trendWeight(daily).filter { point: DataPoint -> point.date in range }
                    bucketize(inRange, bucket, Aggregation.LAST)
                }
            else -> flowOf(emptyList())
        }
    }

    /** One point per local date: that day's last reading by start time. */
    private fun dailyWeights(from: LocalDate, to: LocalDate): Flow<List<DataPoint>> {
        return measurements.observeMeasurements(MeasurementTypeKeys.WEIGHT, from, to)
            .map { readings: List<Measurement> ->
                readings.groupBy { reading: Measurement -> reading.localDate }
                    .map { (date: LocalDate, sameDay: List<Measurement>) ->
                        val last: Measurement = sameDay.maxBy { reading: Measurement -> reading.startTime }
                        DataPoint(date, last.value, coverage = null)
                    }
                    .sortedBy { point: DataPoint -> point.date }
            }
    }

    private fun bodyDescriptor(id: MetricId, nameResource: Int): MetricDescriptor {
        return MetricDescriptor(
            id = id,
            displayNameResource = nameResource,
            canonicalUnit = StandardUnit.KILOGRAM,
            dailyAggregation = Aggregation.LAST,
            category = MetricCategory.BODY,
            higherIsBetter = null,
        )
    }

    private companion object {
        /** Trend weight reads all history; no weigh-in predates the epoch. */
        val HISTORY_START: LocalDate = LocalDate.fromEpochDays(0)
    }
}

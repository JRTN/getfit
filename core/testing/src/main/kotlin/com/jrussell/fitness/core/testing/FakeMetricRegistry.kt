package com.jrussell.fitness.core.testing

import com.jrussell.fitness.core.domain.metric.Bucket
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.domain.metric.MetricDescriptor
import com.jrussell.fitness.core.domain.metric.MetricId
import com.jrussell.fitness.core.domain.metric.MetricRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateRange

/**
 * A [MetricRegistry] whose series tests set directly through [series]. A request returns the
 * stored points that fall inside the requested range (buckets are ignored: tests use DAY).
 */
class FakeMetricRegistry(
    private val descriptors: List<MetricDescriptor> = emptyList(),
    val series: MutableMap<MetricId, MutableStateFlow<List<DataPoint>>> = mutableMapOf(),
) : MetricRegistry {
    override fun allDescriptors(): List<MetricDescriptor> {
        return descriptors
    }

    override fun descriptor(metricId: MetricId): MetricDescriptor? {
        return descriptors.firstOrNull { descriptor: MetricDescriptor -> descriptor.id == metricId }
    }

    override fun series(metricId: MetricId, range: LocalDateRange, bucket: Bucket): Flow<List<DataPoint>> {
        return series.getOrPut(metricId) { MutableStateFlow(emptyList()) }
            .map { points: List<DataPoint> -> points.filter { point: DataPoint -> point.date in range } }
    }
}

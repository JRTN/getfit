package com.jrussell.fitness.core.data.metric

import com.jrussell.fitness.core.domain.metric.Bucket
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.domain.metric.MetricDescriptor
import com.jrussell.fitness.core.domain.metric.MetricId
import com.jrussell.fitness.core.domain.metric.MetricRegistry
import com.jrussell.fitness.core.domain.metric.MetricSeriesProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDateRange

/**
 * Routes each metric to the provider that publishes it. Providers arrive through Hilt
 * multibinding (`@IntoSet` in `:app`'s `MetricsModule`), so adding a domain's metrics never
 * touches this class.
 *
 * @throws IllegalStateException at construction if two providers publish the same metric ID.
 */
class DefaultMetricRegistry @Inject constructor(providers: Set<@JvmSuppressWildcards MetricSeriesProvider>) :
    MetricRegistry {
    private val routes: Map<MetricId, Pair<MetricDescriptor, MetricSeriesProvider>> = buildRoutes(providers)

    override fun allDescriptors(): List<MetricDescriptor> {
        return routes.values.map { (descriptor: MetricDescriptor, _) -> descriptor }
    }

    override fun descriptor(metricId: MetricId): MetricDescriptor? {
        return routes[metricId]?.first
    }

    override fun series(metricId: MetricId, range: LocalDateRange, bucket: Bucket): Flow<List<DataPoint>> {
        val provider: MetricSeriesProvider = routes[metricId]?.second ?: return flowOf(emptyList())
        return provider.series(metricId, range, bucket)
    }

    private fun buildRoutes(
        providers: Set<MetricSeriesProvider>,
    ): Map<MetricId, Pair<MetricDescriptor, MetricSeriesProvider>> {
        val routes: MutableMap<MetricId, Pair<MetricDescriptor, MetricSeriesProvider>> = linkedMapOf()
        providers.forEach { provider: MetricSeriesProvider ->
            provider.descriptors().forEach { descriptor: MetricDescriptor ->
                check(descriptor.id !in routes) { "Metric ${descriptor.id.value} is published by two providers" }
                routes[descriptor.id] = descriptor to provider
            }
        }
        return routes
    }
}

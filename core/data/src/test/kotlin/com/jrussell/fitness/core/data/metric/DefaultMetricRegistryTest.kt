package com.jrussell.fitness.core.data.metric

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.jrussell.fitness.core.domain.metric.Aggregation
import com.jrussell.fitness.core.domain.metric.Bucket
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.domain.metric.MetricCategory
import com.jrussell.fitness.core.domain.metric.MetricDescriptor
import com.jrussell.fitness.core.domain.metric.MetricId
import com.jrussell.fitness.core.domain.metric.MetricSeriesProvider
import com.jrussell.fitness.core.model.StandardUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import org.junit.Test

class DefaultMetricRegistryTest {
    private val range: LocalDateRange = LocalDate(2026, 10, 1)..LocalDate(2026, 10, 31)

    @Test
    fun registry_unknownMetric_emitsEmpty() = runTest {
        val registry: DefaultMetricRegistry = DefaultMetricRegistry(setOf(StubProvider("body.weight")))

        assertThat(registry.series(MetricId("nope"), range, Bucket.DAY).first()).isEmpty()
        assertThat(registry.descriptor(MetricId("nope"))).isNull()
    }

    @Test
    fun registry_duplicateIds_throws() {
        assertFailure { DefaultMetricRegistry(setOf(StubProvider("body.weight"), StubProvider("body.weight"))) }
            .isInstanceOf<IllegalStateException>()
    }

    /** A provider publishing one metric with no data; distinct instances so the set keeps both. */
    private class StubProvider(id: String) : MetricSeriesProvider {
        private val descriptor: MetricDescriptor = MetricDescriptor(
            id = MetricId(id),
            displayNameResource = 0,
            canonicalUnit = StandardUnit.KILOGRAM,
            dailyAggregation = Aggregation.LAST,
            category = MetricCategory.BODY,
            higherIsBetter = null,
        )

        override fun descriptors(): List<MetricDescriptor> {
            return listOf(descriptor)
        }

        override fun series(metricId: MetricId, range: LocalDateRange, bucket: Bucket): Flow<List<DataPoint>> {
            return flowOf(emptyList())
        }
    }
}

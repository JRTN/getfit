package com.jrussell.fitness.di

import com.jrussell.fitness.core.data.metric.BodyMetricProvider
import com.jrussell.fitness.core.data.metric.DefaultMetricRegistry
import com.jrussell.fitness.core.domain.metric.MetricRegistry
import com.jrussell.fitness.core.domain.metric.MetricSeriesProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * The metric registry and every metric provider. Each domain contributes its provider here with
 * `@IntoSet`; the registry routes metric IDs to providers, so charts never know where data lives.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MetricsModule {
    @Binds
    abstract fun bindMetricRegistry(implementation: DefaultMetricRegistry): MetricRegistry

    @Binds
    @IntoSet
    abstract fun bindBodyMetricProvider(implementation: BodyMetricProvider): MetricSeriesProvider
}

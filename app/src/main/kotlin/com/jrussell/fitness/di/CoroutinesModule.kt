package com.jrussell.fitness.di

import com.jrussell.fitness.core.data.di.ApplicationScope
import com.jrussell.fitness.core.data.di.DefaultDispatcher
import com.jrussell.fitness.core.data.di.IoDispatcher
import com.jrussell.fitness.core.data.id.UuidIdGenerator
import com.jrussell.fitness.core.domain.IdGenerator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The only place that names real dispatchers and the system clock (detekt's `NoDirectDispatchers`
 * allows `Dispatchers.*` in this package alone). Everything else injects the qualifiers, so tests
 * substitute test dispatchers and a fake clock.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoroutinesModule {
    @Binds
    abstract fun bindIdGenerator(implementation: UuidIdGenerator): IdGenerator

    companion object {
        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher {
            return Dispatchers.IO
        }

        @Provides
        @DefaultDispatcher
        fun provideDefaultDispatcher(): CoroutineDispatcher {
            return Dispatchers.Default
        }

        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(@DefaultDispatcher defaultDispatcher: CoroutineDispatcher): CoroutineScope {
            return CoroutineScope(SupervisorJob() + defaultDispatcher)
        }

        @Provides
        fun provideClock(): Clock {
            return Clock.System
        }
    }
}

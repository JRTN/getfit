package com.jrussell.fitness.di

import android.content.Context
import com.jrussell.fitness.core.data.di.IoDispatcher
import com.jrussell.fitness.core.data.owner.CurrentOwnerProvider
import com.jrussell.fitness.core.data.owner.SingleLocalOwnerProvider
import com.jrussell.fitness.core.data.repository.DefaultDayBoundaryRepository
import com.jrussell.fitness.core.data.repository.DefaultMeasurementRepository
import com.jrussell.fitness.core.data.time.SystemWallClock
import com.jrussell.fitness.core.data.write.DeviceIdProvider
import com.jrussell.fitness.core.data.write.FileDeviceIdProvider
import com.jrussell.fitness.core.domain.DayBoundaryRepository
import com.jrussell.fitness.core.domain.MeasurementRepository
import com.jrussell.fitness.core.domain.time.WallClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Binds the data layer: the single local owner, the per-install device ID (kept in
 * `noBackupFilesDir`, so a restored backup never carries another device's ID), and the
 * repositories features see as `:core:domain` interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindCurrentOwnerProvider(implementation: SingleLocalOwnerProvider): CurrentOwnerProvider

    @Binds
    abstract fun bindDayBoundaryRepository(implementation: DefaultDayBoundaryRepository): DayBoundaryRepository

    @Binds
    abstract fun bindMeasurementRepository(implementation: DefaultMeasurementRepository): MeasurementRepository

    @Binds
    abstract fun bindWallClock(implementation: SystemWallClock): WallClock

    companion object {
        @Provides
        @Singleton
        fun provideDeviceIdProvider(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DeviceIdProvider {
            return FileDeviceIdProvider(context.noBackupFilesDir, ioDispatcher)
        }
    }
}

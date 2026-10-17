package com.jrussell.fitness.di

import com.jrussell.fitness.core.sync.NoOpSyncGateway
import com.jrussell.fitness.core.sync.SyncGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Sync is disabled in v1: the seam exists, bound to a gateway that does nothing. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    abstract fun bindSyncGateway(implementation: NoOpSyncGateway): SyncGateway
}

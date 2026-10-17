package com.jrussell.fitness.di

import android.content.Context
import com.jrussell.fitness.core.data.di.IoDispatcher
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.createGetFitDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/** Opens `user.db` once per process; Room runs its queries on the injected IO dispatcher. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideGetFitDatabase(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): GetFitDatabase {
        return createGetFitDatabase(context, ioDispatcher)
    }
}

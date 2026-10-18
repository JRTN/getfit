package com.jrussell.fitness.di

import android.content.Context
import com.jrussell.fitness.core.data.backup.installPendingRestore
import com.jrussell.fitness.core.data.di.IoDispatcher
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.createGetFitDatabase
import com.jrussell.fitness.core.model.logging.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Opens `user.db` once per process; Room runs its queries on the injected IO dispatcher. A corrupt
 * database file is copied to `files/corrupt/` before SQLite deletes it.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideGetFitDatabase(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        clock: Clock,
        logger: Logger,
    ): GetFitDatabase {
        // A restore staged before the last restart is installed before Room opens the file.
        if (installPendingRestore(context)) {
            logger.info("Restore", "installed the staged database")
        }
        return createGetFitDatabase(
            context = context,
            queryContext = ioDispatcher,
            corruptDirectory = File(context.filesDir, "corrupt"),
            clock = clock,
        )
    }
}

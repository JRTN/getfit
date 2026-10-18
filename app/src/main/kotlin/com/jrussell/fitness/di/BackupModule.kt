package com.jrussell.fitness.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.jrussell.fitness.core.backup.documents.ContentResolverDocumentStore
import com.jrussell.fitness.core.backup.documents.DocumentStore
import com.jrussell.fitness.core.backup.work.BackupScheduler
import com.jrussell.fitness.core.backup.work.SnapshotJob
import com.jrussell.fitness.core.backup.work.WorkManagerBackupScheduler
import com.jrussell.fitness.core.data.backup.AppVersion
import com.jrussell.fitness.core.data.backup.BackupFiles
import com.jrussell.fitness.core.data.backup.DefaultBackupRepository
import com.jrussell.fitness.core.data.backup.pendingRestoreFor
import com.jrussell.fitness.core.data.logging.FileLogger
import com.jrussell.fitness.core.datastore.BackupSettingsDataSource
import com.jrussell.fitness.core.domain.backup.AppRestarter
import com.jrussell.fitness.core.domain.backup.BackupRepository
import com.jrussell.fitness.core.model.logging.Logger
import com.jrussell.fitness.restart.ProcessAppRestarter
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlin.time.Clock
import kotlinx.datetime.TimeZone

/**
 * Backup and restore: the repository Settings uses (also the daily worker's job), the document
 * picker's store, the WorkManager scheduler, the restart after a full restore, and the file logger
 * background work writes to.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {
    @Binds
    abstract fun bindBackupRepository(implementation: DefaultBackupRepository): BackupRepository

    @Binds
    abstract fun bindSnapshotJob(implementation: DefaultBackupRepository): SnapshotJob

    @Binds
    abstract fun bindBackupScheduler(implementation: WorkManagerBackupScheduler): BackupScheduler

    @Binds
    abstract fun bindAppRestarter(implementation: ProcessAppRestarter): AppRestarter

    companion object {
        @Provides
        fun provideDocumentStore(@ApplicationContext context: Context): DocumentStore {
            return ContentResolverDocumentStore(context)
        }

        @Provides
        fun provideBackupFiles(@ApplicationContext context: Context): BackupFiles {
            return BackupFiles(File(context.cacheDir, "backup"), pendingRestoreFor(context))
        }

        @Provides
        @Singleton
        fun provideBackupSettingsDataSource(dataStore: DataStore<Preferences>): BackupSettingsDataSource {
            return BackupSettingsDataSource(dataStore)
        }

        /** The installed version name, as exports record it. */
        @Provides
        fun provideAppVersion(@ApplicationContext context: Context): AppVersion {
            val name: String? = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            return AppVersion(name ?: "unknown")
        }

        @Provides
        @Singleton
        fun provideLogger(@ApplicationContext context: Context, clock: Clock): Logger {
            return FileLogger(File(context.filesDir, "logs"), clock, TimeZone.currentSystemDefault())
        }
    }
}

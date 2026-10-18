package com.jrussell.fitness.core.data.backup

import com.jrussell.fitness.core.backup.work.BackupScheduler
import com.jrussell.fitness.core.datastore.BackupSettings
import com.jrussell.fitness.core.datastore.BackupSettingsDataSource
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first

/**
 * What the app does for backups at every start, once a folder is chosen: make sure the daily
 * snapshot is scheduled, and ask for a catch-up snapshot if the last success is stale (battery
 * management can hold back periodic work for apps it thinks are unused).
 */
class BackupStartup @Inject constructor(
    private val settings: BackupSettingsDataSource,
    private val scheduler: BackupScheduler,
    private val clock: Clock,
) {
    suspend fun run() {
        val stored: BackupSettings = settings.settings.first()
        if (stored.folderUri != null) {
            scheduler.ensureScheduled()
            scheduler.catchUpIfStale(stored.lastSuccessMillis?.let(Instant::fromEpochMilliseconds), clock.now())
        }
    }
}

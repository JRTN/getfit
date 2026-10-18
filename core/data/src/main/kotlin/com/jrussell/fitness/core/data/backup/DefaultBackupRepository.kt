package com.jrussell.fitness.core.data.backup

import com.jrussell.fitness.core.backup.documents.DocumentStore
import com.jrussell.fitness.core.backup.snapshot.PendingRestore
import com.jrussell.fitness.core.backup.snapshot.SnapshotCheck
import com.jrussell.fitness.core.backup.work.BackupScheduler
import com.jrussell.fitness.core.backup.work.SnapshotJob
import com.jrussell.fitness.core.data.di.IoDispatcher
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.datastore.BackupSettings
import com.jrussell.fitness.core.datastore.BackupSettingsDataSource
import com.jrussell.fitness.core.domain.DomainError
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.domain.backup.BackupFailure
import com.jrussell.fitness.core.domain.backup.BackupRepository
import com.jrussell.fitness.core.domain.backup.BackupStatus
import com.jrussell.fitness.core.domain.backup.DocumentRef
import com.jrussell.fitness.core.domain.backup.RestoreKind
import com.jrussell.fitness.core.domain.backup.RestorePreview
import com.jrussell.fitness.core.domain.backup.RestoreResult
import com.jrussell.fitness.core.importers.export.ExportBundle
import com.jrussell.fitness.core.importers.export.MeasurementRecord
import com.jrussell.fitness.core.importers.export.encodeExportBundle
import com.jrussell.fitness.core.model.logging.Logger
import java.io.OutputStream
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * [BackupRepository] over `user.db`, the document picker and the backup settings, and the daily
 * [SnapshotJob] the worker runs.
 *
 * - **Daily backup:** a `VACUUM INTO` snapshot is copied into the chosen folder as
 *   `getfit-snapshot-<UTC time>.db`, and snapshots beyond the newest 14 are deleted. Its outcome
 *   is recorded for Settings; nothing else touches that status.
 * - **Exports:** the JSON bundle, or a snapshot, written to a document the user created.
 * - **Restore:** the chosen document is copied to the cache first. A SQLite file is checked, the
 *   current data is copied to the safety copy, and the file is staged for the next start
 *   ([RestoreResult.RestartRequired]); anything else is read as a JSON export, checked, and merged
 *   ([BundleMerger]). Nothing changes unless every check passes.
 *
 * All file and database work runs on [ioDispatcher].
 */
class DefaultBackupRepository @Inject constructor(
    database: GetFitDatabase,
    private val documents: DocumentStore,
    private val settings: BackupSettingsDataSource,
    private val scheduler: BackupScheduler,
    private val exporter: BundleExporter,
    private val merger: BundleMerger,
    files: BackupFiles,
    private val clock: Clock,
    private val logger: Logger,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BackupRepository,
    SnapshotJob {
    private val operations: BackupFileOperations = BackupFileOperations(database, documents, files)
    private val pendingRestore: PendingRestore = files.pendingRestore

    override val status: Flow<BackupStatus> = settings.settings.map { stored: BackupSettings ->
        BackupStatus(
            folderName = stored.folderName,
            lastSuccess = stored.lastSuccessMillis?.let(Instant::fromEpochMilliseconds),
            lastFailure = stored.lastFailure?.let { name: String ->
                BackupFailure.entries.firstOrNull { it.name == name }
            },
        )
    }

    override suspend fun chooseFolder(folder: DocumentRef): Outcome<Unit> {
        return withContext(ioDispatcher) {
            if (documents.takePersistablePermission(folder.uri)) {
                settings.setFolder(folder.uri, documents.displayName(folder.uri) ?: folder.uri)
                scheduler.reschedule()
                Outcome.Success(Unit)
            } else {
                backupFailure(BackupFailure.FOLDER_UNAVAILABLE)
            }
        }
    }

    override suspend fun run(): Boolean {
        return backUpNow() is Outcome.Success
    }

    override suspend fun backUpNow(): Outcome<Instant> {
        return withContext(ioDispatcher) {
            val takenAt: Instant = clock.now()
            val failure: BackupFailure? = operations.snapshotIntoFolder(settings.settings.first().folderUri, takenAt)
            if (failure == null) {
                settings.recordSuccess(takenAt.toEpochMilliseconds())
                Outcome.Success(takenAt)
            } else {
                settings.recordFailure(failure.name)
                logger.warn(TAG, "snapshot failed: $failure")
                backupFailure(failure)
            }
        }
    }

    override suspend fun exportJson(to: DocumentRef): Outcome<Int> {
        return withContext(ioDispatcher) {
            val bundle: ExportBundle = exporter.export()
            val written: Boolean = operations.write(to.uri) { output: OutputStream ->
                output.write(encodeExportBundle(bundle).toByteArray(Charsets.UTF_8))
            }
            if (written) Outcome.Success(liveCount(bundle)) else backupFailure(BackupFailure.WRITE_FAILED)
        }
    }

    override suspend fun exportDatabase(to: DocumentRef): Outcome<Unit> {
        return withContext(ioDispatcher) {
            if (operations.copySnapshotTo(to.uri)) Outcome.Success(Unit) else backupFailure(BackupFailure.WRITE_FAILED)
        }
    }

    override suspend fun inspect(from: DocumentRef): Outcome<RestorePreview> {
        return withContext(ioDispatcher) {
            when (val read: ReadFile = operations.readChosen(from.uri, ::restorable)) {
                is ReadFile.Snapshot -> read.check.toPreview()
                is ReadFile.Bundle -> Outcome.Success(bundlePreview(read.bundle))
                is ReadFile.Failed -> backupFailure(read.failure)
            }
        }
    }

    override suspend fun restore(from: DocumentRef): Outcome<RestoreResult> {
        return withContext(ioDispatcher) {
            when (val read: ReadFile = operations.readChosen(from.uri, ::restorable)) {
                is ReadFile.Snapshot -> stageReplacement(read)
                is ReadFile.Bundle -> Outcome.Success(
                    merger.merge(read.bundle).also { merged ->
                        logger.info(TAG, "merged a JSON export: $merged")
                    },
                )
                is ReadFile.Failed -> backupFailure(read.failure)
            }
        }
    }

    private suspend fun restorable(bundle: ExportBundle): Boolean {
        return merger.check(bundle).isEmpty()
    }

    private fun stageReplacement(read: ReadFile.Snapshot): Outcome<RestoreResult> {
        return when (val preview: Outcome<RestorePreview> = read.check.toPreview()) {
            is Outcome.Failure -> preview
            is Outcome.Success -> {
                // The current data is kept before anything is replaced.
                operations.writeSafetyCopy()
                pendingRestore.stage(read.file)
                logger.info(TAG, "staged a database restore; restarting")
                Outcome.Success(RestoreResult.RestartRequired)
            }
        }
    }
}

private const val TAG: String = "Backup"

private fun bundlePreview(bundle: ExportBundle): RestorePreview {
    val newest: Instant? = bundle.measurements.maxOfOrNull { record: MeasurementRecord -> record.sync.updatedAt }
    return RestorePreview(RestoreKind.MERGE, liveCount(bundle), newest)
}

private fun SnapshotCheck.toPreview(): Outcome<RestorePreview> {
    return when (this) {
        is SnapshotCheck.Valid -> Outcome.Success(RestorePreview(RestoreKind.REPLACE, measurementCount, newestChange))
        SnapshotCheck.NotSqlite -> backupFailure(BackupFailure.UNREADABLE)
        SnapshotCheck.Damaged -> backupFailure(BackupFailure.DAMAGED)
        SnapshotCheck.NotGetFit -> backupFailure(BackupFailure.NOT_GETFIT)
        is SnapshotCheck.NewerVersion -> backupFailure(BackupFailure.NEWER_VERSION)
        SnapshotCheck.InvalidRows -> backupFailure(BackupFailure.INVALID_ROWS)
    }
}

/** Entries the user would count: live measurements. */
private fun liveCount(bundle: ExportBundle): Int {
    return bundle.measurements.count { record: MeasurementRecord -> record.sync.deletedAt == null }
}

private fun backupFailure(reason: BackupFailure): Outcome.Failure {
    return Outcome.Failure(DomainError.Backup(reason))
}

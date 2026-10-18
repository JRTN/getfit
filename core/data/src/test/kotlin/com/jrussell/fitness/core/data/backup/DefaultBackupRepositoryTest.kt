package com.jrussell.fitness.core.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.jrussell.fitness.core.backup.snapshot.PendingRestore
import com.jrussell.fitness.core.backup.snapshot.SnapshotCheck
import com.jrussell.fitness.core.backup.snapshot.inspectSnapshot
import com.jrussell.fitness.core.backup.snapshot.snapshotFileName
import com.jrussell.fitness.core.data.repository.RepositoryTestHarness
import com.jrussell.fitness.core.database.USER_DATABASE_IDENTITY_HASHES
import com.jrussell.fitness.core.database.USER_DATABASE_VERSION
import com.jrussell.fitness.core.datastore.BackupSettingsDataSource
import com.jrussell.fitness.core.domain.DomainError
import com.jrussell.fitness.core.domain.ManualMeasurement
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.domain.backup.BackupFailure
import com.jrussell.fitness.core.domain.backup.BackupStatus
import com.jrussell.fitness.core.domain.backup.DocumentRef
import com.jrussell.fitness.core.domain.backup.RestoreKind
import com.jrussell.fitness.core.domain.backup.RestorePreview
import com.jrussell.fitness.core.domain.backup.RestoreResult
import com.jrussell.fitness.core.importers.export.BundleDecode
import com.jrussell.fitness.core.importers.export.decodeExportBundle
import com.jrussell.fitness.core.importers.export.encodeExportBundle
import com.jrussell.fitness.core.model.MeasurementTypeKeys
import com.jrussell.fitness.core.testing.RecordingLogger
import java.io.File
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultBackupRepositoryTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val harness: RepositoryTestHarness = RepositoryTestHarness()
    private val documents: FileDocumentStore = FileDocumentStore()
    private val scheduler: RecordingScheduler = RecordingScheduler()

    private val folder: File by lazy { temporaryFolder.newFolder("GetFit backups") }
    private val restoreDirectory: File by lazy { File(temporaryFolder.root, "files/restore") }
    private val databaseFile: File by lazy { File(temporaryFolder.root, "databases/user.db") }

    @After
    fun close() {
        harness.close()
    }

    @Test
    fun chooseFolder_takesPermission_savesNameAndSchedules() = runTest {
        val repository: DefaultBackupRepository = repository()

        val outcome: Outcome<Unit> = repository.chooseFolder(DocumentRef(folder.path))

        assertThat(outcome).isEqualTo(Outcome.Success(Unit))
        assertThat(repository.status.first()).isEqualTo(BackupStatus("GetFit backups", null, null))
        assertThat(scheduler.rescheduled).isEqualTo(1)
    }

    @Test
    fun chooseFolder_permissionRefused_isFolderUnavailable() = runTest {
        documents.permitted = false

        assertThat(
            repository().chooseFolder(DocumentRef(folder.path)),
        ).isEqualTo(failure(BackupFailure.FOLDER_UNAVAILABLE))
    }

    @Test
    fun backUpNow_writesSnapshotIntoFolder_recordsSuccess() = runTest {
        val repository: DefaultBackupRepository = repository()
        logWeight(harness, 82.4)
        repository.chooseFolder(DocumentRef(folder.path))

        val outcome: Outcome<*> = repository.backUpNow()

        assertThat(outcome).isEqualTo(Outcome.Success(harness.clock.now()))
        val snapshot: File = File(folder, snapshotFileName(harness.clock.now()))
        assertThat(inspect(snapshot)).isEqualTo(SnapshotCheck.Valid(1, harness.clock.now()))
        assertThat(repository.status.first().lastSuccess).isEqualTo(harness.clock.now())
    }

    @Test
    fun backUpNow_prunesBeyond14() = runTest {
        val repository: DefaultBackupRepository = repository()
        repository.chooseFolder(DocumentRef(folder.path))
        (1..15).forEach { day: Int ->
            File(folder, "getfit-snapshot-202609${day.toString().padStart(2, '0')}-0300Z.db").writeText("old")
        }
        File(folder, "notes.txt").writeText("keep me")

        repository.backUpNow()

        val snapshots: List<String> = folder.list().orEmpty().filter { name: String ->
            name.startsWith("getfit-snapshot-")
        }
        assertThat(snapshots).hasSize(14)
        assertThat(snapshots.contains(snapshotFileName(harness.clock.now()))).isTrue()
        assertThat(File(folder, "notes.txt").exists()).isTrue()
    }

    @Test
    fun backUpNow_folderGone_recordsFolderUnavailable() = runTest {
        val repository: DefaultBackupRepository = repository()
        repository.chooseFolder(DocumentRef(folder.path))
        folder.deleteRecursively()

        assertThat(repository.backUpNow()).isEqualTo(failure(BackupFailure.FOLDER_UNAVAILABLE))
        assertThat(repository.status.first().lastFailure).isEqualTo(BackupFailure.FOLDER_UNAVAILABLE)
    }

    @Test
    fun backUpNow_noFolder_isFolderUnavailable() = runTest {
        assertThat(repository().backUpNow()).isEqualTo(failure(BackupFailure.FOLDER_UNAVAILABLE))
    }

    @Test
    fun success_clearsPreviousFailure() = runTest {
        val repository: DefaultBackupRepository = repository()
        repository.chooseFolder(DocumentRef(folder.path))
        documents.permitted = false
        repository.backUpNow()
        documents.permitted = true

        repository.backUpNow()

        assertThat(repository.status.first().lastFailure).isNull()
    }

    @Test
    fun snapshotJob_reportsWhetherTheBackupWorked() = runTest {
        val repository: DefaultBackupRepository = repository()

        assertThat(repository.run()).isEqualTo(false)
        repository.chooseFolder(DocumentRef(folder.path))
        assertThat(repository.run()).isEqualTo(true)
    }

    @Test
    fun exportJson_writesDecodableBundle_returnsEntryCount() = runTest {
        logWeight(harness, 82.4)
        logWeight(harness, 82.0)
        val target: File = File(temporaryFolder.root, "getfit-backup.json")

        val outcome: Outcome<Int> = repository().exportJson(DocumentRef(target.path))

        assertThat(outcome).isEqualTo(Outcome.Success(2))
        assertThat(decodeExportBundle(target.readText())).isInstanceOf<BundleDecode.Decoded>()
    }

    @Test
    fun exportDatabase_writesValidSnapshot() = runTest {
        logWeight(harness, 82.4)
        val target: File = File(temporaryFolder.root, "getfit-backup.db")

        assertThat(repository().exportDatabase(DocumentRef(target.path))).isEqualTo(Outcome.Success(Unit))
        assertThat(inspect(target)).isInstanceOf<SnapshotCheck.Valid>()
    }

    @Test
    fun inspect_json_isMergeWithCounts() = runTest {
        logWeight(harness, 82.4)
        val file: File = File(temporaryFolder.root, "backup.json")
        repository().exportJson(DocumentRef(file.path))

        assertThat(repository().inspect(DocumentRef(file.path)))
            .isEqualTo(Outcome.Success(RestorePreview(RestoreKind.MERGE, 1, harness.clock.now())))
    }

    @Test
    fun inspect_snapshot_isReplaceWithCounts() = runTest {
        logWeight(harness, 82.4)
        val file: File = File(temporaryFolder.root, "backup.db")
        repository().exportDatabase(DocumentRef(file.path))

        assertThat(repository().inspect(DocumentRef(file.path)))
            .isEqualTo(Outcome.Success(RestorePreview(RestoreKind.REPLACE, 1, harness.clock.now())))
    }

    @Test
    fun inspect_photo_isUnreadable() = runTest {
        val photo: File = File(temporaryFolder.root, "photo.png")
        photo.writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13))

        assertThat(repository().inspect(DocumentRef(photo.path))).isEqualTo(failure(BackupFailure.UNREADABLE))
    }

    @Test
    fun inspect_newerJson_isNewerVersion() = runTest {
        val file: File = File(temporaryFolder.root, "future.json").apply { writeText("""{"formatVersion": 9}""") }

        assertThat(repository().inspect(DocumentRef(file.path))).isEqualTo(failure(BackupFailure.NEWER_VERSION))
    }

    @Test
    fun restore_truncatedJson_changesNothing() = runTest {
        val other: RepositoryTestHarness = RepositoryTestHarness()
        logWeight(other, 70.0)
        val file: File = File(temporaryFolder.root, "backup.json")
        file.writeText(encodeExportBundle(exporter(other).export()))
        other.close()
        file.writeText(file.readText().dropLast(40))

        assertThat(repository().restore(DocumentRef(file.path))).isEqualTo(failure(BackupFailure.UNREADABLE))
        assertThat(harness.database.measurementDao().exportAll(harness.ownerProvider.currentOwnerId().value)).hasSize(0)
    }

    @Test
    fun restore_json_merges_returnsMerged() = runTest {
        val other: RepositoryTestHarness = RepositoryTestHarness()
        logWeight(other, 70.0)
        logWeight(other, 71.0)
        val file: File = File(temporaryFolder.root, "backup.json")
        file.writeText(encodeExportBundle(exporter(other).export()))
        other.close()

        val outcome: Outcome<RestoreResult> = repository().restore(DocumentRef(file.path))

        assertThat((outcome as Outcome.Success).value).isInstanceOf<RestoreResult.Merged>()
        assertThat((outcome.value as RestoreResult.Merged).added).isEqualTo(2)
    }

    @Test
    fun restore_snapshot_makesSafetyCopy_stages_returnsRestartRequired() = runTest {
        logWeight(harness, 82.4)
        val file: File = File(temporaryFolder.root, "backup.db")
        val repository: DefaultBackupRepository = repository()
        repository.exportDatabase(DocumentRef(file.path))

        val outcome: Outcome<RestoreResult> = repository.restore(DocumentRef(file.path))

        assertThat(outcome).isEqualTo(Outcome.Success(RestoreResult.RestartRequired))
        assertThat(inspect(File(restoreDirectory, "before-restore.db"))).isInstanceOf<SnapshotCheck.Valid>()
        assertThat(File(restoreDirectory, "staged.db").readBytes().contentEquals(file.readBytes())).isTrue()
    }

    private fun TestScope.repository(): DefaultBackupRepository {
        return DefaultBackupRepository(
            database = harness.database,
            documents = documents,
            settings = BackupSettingsDataSource(dataStoreIn(backgroundScope)),
            scheduler = scheduler,
            exporter = exporter(harness),
            merger = merger(harness),
            files = BackupFiles(File(temporaryFolder.root, "cache"), PendingRestore(restoreDirectory, databaseFile)),
            clock = harness.clock,
            logger = RecordingLogger(),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
    }

    private val dataStoreFile: File by lazy { File(temporaryFolder.root, "user_preferences.preferences_pb") }

    /** One DataStore per file, as DataStore requires, even when a test builds two repositories. */
    private var dataStore: DataStore<Preferences>? = null

    private fun dataStoreIn(scope: CoroutineScope): DataStore<Preferences> {
        return dataStore ?: PreferenceDataStoreFactory.create(scope = scope, produceFile = { dataStoreFile })
            .also { created: DataStore<Preferences> -> dataStore = created }
    }

    private fun inspect(file: File): SnapshotCheck {
        return inspectSnapshot(file, USER_DATABASE_VERSION, USER_DATABASE_IDENTITY_HASHES)
    }

    private fun failure(reason: BackupFailure): Outcome<Nothing> {
        return Outcome.Failure(DomainError.Backup(reason))
    }

    private suspend fun logWeight(target: RepositoryTestHarness, kilograms: Double) {
        target.clock.advanceBy(1.hours)
        target.measurementRepository.addManual(
            ManualMeasurement(
                typeKey = MeasurementTypeKeys.WEIGHT,
                value = kilograms,
                takenAt = target.clock.now(),
                timeZone = TimeZone.UTC,
                enteredValue = null,
                enteredUnit = null,
                note = null,
            ),
        )
    }
}

package com.jrussell.fitness.core.data.backup

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.jrussell.fitness.core.backup.snapshot.PendingRestore
import com.jrussell.fitness.core.data.TEST_NOW
import com.jrussell.fitness.core.data.repository.RepositoryTestHarness
import com.jrussell.fitness.core.database.entity.MeasurementTypeEntity
import com.jrussell.fitness.core.datastore.BackupSettingsDataSource
import com.jrussell.fitness.core.domain.DomainError
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.domain.backup.BackupFailure
import com.jrussell.fitness.core.domain.backup.DocumentRef
import com.jrussell.fitness.core.importers.export.MeasurementTypeRecord
import com.jrussell.fitness.core.importers.export.encodeExportBundle
import com.jrussell.fitness.core.model.DailyAggregation
import com.jrussell.fitness.core.model.MeasurementCategory
import com.jrussell.fitness.core.model.StandardUnit
import com.jrussell.fitness.core.testing.RecordingLogger
import java.io.File
import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Database and storage failures during a user's backup action become a [BackupFailure] instead of
 * an exception that would crash the app, and snapshot runs never overlap.
 */
@RunWith(RobolectricTestRunner::class)
class BackupFailureHandlingTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val harness: RepositoryTestHarness = RepositoryTestHarness()

    @After
    fun close() {
        harness.close()
    }

    @Test
    fun restore_conflictingTypeKey_isInvalidRows_andChangesNothing() = runTest {
        harness.seeder.seed()
        val owner: Uuid = harness.ownerProvider.currentOwnerId().value
        harness.database.measurementTypeDao().insert(aLocalType(owner, "custom.glucose"))
        // Same key as the local type, different ID: the (ownerId, key) unique index rejects it.
        val file: File = File(temporaryFolder.root, "backup.json")
        file.writeText(
            encodeExportBundle(aBundleOf(emptyList()).copy(measurementTypes = listOf(aTypeRecord("custom.glucose")))),
        )

        val outcome: Outcome<*> = repository().restore(DocumentRef(file.path))

        assertThat(outcome).isEqualTo(Outcome.Failure(DomainError.Backup(BackupFailure.INVALID_ROWS)))
        assertThat(harness.database.measurementTypeDao().exportOwned(owner).size).isEqualTo(1)
    }

    @Test
    fun exportDatabase_cacheUnwritable_isWriteFailed() = runTest {
        // A file where the cache directory should be: VACUUM INTO can't create its output.
        val notADirectory: File = temporaryFolder.newFile("cache")

        val outcome: Outcome<*> =
            repository(
                cacheDirectory = notADirectory,
            ).exportDatabase(DocumentRef(File(temporaryFolder.root, "out.db").path))

        assertThat(outcome).isEqualTo(Outcome.Failure(DomainError.Backup(BackupFailure.WRITE_FAILED)))
    }

    @Test
    fun backUpNow_concurrentRuns_neverOverlap() = runTest {
        val store: OverlapDetectingStore = OverlapDetectingStore()
        val pool: ExecutorCoroutineDispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
        try {
            val repository: DefaultBackupRepository = repository(store = store, dispatcher = pool)
            repository.chooseFolder(DocumentRef(temporaryFolder.newFolder("backups").path))

            val outcomes: List<Outcome<Instant>> = listOf(
                async(pool) { repository.backUpNow() },
                async(pool) { repository.backUpNow() },
            ).awaitAll()

            assertThat(store.maxOpen.get()).isEqualTo(1)
            assertThat(outcomes.all { it is Outcome.Success }).isTrue()
        } finally {
            pool.close()
        }
    }

    private fun aLocalType(owner: Uuid, key: String): MeasurementTypeEntity {
        return MeasurementTypeEntity(
            id = Uuid.random(),
            ownerId = owner,
            key = key,
            displayName = key,
            canonicalUnit = StandardUnit.KILOGRAM,
            category = MeasurementCategory.LAB,
            dailyAggregation = DailyAggregation.MEAN,
            hasSecondaryValue = false,
            referenceRangeLow = null,
            referenceRangeHigh = null,
            decimals = 0,
            healthConnectRecordType = null,
            createdAt = TEST_NOW,
            updatedAt = TEST_NOW,
            deletedAt = null,
            localRevision = 1,
            originDeviceId = OTHER_INSTALL,
        )
    }

    private fun aTypeRecord(key: String): MeasurementTypeRecord {
        return MeasurementTypeRecord(
            id = Uuid.random(),
            key = key,
            displayName = "Glucose",
            canonicalUnit = "custom:mg/dL",
            category = "LAB",
            dailyAggregation = "MEAN",
            hasSecondaryValue = false,
            referenceRangeLow = null,
            referenceRangeHigh = null,
            decimals = 0,
            healthConnectRecordType = null,
            sync = aSyncRecord(Instant.parse("2026-10-20T00:00:00Z")),
        )
    }

    private fun TestScope.repository(
        store: FileDocumentStore = FileDocumentStore(),
        cacheDirectory: File = File(temporaryFolder.root, "work-cache"),
        dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
    ): DefaultBackupRepository {
        val restoreDirectory: File = File(temporaryFolder.root, "restore")
        return DefaultBackupRepository(
            database = harness.database,
            documents = store,
            settings = BackupSettingsDataSource(dataStoreIn(backgroundScope)),
            scheduler = RecordingScheduler(),
            exporter = exporter(harness),
            merger = merger(harness),
            files = BackupFiles(
                cacheDirectory,
                PendingRestore(restoreDirectory, File(temporaryFolder.root, "db/user.db")),
            ),
            clock = harness.clock,
            logger = RecordingLogger(),
            ioDispatcher = dispatcher,
        )
    }

    private fun dataStoreIn(scope: CoroutineScope) = PreferenceDataStoreFactory.create(scope = scope) {
        File(temporaryFolder.root, "prefs.preferences_pb")
    }

    /**
     * Counts how many snapshot copies are being written at once. The first writer waits briefly
     * for a second one to arrive, so overlapping runs are caught reliably.
     */
    private class OverlapDetectingStore : FileDocumentStore() {
        private val open: AtomicInteger = AtomicInteger(0)
        val maxOpen: AtomicInteger = AtomicInteger(0)

        override fun openOutput(uri: String): OutputStream? {
            val stream: OutputStream = super.openOutput(uri) ?: return null
            val now: Int = open.incrementAndGet()
            maxOpen.accumulateAndGet(now) { a: Int, b: Int -> maxOf(a, b) }
            if (now == 1) {
                val deadline: Long = System.currentTimeMillis() + WAIT_FOR_OVERLAP_MILLIS
                while (open.get() < 2 && System.currentTimeMillis() < deadline) {
                    Thread.sleep(POLL_MILLIS)
                }
            }
            return object : FilterOutputStream(stream) {
                override fun close() {
                    super.close()
                    open.decrementAndGet()
                }
            }
        }

        private companion object {
            const val WAIT_FOR_OVERLAP_MILLIS: Long = 500
            const val POLL_MILLIS: Long = 10
        }
    }
}

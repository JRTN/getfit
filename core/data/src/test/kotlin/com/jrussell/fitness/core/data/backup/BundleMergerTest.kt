package com.jrussell.fitness.core.data.backup

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import com.jrussell.fitness.core.data.TEST_NOW
import com.jrussell.fitness.core.data.count
import com.jrussell.fitness.core.data.repository.RepositoryTestHarness
import com.jrussell.fitness.core.data.repository.aMeasurementFor
import com.jrussell.fitness.core.database.entity.DayBoundarySettingEntity
import com.jrussell.fitness.core.database.entity.MeasurementEntity
import com.jrussell.fitness.core.database.entity.UserProfileEntity
import com.jrussell.fitness.core.domain.backup.RestoreResult
import com.jrussell.fitness.core.importers.export.BundleProblem
import com.jrussell.fitness.core.importers.export.DayBoundaryRecord
import com.jrussell.fitness.core.importers.export.ExportBundle
import com.jrussell.fitness.core.importers.export.ProfileRecord
import com.jrussell.fitness.core.model.Measurement
import com.jrussell.fitness.core.model.MeasurementSource
import com.jrussell.fitness.core.model.MeasurementTypeKeys
import com.jrussell.fitness.core.model.Sex
import com.jrussell.fitness.core.model.ids.OwnerId
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BundleMergerTest {
    private val harness: RepositoryTestHarness = RepositoryTestHarness()
    private var owner: OwnerId = OwnerId(Uuid.random())
    private lateinit var weightType: Uuid

    private val older: Instant = TEST_NOW - 2.days
    private val newer: Instant = TEST_NOW + 1.days

    @Before
    fun setUp() = runTest {
        harness.seeder.seed()
        owner = harness.ownerProvider.currentOwnerId()
        weightType = harness.database.referenceCatalogDao().builtInMeasurementTypeIds().single()
    }

    @After
    fun close() {
        harness.close()
    }

    @Test
    fun merge_freshInstall_adoptsRowsIntoLocalOwner() = runTest {
        val bundleOwner: Uuid = Uuid.parse("0199f0a1-2b3c-7d4e-8f50-6a7b8c9d0e1f")
        val records = (1..3).map { day: Int -> aWeightRecord(Uuid.random(), weightType, 80.0 + day, older) }
        val bundle: ExportBundle = aBundleOf(records).copy(
            ownerId = bundleOwner,
            profiles = listOf(aProfileRecord(bundleOwner, updatedAt = older)),
        )

        merger(harness).merge(bundle)

        assertThat(harness.database.count("SELECT COUNT(*) FROM UserProfile")).isEqualTo(1)
        assertThat(harness.database.measurementDao().exportAll(owner.value)).hasSize(3)
        val seen: List<Measurement> = harness.measurementRepository
            .observeMeasurements(MeasurementTypeKeys.WEIGHT, LocalDate(2026, 1, 1), LocalDate(2026, 12, 31))
            .first()
        assertThat(seen).hasSize(3)
    }

    @Test
    fun merge_sameBundleTwice_secondChangesNothing() = runTest {
        val bundle: ExportBundle = aBundleOf((1..3).map { aWeightRecord(Uuid.random(), weightType, 80.0, older) })
        merger(harness).merge(bundle)
        val revisionAfterFirst: Int = harness.database.count("SELECT value FROM SyncSequence")

        val second: RestoreResult.Merged = merger(harness).merge(bundle)

        assertThat(second).isEqualTo(RestoreResult.Merged(added = 0, updated = 0, unchanged = 3))
        assertThat(harness.database.count("SELECT value FROM SyncSequence") - revisionAfterFirst)
            .isLessThanOrEqualTo(1)
        assertThat(harness.database.measurementDao().exportAll(owner.value)).hasSize(3)
    }

    @Test
    fun merge_newerBundleRowWins_olderLoses() = runTest {
        val local: MeasurementEntity = aMeasurementFor(owner.value, weightType, LocalDate(2026, 10, 10))
        val untouched: MeasurementEntity = aMeasurementFor(owner.value, weightType, LocalDate(2026, 10, 11))
        harness.database.measurementDao().insert(local)
        harness.database.measurementDao().insert(untouched)

        val result: RestoreResult.Merged = merger(harness).merge(
            aBundleOf(
                listOf(
                    aWeightRecord(local.id, weightType, value = 75.0, updatedAt = newer),
                    aWeightRecord(untouched.id, weightType, value = 60.0, updatedAt = older),
                ),
            ),
        )

        assertThat(result).isEqualTo(RestoreResult.Merged(added = 0, updated = 1, unchanged = 1))
        assertThat(harness.database.measurementDao().findAnyById(owner.value, local.id)?.value).isEqualTo(75.0)
        assertThat(harness.database.measurementDao().findAnyById(owner.value, untouched.id)?.value).isEqualTo(70.0)
    }

    @Test
    fun merge_tombstoneInBundleNewer_deletesLocal() = runTest {
        val local: MeasurementEntity = aMeasurementFor(owner.value, weightType, LocalDate(2026, 10, 10))
        harness.database.measurementDao().insert(local)

        merger(harness).merge(aBundleOf(listOf(aWeightRecord(local.id, weightType, 70.0, newer, deletedAt = newer))))

        assertThat(harness.database.measurementDao().findById(owner.value, local.id)).isNull()
        assertThat(harness.database.measurementDao().findAnyById(owner.value, local.id)?.sync?.deletedAt)
            .isEqualTo(newer)
    }

    @Test
    fun merge_dayStartSameDateDifferentId_newerWinsInPlace() = runTest {
        harness.dayBoundaryRepository.setDayStart(240, LocalDate(2026, 10, 1))
        val localId: Uuid = harness.database.dayBoundarySettingDao().exportAll(owner.value).single().id
        val record = DayBoundaryRecord(Uuid.random(), 300, LocalDate(2026, 10, 1), aSyncRecord(newer))

        val result: RestoreResult.Merged = merger(
            harness,
        ).merge(aBundleOf(emptyList()).copy(dayBoundarySettings = listOf(record)))

        val rows: List<DayBoundarySettingEntity> = harness.database.dayBoundarySettingDao().exportAll(owner.value)
        assertThat(rows.map { row -> row.id to row.dayStartMinutes }).containsExactly(localId to 300)
        assertThat(result.updated).isEqualTo(1)
    }

    @Test
    fun merge_measurementSameExternalIdDifferentId_matchesBySourceAndExternalId() = runTest {
        val local: MeasurementEntity = aMeasurementFor(owner.value, weightType, LocalDate(2026, 10, 10))
            .copy(source = MeasurementSource.IMPORT, externalId = "hc-1")
        harness.database.measurementDao().insert(local)

        val result: RestoreResult.Merged = merger(harness).merge(
            aBundleOf(
                listOf(aWeightRecord(Uuid.random(), weightType, 77.0, newer, source = "IMPORT", externalId = "hc-1")),
            ),
        )

        assertThat(result).isEqualTo(RestoreResult.Merged(added = 0, updated = 1, unchanged = 0))
        val rows: List<MeasurementEntity> = harness.database.measurementDao().exportAll(owner.value)
        assertThat(rows.map { row -> row.id to row.value }).containsExactly(local.id to 77.0)
    }

    @Test
    fun merge_profileFields_newerWins_keepsLocalId() = runTest {
        val bundleOwner: Uuid = Uuid.parse("0199f0a1-2b3c-7d4e-8f50-6a7b8c9d0e1f")
        val bundle: ExportBundle = aBundleOf(emptyList()).copy(
            ownerId = bundleOwner,
            profiles = listOf(aProfileRecord(bundleOwner, updatedAt = newer)),
        )

        merger(harness).merge(bundle)

        val profile: UserProfileEntity? = harness.database.localProfileDao().findLocalProfile()
        assertThat(profile?.id).isEqualTo(owner.value)
        assertThat(profile?.heightM).isEqualTo(1.88)
        assertThat(profile?.sex).isEqualTo(Sex.MALE)
    }

    @Test
    fun check_problemRows_reported_andMergeNotCalledWritesNothing() = runTest {
        val bad: ExportBundle = aBundleOf(listOf(aWeightRecord(Uuid.random(), weightType, Double.NaN, older)))

        val problems: List<BundleProblem> = merger(harness).check(bad)

        assertThat(problems).isNotEmpty()
        assertThat(harness.database.measurementDao().exportAll(owner.value)).isEmpty()
        assertThat(merger(harness).check(aBundleOf(listOf(aWeightRecord(Uuid.random(), weightType, 80.0, older)))))
            .isEmpty()
    }

    private fun aProfileRecord(id: Uuid, updatedAt: Instant): ProfileRecord {
        return ProfileRecord(
            id = id,
            displayName = "J",
            sex = "MALE",
            birthDate = LocalDate(1990, 5, 1),
            heightM = 1.88,
            sync = aSyncRecord(updatedAt),
        )
    }
}

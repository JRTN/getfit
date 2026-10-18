package com.jrussell.fitness.core.data.backup

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import com.jrussell.fitness.core.data.repository.RepositoryTestHarness
import com.jrussell.fitness.core.database.entity.DayBoundarySettingEntity
import com.jrussell.fitness.core.database.entity.MeasurementEntity
import com.jrussell.fitness.core.domain.ManualMeasurement
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.domain.backup.RestoreResult
import com.jrussell.fitness.core.importers.export.EXPORT_FORMAT_VERSION
import com.jrussell.fitness.core.importers.export.ExportBundle
import com.jrussell.fitness.core.model.MeasurementTypeKeys
import com.jrussell.fitness.core.model.ids.MeasurementId
import com.jrussell.fitness.core.model.ids.OwnerId
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BundleRoundTripTest {
    private val source: RepositoryTestHarness = RepositoryTestHarness()
    private val target: RepositoryTestHarness = RepositoryTestHarness()

    @After
    fun close() {
        source.close()
        target.close()
    }

    @Test
    fun export_thenMergeIntoEmptyDatabase_rowsIdentical() = runTest {
        logWeights(source)
        source.dayBoundaryRepository.setDayStart(240, LocalDate(2026, 10, 1))
        val sourceOwner: OwnerId = source.ownerProvider.currentOwnerId()
        val bundle: ExportBundle = exporter(source).export()

        val targetOwner: OwnerId = target.ownerProvider.currentOwnerId()
        val result: RestoreResult.Merged = merger(target).merge(bundle)

        assertThat(targetOwner).isNotEqualTo(sourceOwner)
        // Three weigh-ins and the day start are added; both profiles were stamped at the same
        // instant, so the file's profile is not newer and stays unchanged.
        assertThat(result).isEqualTo(RestoreResult.Merged(added = 4, updated = 0, unchanged = 1))
        assertThat(comparable(target.database.measurementDao().exportAll(targetOwner.value)))
            .containsExactlyInAnyOrder(
                *comparable(source.database.measurementDao().exportAll(sourceOwner.value)).toTypedArray(),
            )
        assertThat(target.database.dayBoundarySettingDao().exportAll(targetOwner.value).map(::comparable))
            .isEqualTo(source.database.dayBoundarySettingDao().exportAll(sourceOwner.value).map(::comparable))
    }

    @Test
    fun export_excludesBuiltInTypesAndDeviceTables() = runTest {
        logWeights(source)

        val bundle: ExportBundle = exporter(source).export()

        assertThat(bundle.formatVersion).isEqualTo(EXPORT_FORMAT_VERSION)
        assertThat(bundle.appVersion).isEqualTo("0.1.0-test")
        assertThat(bundle.ownerId).isEqualTo(source.ownerProvider.currentOwnerId().value)
        assertThat(bundle.measurementTypes).isEmpty()
        assertThat(bundle.profiles.single().id).isEqualTo(bundle.ownerId)
        assertThat(bundle.measurements.size).isEqualTo(3)
        assertThat(bundle.measurements.any { record -> record.sync.deletedAt != null }).isTrue()
    }

    /** Three weigh-ins on different days, one of them deleted. */
    private suspend fun logWeights(harness: RepositoryTestHarness) {
        val ids: List<MeasurementId> = listOf(82.4, 82.1, 81.9).mapIndexed { index: Int, kilograms: Double ->
            val outcome = harness.measurementRepository.addManual(
                ManualMeasurement(
                    typeKey = MeasurementTypeKeys.WEIGHT,
                    value = kilograms,
                    takenAt = harness.clock.now() - (24 * (index + 1)).hours,
                    timeZone = TimeZone.of("America/Toronto"),
                    enteredValue = kilograms * 2.2,
                    enteredUnit = "lb",
                    note = null,
                ),
            )
            (outcome as Outcome.Success).value
        }
        harness.measurementRepository.delete(ids.last())
    }

    /** Rows as they should match across installs: owner and local revision are per install. */
    private fun comparable(rows: List<MeasurementEntity>): List<MeasurementEntity> {
        return rows.map { row: MeasurementEntity ->
            row.copy(ownerId = OWNERLESS, sync = row.sync.copy(localRevision = 0))
        }
    }

    private fun comparable(row: DayBoundarySettingEntity): DayBoundarySettingEntity {
        return row.copy(ownerId = OWNERLESS, sync = row.sync.copy(localRevision = 0))
    }
}

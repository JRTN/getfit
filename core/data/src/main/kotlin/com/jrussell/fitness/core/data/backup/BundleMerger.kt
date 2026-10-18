package com.jrussell.fitness.core.data.backup

import com.jrussell.fitness.core.data.owner.CurrentOwnerProvider
import com.jrussell.fitness.core.data.seed.ReferenceSeeder
import com.jrussell.fitness.core.data.write.StampedWriter
import com.jrussell.fitness.core.data.write.WriteStamp
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.entity.DayBoundarySettingEntity
import com.jrussell.fitness.core.database.entity.MeasurementEntity
import com.jrussell.fitness.core.database.entity.MeasurementTypeEntity
import com.jrussell.fitness.core.database.entity.UserProfileEntity
import com.jrussell.fitness.core.domain.backup.RestoreResult
import com.jrussell.fitness.core.importers.export.BundleProblem
import com.jrussell.fitness.core.importers.export.DayBoundaryRecord
import com.jrussell.fitness.core.importers.export.ExportBundle
import com.jrussell.fitness.core.importers.export.MeasurementRecord
import com.jrussell.fitness.core.importers.export.MeasurementTypeRecord
import com.jrussell.fitness.core.importers.export.ProfileRecord
import com.jrussell.fitness.core.importers.export.validateExportBundle
import com.jrussell.fitness.core.model.MeasurementSource
import com.jrussell.fitness.core.model.ids.OwnerId
import javax.inject.Inject
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Restores a JSON export by merging it into this install (spec: "restore from JSON merges by `id`
 * and `updatedAt`"):
 * - every row is adopted into the current owner, so a fresh install that already made its own
 *   empty profile ends up with one profile holding all the data;
 * - a row whose ID is unknown is inserted; a known row is replaced only when the file's copy has
 *   a later `updatedAt` (tombstones included); otherwise the local row stays;
 * - rows with a natural key match by it when the ID differs: a day-start setting by its
 *   `effectiveFrom`, an imported measurement by `(source, externalId)`. The newer one wins in
 *   place and the local ID is kept;
 * - the profile's personal fields are taken when the file's profile is newer; its ID never changes.
 *
 * Imported rows keep their times and origin device, so importing the same file again changes
 * nothing. Everything runs in one stamped transaction, after [check] found no problems.
 */
class BundleMerger @Inject constructor(
    private val database: GetFitDatabase,
    private val stampedWriter: StampedWriter,
    private val ownerProvider: CurrentOwnerProvider,
    private val referenceSeeder: ReferenceSeeder,
) {
    /** Problems that would stop [bundle] from being stored; empty means [merge] may run. */
    suspend fun check(bundle: ExportBundle): List<BundleProblem> {
        referenceSeeder.seed()
        val builtIns: Set<Uuid> = database.referenceCatalogDao().builtInMeasurementTypeIds().toSet()
        return validateExportBundle(bundle, builtIns)
    }

    suspend fun merge(bundle: ExportBundle): RestoreResult.Merged {
        val owner: OwnerId = ownerProvider.currentOwnerId()
        return stampedWriter.write { stamp: WriteStamp ->
            val tally: Tally = Tally()
            bundle.profiles.firstOrNull { profile: ProfileRecord -> profile.id == bundle.ownerId }
                ?.let { profile: ProfileRecord -> tally.count(mergeProfile(profile, stamp)) }
            bundle.dayBoundarySettings.forEach { record -> tally.count(mergeDayBoundary(record, owner.value, stamp)) }
            bundle.measurementTypes.forEach { record -> tally.count(mergeType(record, owner.value, stamp)) }
            bundle.measurements.forEach { record -> tally.count(mergeMeasurement(record, owner.value, stamp)) }
            RestoreResult.Merged(added = tally.added, updated = tally.updated, unchanged = tally.unchanged)
        }
    }

    private suspend fun mergeProfile(record: ProfileRecord, stamp: WriteStamp): Change {
        val local: UserProfileEntity? = database.localProfileDao().findLocalProfile()
        return if (local != null && record.sync.updatedAt.isAfter(local.sync.updatedAt)) {
            database.localProfileDao().update(record.applyTo(local, stamp))
            Change.UPDATED
        } else {
            Change.UNCHANGED
        }
    }

    private suspend fun mergeDayBoundary(record: DayBoundaryRecord, owner: Uuid, stamp: WriteStamp): Change {
        val dao = database.dayBoundarySettingDao()
        val local: DayBoundarySettingEntity? =
            dao.findAnyById(owner, record.id) ?: dao.findAnyByEffectiveFrom(owner, record.effectiveFrom)
        return when {
            local == null -> {
                dao.insert(record.toEntity(record.id, owner, stamp))
                Change.ADDED
            }
            record.sync.updatedAt.isAfter(local.sync.updatedAt) -> {
                dao.update(record.toEntity(local.id, owner, stamp))
                Change.UPDATED
            }
            else -> Change.UNCHANGED
        }
    }

    private suspend fun mergeType(record: MeasurementTypeRecord, owner: Uuid, stamp: WriteStamp): Change {
        val dao = database.measurementTypeDao()
        val local: MeasurementTypeEntity? = dao.findAnyById(owner, record.id)
        return when {
            local == null -> {
                dao.insert(record.toEntity(owner, stamp))
                Change.ADDED
            }
            // A built-in with this ID is reference data; the file never overrides it.
            local.ownerId == null -> Change.UNCHANGED
            record.sync.updatedAt.isAfter(local.updatedAt) -> {
                dao.update(record.toEntity(owner, stamp))
                Change.UPDATED
            }
            else -> Change.UNCHANGED
        }
    }

    private suspend fun mergeMeasurement(record: MeasurementRecord, owner: Uuid, stamp: WriteStamp): Change {
        val dao = database.measurementDao()
        val local: MeasurementEntity? = dao.findAnyById(owner, record.id)
            ?: record.externalId?.let { externalId: String ->
                dao.findAnyByExternalId(owner, MeasurementSource.valueOf(record.source), externalId)
            }
        return when {
            local == null -> {
                dao.insert(record.toEntity(record.id, owner, stamp))
                Change.ADDED
            }
            record.sync.updatedAt.isAfter(local.sync.updatedAt) -> {
                dao.update(record.toEntity(local.id, owner, stamp))
                Change.UPDATED
            }
            else -> Change.UNCHANGED
        }
    }

    private fun Instant.isAfter(other: Instant): Boolean {
        return this > other
    }

    private enum class Change { ADDED, UPDATED, UNCHANGED }

    private class Tally {
        var added: Int = 0
        var updated: Int = 0
        var unchanged: Int = 0

        fun count(change: Change) {
            when (change) {
                Change.ADDED -> added++
                Change.UPDATED -> updated++
                Change.UNCHANGED -> unchanged++
            }
        }
    }
}

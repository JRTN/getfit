package com.jrussell.fitness.core.data.backup

import com.jrussell.fitness.core.data.write.WriteStamp
import com.jrussell.fitness.core.database.entity.DayBoundarySettingEntity
import com.jrussell.fitness.core.database.entity.MeasurementEntity
import com.jrussell.fitness.core.database.entity.MeasurementTypeEntity
import com.jrussell.fitness.core.database.entity.SyncMetadata
import com.jrussell.fitness.core.database.entity.UserProfileEntity
import com.jrussell.fitness.core.importers.export.DayBoundaryRecord
import com.jrussell.fitness.core.importers.export.MeasurementRecord
import com.jrussell.fitness.core.importers.export.MeasurementTypeRecord
import com.jrussell.fitness.core.importers.export.ProfileRecord
import com.jrussell.fitness.core.importers.export.SyncRecord
import com.jrussell.fitness.core.model.DailyAggregation
import com.jrussell.fitness.core.model.MeasurementCategory
import com.jrussell.fitness.core.model.MeasurementSource
import com.jrussell.fitness.core.model.Sex
import com.jrussell.fitness.core.model.measurementUnitOf
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone

// Rows → export records. Enums become names and units codes; localRevision stays behind.

internal fun SyncMetadata.toRecord(): SyncRecord {
    return SyncRecord(createdAt, updatedAt, deletedAt, originDeviceId)
}

internal fun UserProfileEntity.toRecord(): ProfileRecord {
    return ProfileRecord(id, displayName, sex?.name, birthDate, heightM, sync.toRecord())
}

internal fun DayBoundarySettingEntity.toRecord(): DayBoundaryRecord {
    return DayBoundaryRecord(id, dayStartMinutes, effectiveFrom, sync.toRecord())
}

internal fun MeasurementTypeEntity.toRecord(): MeasurementTypeRecord {
    return MeasurementTypeRecord(
        id = id,
        key = key,
        displayName = displayName,
        canonicalUnit = canonicalUnit.code,
        category = category.name,
        dailyAggregation = dailyAggregation.name,
        hasSecondaryValue = hasSecondaryValue,
        referenceRangeLow = referenceRangeLow,
        referenceRangeHigh = referenceRangeHigh,
        decimals = decimals,
        healthConnectRecordType = healthConnectRecordType,
        sync = SyncRecord(createdAt, updatedAt, deletedAt, originDeviceId),
    )
}

internal fun MeasurementEntity.toRecord(): MeasurementRecord {
    return MeasurementRecord(
        id = id,
        measurementTypeId = measurementTypeId,
        value = value,
        secondaryValue = secondaryValue,
        startTime = startTime,
        endTime = endTime,
        timeZone = timeZone.id,
        localDate = localDate,
        source = source.name,
        sourcePackage = sourcePackage,
        externalId = externalId,
        enteredValue = enteredValue,
        enteredUnit = enteredUnit,
        note = note,
        sync = sync.toRecord(),
    )
}

// Export records → rows for this install. Callers validate the bundle first
// (validateExportBundle), so the name and code lookups below cannot fail.

/**
 * The record's sync columns as written into this install: times and origin device are kept, so
 * re-importing the same file changes nothing, and the row gets this transaction's revision.
 */
internal fun SyncRecord.toMetadata(stamp: WriteStamp): SyncMetadata {
    return SyncMetadata(
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        localRevision = stamp.revision,
        originDeviceId = originDeviceId ?: stamp.deviceId.value,
    )
}

internal fun DayBoundaryRecord.toEntity(id: Uuid, ownerId: Uuid, stamp: WriteStamp): DayBoundarySettingEntity {
    return DayBoundarySettingEntity(id, ownerId, dayStartMinutes, effectiveFrom, sync.toMetadata(stamp))
}

internal fun MeasurementTypeRecord.toEntity(ownerId: Uuid, stamp: WriteStamp): MeasurementTypeEntity {
    return MeasurementTypeEntity(
        id = id,
        ownerId = ownerId,
        key = key,
        displayName = displayName,
        canonicalUnit = checkNotNull(measurementUnitOf(canonicalUnit)),
        category = MeasurementCategory.valueOf(category),
        dailyAggregation = DailyAggregation.valueOf(dailyAggregation),
        hasSecondaryValue = hasSecondaryValue,
        referenceRangeLow = referenceRangeLow,
        referenceRangeHigh = referenceRangeHigh,
        decimals = decimals,
        healthConnectRecordType = healthConnectRecordType,
        createdAt = sync.createdAt,
        updatedAt = sync.updatedAt,
        deletedAt = sync.deletedAt,
        localRevision = stamp.revision,
        originDeviceId = sync.originDeviceId ?: stamp.deviceId.value,
    )
}

internal fun MeasurementRecord.toEntity(id: Uuid, ownerId: Uuid, stamp: WriteStamp): MeasurementEntity {
    return MeasurementEntity(
        id = id,
        ownerId = ownerId,
        measurementTypeId = measurementTypeId,
        value = value,
        secondaryValue = secondaryValue,
        startTime = startTime,
        endTime = endTime,
        timeZone = TimeZone.of(timeZone),
        localDate = localDate,
        source = MeasurementSource.valueOf(source),
        sourcePackage = sourcePackage,
        externalId = externalId,
        enteredValue = enteredValue,
        enteredUnit = enteredUnit,
        note = note,
        sync = sync.toMetadata(stamp),
    )
}

/** [local] with the record's personal fields and times; the local profile keeps its ID and owner. */
internal fun ProfileRecord.applyTo(local: UserProfileEntity, stamp: WriteStamp): UserProfileEntity {
    return local.copy(
        displayName = displayName,
        sex = sex?.let(Sex::valueOf),
        birthDate = birthDate,
        heightM = heightM,
        sync = local.sync.copy(updatedAt = sync.updatedAt, localRevision = stamp.revision),
    )
}

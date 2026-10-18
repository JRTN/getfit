package com.jrussell.fitness.core.database

import com.jrussell.fitness.core.database.entity.DayBoundarySettingEntity
import com.jrussell.fitness.core.database.entity.MeasurementEntity
import com.jrussell.fitness.core.database.entity.MeasurementTypeEntity
import com.jrussell.fitness.core.database.entity.SyncMetadata
import com.jrussell.fitness.core.model.DailyAggregation
import com.jrussell.fitness.core.model.MeasurementCategory
import com.jrussell.fitness.core.model.MeasurementSource
import com.jrussell.fitness.core.model.StandardUnit
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

val FIXTURE_NOW: Instant = Instant.parse("2026-10-17T12:00:00Z")
val FIXTURE_DEVICE: Uuid = Uuid.parse("5b1d7e9a-0c44-4f7a-9c2e-3f6b2c1e8a4d")

fun aSyncMetadata(deletedAt: Instant? = null, localRevision: Long = 1): SyncMetadata {
    return SyncMetadata(
        createdAt = FIXTURE_NOW,
        updatedAt = FIXTURE_NOW,
        deletedAt = deletedAt,
        localRevision = localRevision,
        originDeviceId = FIXTURE_DEVICE,
    )
}

fun aDayBoundarySetting(
    ownerId: Uuid,
    effectiveFrom: LocalDate,
    dayStartMinutes: Int,
    deletedAt: Instant? = null,
): DayBoundarySettingEntity {
    return DayBoundarySettingEntity(
        id = Uuid.random(),
        ownerId = ownerId,
        dayStartMinutes = dayStartMinutes,
        effectiveFrom = effectiveFrom,
        sync = aSyncMetadata(deletedAt = deletedAt),
    )
}

fun aMeasurementType(id: Uuid = Uuid.random(), ownerId: Uuid?, key: String): MeasurementTypeEntity {
    return MeasurementTypeEntity(
        id = id,
        ownerId = ownerId,
        key = key,
        displayName = key,
        canonicalUnit = StandardUnit.KILOGRAM,
        category = MeasurementCategory.BODY,
        dailyAggregation = DailyAggregation.LAST,
        hasSecondaryValue = false,
        referenceRangeLow = null,
        referenceRangeHigh = null,
        decimals = 1,
        healthConnectRecordType = null,
        createdAt = FIXTURE_NOW,
        updatedAt = FIXTURE_NOW,
        deletedAt = null,
        localRevision = 0,
        originDeviceId = null,
    )
}

fun aMeasurement(
    ownerId: Uuid,
    measurementTypeId: Uuid,
    localDate: LocalDate,
    deletedAt: Instant? = null,
): MeasurementEntity {
    return MeasurementEntity(
        id = Uuid.random(),
        ownerId = ownerId,
        measurementTypeId = measurementTypeId,
        value = 82.4,
        secondaryValue = null,
        startTime = FIXTURE_NOW,
        endTime = FIXTURE_NOW,
        timeZone = TimeZone.of("America/Toronto"),
        localDate = localDate,
        source = MeasurementSource.MANUAL,
        sourcePackage = null,
        externalId = null,
        enteredValue = null,
        enteredUnit = null,
        note = null,
        sync = aSyncMetadata(deletedAt = deletedAt),
    )
}

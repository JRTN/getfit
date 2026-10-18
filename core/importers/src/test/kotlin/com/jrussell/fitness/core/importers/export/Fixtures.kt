package com.jrussell.fitness.core.importers.export

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

internal val OWNER: Uuid = Uuid.parse("0199f0a1-2b3c-7d4e-8f50-6a7b8c9d0e1f")
internal val DEVICE: Uuid = Uuid.parse("5a1d0c3e-2b4f-4c6d-8e9f-0a1b2c3d4e5f")
internal val BUILT_IN_WEIGHT: Uuid = Uuid.parse("8c6f2a10-1b2c-5d3e-9f40-5a6b7c8d9e0f")
internal val CUSTOM_TYPE: Uuid = Uuid.parse("0199f0a1-2b3c-7d4e-8f50-6a7b8c9d0e20")
internal val EXPORTED_AT: Instant = Instant.parse("2026-10-18T12:00:00Z")

internal fun aSync(deletedAt: Instant? = null): SyncRecord {
    return SyncRecord(
        createdAt = Instant.parse("2026-10-01T08:00:00Z"),
        updatedAt = Instant.parse("2026-10-02T08:00:00Z"),
        deletedAt = deletedAt,
        originDeviceId = DEVICE,
    )
}

internal fun aProfile(): ProfileRecord {
    return ProfileRecord(
        id = OWNER,
        displayName = "J",
        sex = "MALE",
        birthDate = LocalDate(1990, 5, 1),
        heightM = 1.88,
        sync = aSync(),
    )
}

internal fun aDayBoundary(minutes: Int = 240): DayBoundaryRecord {
    return DayBoundaryRecord(
        id = Uuid.parse("0199f0a1-2b3c-7d4e-8f50-6a7b8c9d0e21"),
        dayStartMinutes = minutes,
        effectiveFrom = LocalDate(2026, 10, 1),
        sync = aSync(),
    )
}

internal fun aType(canonicalUnit: String = "custom:mg/dL"): MeasurementTypeRecord {
    return MeasurementTypeRecord(
        id = CUSTOM_TYPE,
        key = "custom.glucose",
        displayName = "Glucose",
        canonicalUnit = canonicalUnit,
        category = "LAB",
        dailyAggregation = "MEAN",
        hasSecondaryValue = false,
        referenceRangeLow = 70.0,
        referenceRangeHigh = 99.0,
        decimals = 0,
        healthConnectRecordType = null,
        sync = aSync(),
    )
}

internal fun aMeasurement(
    typeId: Uuid = BUILT_IN_WEIGHT,
    value: Double = 82.4,
    timeZone: String = "America/Toronto",
    source: String = "MANUAL",
    enteredUnit: String? = "lb",
    deletedAt: Instant? = null,
): MeasurementRecord {
    return MeasurementRecord(
        id = Uuid.parse("0199f0a1-2b3c-7d4e-8f50-6a7b8c9d0e22"),
        measurementTypeId = typeId,
        value = value,
        secondaryValue = null,
        startTime = Instant.parse("2026-10-02T11:30:00Z"),
        endTime = Instant.parse("2026-10-02T11:30:00Z"),
        timeZone = timeZone,
        localDate = LocalDate(2026, 10, 2),
        source = source,
        sourcePackage = null,
        externalId = null,
        enteredValue = 181.7,
        enteredUnit = enteredUnit,
        note = "after run",
        sync = aSync(deletedAt),
    )
}

internal fun aBundle(
    dayBoundaries: List<DayBoundaryRecord> = listOf(aDayBoundary()),
    types: List<MeasurementTypeRecord> = listOf(aType()),
    measurements: List<MeasurementRecord> = listOf(aMeasurement()),
): ExportBundle {
    return ExportBundle(
        formatVersion = EXPORT_FORMAT_VERSION,
        exportedAt = EXPORTED_AT,
        appVersion = "0.1.0",
        ownerId = OWNER,
        profiles = listOf(aProfile()),
        dayBoundarySettings = dayBoundaries,
        measurementTypes = types,
        measurements = measurements,
    )
}

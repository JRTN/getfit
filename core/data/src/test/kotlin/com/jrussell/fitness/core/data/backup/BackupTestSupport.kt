package com.jrussell.fitness.core.data.backup

import com.jrussell.fitness.core.data.repository.RepositoryTestHarness
import com.jrussell.fitness.core.importers.export.EXPORT_FORMAT_VERSION
import com.jrussell.fitness.core.importers.export.ExportBundle
import com.jrussell.fitness.core.importers.export.MeasurementRecord
import com.jrussell.fitness.core.importers.export.SyncRecord
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

/** Stands in for an owner ID when comparing rows across installs. */
internal val OWNERLESS: Uuid = Uuid.parse("00000000-0000-7000-8000-000000000000")

internal val OTHER_INSTALL: Uuid = Uuid.parse("7e9a0c44-5b1d-4f7a-9c2e-3f6b2c1e8a4d")

internal fun exporter(harness: RepositoryTestHarness): BundleExporter {
    return BundleExporter(harness.database, harness.ownerProvider, harness.clock, AppVersion("0.1.0-test"))
}

internal fun merger(harness: RepositoryTestHarness): BundleMerger {
    return BundleMerger(harness.database, harness.stampedWriter, harness.ownerProvider, harness.seeder)
}

internal fun aSyncRecord(updatedAt: Instant, deletedAt: Instant? = null): SyncRecord {
    return SyncRecord(
        createdAt = Instant.parse("2026-10-01T00:00:00Z"),
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        originDeviceId = OTHER_INSTALL,
    )
}

/** A bundle from another install holding only [measurements] (no profile, no settings). */
internal fun aBundleOf(measurements: List<MeasurementRecord>): ExportBundle {
    return ExportBundle(
        formatVersion = EXPORT_FORMAT_VERSION,
        exportedAt = Instant.parse("2026-10-18T00:00:00Z"),
        appVersion = "0.1.0",
        ownerId = Uuid.parse("0199f0a1-2b3c-7d4e-8f50-6a7b8c9d0e1f"),
        profiles = emptyList(),
        dayBoundarySettings = emptyList(),
        measurementTypes = emptyList(),
        measurements = measurements,
    )
}

internal fun aWeightRecord(
    id: Uuid,
    typeId: Uuid,
    value: Double,
    updatedAt: Instant,
    deletedAt: Instant? = null,
    source: String = "MANUAL",
    externalId: String? = null,
): MeasurementRecord {
    return MeasurementRecord(
        id = id,
        measurementTypeId = typeId,
        value = value,
        secondaryValue = null,
        startTime = Instant.parse("2026-10-10T12:00:00Z"),
        endTime = Instant.parse("2026-10-10T12:00:00Z"),
        timeZone = "UTC",
        localDate = LocalDate(2026, 10, 10),
        source = source,
        sourcePackage = null,
        externalId = externalId,
        enteredValue = null,
        enteredUnit = null,
        note = null,
        sync = aSyncRecord(updatedAt, deletedAt),
    )
}

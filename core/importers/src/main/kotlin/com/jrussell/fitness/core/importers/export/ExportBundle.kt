package com.jrussell.fitness.core.importers.export

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** The JSON export format's version. A file with a higher one was made by a newer GetFit. */
const val EXPORT_FORMAT_VERSION: Int = 1

/**
 * GetFit's JSON export, format v1 (spec: "JSON export format"): the header fields, then one
 * array per `user.db` table of user data, tombstoned rows included. Device-local bookkeeping
 * (`SyncSequence`, `SyncCursor`, `DatasetVersion`, each row's `localRevision`) is left out.
 * Built-in measurement types are left out too: every install seeds them with the same IDs.
 *
 * Enums are stored by name and units by code, as plain strings, so `:core:model` stays free of
 * serialization annotations; [validateExportBundle] checks them.
 *
 * The same format serves backups now, and later a sync server's first upload and test fixtures.
 * Decoders must keep reading every older format version.
 */
@Serializable
data class ExportBundle(
    val formatVersion: Int,
    val exportedAt: Instant,
    val appVersion: String,
    val ownerId: Uuid,
    val profiles: List<ProfileRecord>,
    val dayBoundarySettings: List<DayBoundaryRecord>,
    val measurementTypes: List<MeasurementTypeRecord>,
    val measurements: List<MeasurementRecord>,
)

/** The standard sync columns of a row, minus `localRevision`, which belongs to the device. */
@Serializable
data class SyncRecord(
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val originDeviceId: Uuid?,
)

/** A `UserProfile` row; [sex] is a `Sex` name. */
@Serializable
data class ProfileRecord(
    val id: Uuid,
    val displayName: String?,
    val sex: String?,
    val birthDate: LocalDate?,
    val heightM: Double?,
    val sync: SyncRecord,
)

/** A `DayBoundarySetting` row. */
@Serializable
data class DayBoundaryRecord(
    val id: Uuid,
    val dayStartMinutes: Int,
    val effectiveFrom: LocalDate,
    val sync: SyncRecord,
)

/**
 * A user-defined `MeasurementType` row. [canonicalUnit] is a `MeasurementUnit` code; [category]
 * and [dailyAggregation] are enum names.
 */
@Serializable
data class MeasurementTypeRecord(
    val id: Uuid,
    val key: String,
    val displayName: String,
    val canonicalUnit: String,
    val category: String,
    val dailyAggregation: String,
    val hasSecondaryValue: Boolean,
    val referenceRangeLow: Double?,
    val referenceRangeHigh: Double?,
    val decimals: Int,
    val healthConnectRecordType: String?,
    val sync: SyncRecord,
)

/**
 * A `Measurement` row. [timeZone] is an IANA ID, [source] a `MeasurementSource` name and
 * [enteredUnit] a `DisplayUnit` code.
 */
@Serializable
data class MeasurementRecord(
    val id: Uuid,
    val measurementTypeId: Uuid,
    val value: Double,
    val secondaryValue: Double?,
    val startTime: Instant,
    val endTime: Instant,
    val timeZone: String,
    val localDate: LocalDate,
    val source: String,
    val sourcePackage: String?,
    val externalId: String?,
    val enteredValue: Double?,
    val enteredUnit: String?,
    val note: String?,
    val sync: SyncRecord,
)

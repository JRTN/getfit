package com.jrussell.fitness.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jrussell.fitness.core.model.DailyAggregation
import com.jrussell.fitness.core.model.MeasurementCategory
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * A kind of measurement (weight, body fat, blood pressure, a lab marker). A mixed-catalog table:
 * built-in rows have `ownerId = null`, a UUIDv5 [id] identical on every device, `localRevision = 0`
 * (never pushed) and `originDeviceId = null`; user-defined rows carry an owner and normal sync
 * columns. That difference is why the sync columns are explicit here instead of [SyncMetadata].
 *
 * SQLite treats nulls as distinct in the unique `(ownerId, key)` index, so built-in keys are kept
 * unique by their deterministic primary key instead.
 */
@Entity(
    tableName = "MeasurementType",
    indices = [Index(value = ["ownerId", "key"], unique = true)],
)
data class MeasurementTypeEntity(
    @PrimaryKey val id: Uuid,
    val ownerId: Uuid?,
    val key: String,
    val displayName: String,
    val canonicalUnit: String,
    val category: MeasurementCategory,
    val dailyAggregation: DailyAggregation,
    val hasSecondaryValue: Boolean,
    val referenceRangeLow: Double?,
    val referenceRangeHigh: Double?,
    val decimals: Int,
    val healthConnectRecordType: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val localRevision: Long,
    val originDeviceId: Uuid?,
)

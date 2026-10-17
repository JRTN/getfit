package com.jrussell.fitness.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jrussell.fitness.core.model.MeasurementSource
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * One reading of a [MeasurementTypeEntity] in its canonical unit. [enteredValue] and
 * [enteredUnit] keep what the user typed (e.g. 181.7 lb). [localDate] is fixed at creation from
 * [startTime], [timeZone] and the day start in force, and is user-editable afterwards.
 * `(ownerId, source, externalId)` is unique so imports never duplicate a record; manual rows have
 * a null `externalId`, which SQLite treats as distinct.
 */
@Entity(
    tableName = "Measurement",
    indices = [
        Index(value = ["ownerId", "measurementTypeId", "startTime"]),
        Index(value = ["ownerId", "measurementTypeId", "localDate"]),
        Index(value = ["ownerId", "source", "externalId"], unique = true),
    ],
)
data class MeasurementEntity(
    @PrimaryKey val id: Uuid,
    val ownerId: Uuid,
    val measurementTypeId: Uuid,
    val value: Double,
    val secondaryValue: Double?,
    val startTime: Instant,
    val endTime: Instant,
    val timeZone: TimeZone,
    val localDate: LocalDate,
    val source: MeasurementSource,
    val sourcePackage: String?,
    val externalId: String?,
    val enteredValue: Double?,
    val enteredUnit: String?,
    val note: String?,
    @Embedded val sync: SyncMetadata,
)

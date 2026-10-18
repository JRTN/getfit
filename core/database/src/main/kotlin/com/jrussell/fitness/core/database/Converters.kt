package com.jrussell.fitness.core.database

import androidx.room.TypeConverter
import com.jrussell.fitness.core.model.MeasurementUnit
import com.jrussell.fitness.core.model.measurementUnitOf
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * How `user.db` stores non-primitive column types, as the design fixes them:
 * `Uuid` → TEXT, `Instant` → INTEGER epoch milliseconds, `LocalDate` → INTEGER epoch day (cheap
 * range queries and indexes), `TimeZone` → TEXT IANA ID, `MeasurementUnit` → TEXT code. Enums use Room's built-in by-name storage.
 */
class Converters {
    @TypeConverter
    fun uuidToText(value: Uuid): String {
        return value.toString()
    }

    @TypeConverter
    fun uuidFromText(value: String): Uuid {
        return Uuid.parse(value)
    }

    @TypeConverter
    fun instantToEpochMillis(value: Instant): Long {
        return value.toEpochMilliseconds()
    }

    @TypeConverter
    fun instantFromEpochMillis(value: Long): Instant {
        return Instant.fromEpochMilliseconds(value)
    }

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate): Int {
        return value.toEpochDays().toInt()
    }

    @TypeConverter
    fun localDateFromEpochDay(value: Int): LocalDate {
        return LocalDate.fromEpochDays(value)
    }

    @TypeConverter
    fun timeZoneToId(value: TimeZone): String {
        return value.id
    }

    /**
     * An ID this device's time-zone data doesn't know (e.g. a row written on a device with newer
     * tzdata) reads as UTC instead of failing the whole query. The stored text is untouched unless
     * the row is edited.
     */
    @TypeConverter
    fun timeZoneFromId(value: String): TimeZone {
        return try {
            TimeZone.of(value)
        } catch (
            @Suppress("SwallowedException") unknown: IllegalTimeZoneException,
        ) {
            TimeZone.UTC
        }
    }

    @TypeConverter
    fun measurementUnitToCode(value: MeasurementUnit): String {
        return value.code
    }

    /**
     * @throws IllegalStateException for a code no version of GetFit writes. Restore and import
     *   checks reject such codes before they reach `user.db`, so this is a programming error.
     */
    @TypeConverter
    fun measurementUnitFromCode(value: String): MeasurementUnit {
        return checkNotNull(measurementUnitOf(value)) { "Unknown measurement unit code: $value" }
    }
}

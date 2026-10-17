package com.jrussell.fitness.core.database

import androidx.room.TypeConverter
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * How `user.db` stores non-primitive column types, as the design fixes them:
 * `Uuid` → TEXT, `Instant` → INTEGER epoch milliseconds, `LocalDate` → INTEGER epoch day (cheap
 * range queries and indexes), `TimeZone` → TEXT IANA ID. Enums use Room's built-in by-name storage.
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

    @TypeConverter
    fun timeZoneFromId(value: String): TimeZone {
        return TimeZone.of(value)
    }
}

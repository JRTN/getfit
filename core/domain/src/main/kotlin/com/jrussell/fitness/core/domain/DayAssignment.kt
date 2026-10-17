package com.jrussell.fitness.core.domain

import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

private const val MINUTES_PER_HOUR: Int = 60
private const val MINUTES_PER_DAY: Int = 1440

/**
 * The day an entry made at [instant] counts toward, for someone whose day starts
 * [dayStartMinutes] after midnight in [timeZone].
 *
 * Uses the wall-clock time in the entry's zone: at or after the day start, the entry belongs to
 * that calendar date; before it (e.g. 01:30 with a 04:00 start), to the previous date. No zone
 * arithmetic is involved, so DST gaps and overlaps can never shift a date.
 *
 * @throws IllegalArgumentException if [dayStartMinutes] is outside `0..1439`.
 */
fun localDateFor(instant: Instant, timeZone: TimeZone, dayStartMinutes: Int): LocalDate {
    require(dayStartMinutes in 0 until MINUTES_PER_DAY) { "dayStartMinutes must be in 0..1439, was $dayStartMinutes" }
    val local: LocalDateTime = instant.toLocalDateTime(timeZone)
    val minuteOfDay: Int = local.hour * MINUTES_PER_HOUR + local.minute
    return if (minuteOfDay >= dayStartMinutes) {
        local.date
    } else {
        local.date.minus(DatePeriod(days = 1))
    }
}

package com.jrussell.fitness.core.domain

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test

class DayAssignmentTest {
    private val toronto: TimeZone = TimeZone.of("America/Toronto")

    @Test
    fun localDateFor_beforeDayStart_isPreviousDay() {
        assertThat(localDateFor(at("2026-10-17T01:30"), toronto, 240)).isEqualTo(LocalDate(2026, 10, 16))
    }

    @Test
    fun localDateFor_atDayStart_isSameDay() {
        assertThat(localDateFor(at("2026-10-17T04:00"), toronto, 240)).isEqualTo(LocalDate(2026, 10, 17))
    }

    @Test
    fun localDateFor_midnightStart_isCalendarDate() {
        assertThat(localDateFor(at("2026-10-17T23:59"), toronto, 0)).isEqualTo(LocalDate(2026, 10, 17))
    }

    @Test
    fun localDateFor_afterSpringForward_usesWallClock() {
        assertThat(localDateFor(at("2026-03-08T03:15"), toronto, 150)).isEqualTo(LocalDate(2026, 3, 8))
    }

    @Test
    fun localDateFor_zoneDecidesDate() {
        val instant: Instant = Instant.parse("2026-10-17T02:00:00Z")

        assertThat(localDateFor(instant, TimeZone.UTC, 0)).isEqualTo(LocalDate(2026, 10, 17))
        assertThat(localDateFor(instant, toronto, 0)).isEqualTo(LocalDate(2026, 10, 16))
    }

    @Test
    fun localDateFor_outOfRangeStart_throws() {
        assertFailure { localDateFor(at("2026-10-17T12:00"), toronto, 1440) }
            .isInstanceOf<IllegalArgumentException>()
    }

    private fun at(local: String): Instant {
        return LocalDateTime.parse(local).toInstant(toronto)
    }
}

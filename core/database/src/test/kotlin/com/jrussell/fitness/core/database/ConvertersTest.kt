package com.jrussell.fitness.core.database

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Test

class ConvertersTest {
    private val converters: Converters = Converters()

    @Test
    fun uuid_roundTrips() {
        val uuid: Uuid = Uuid.parse("0199f0a1-2b3c-7d4e-8f50-6a7b8c9d0e1f")

        assertThat(converters.uuidFromText(converters.uuidToText(uuid))).isEqualTo(uuid)
    }

    @Test
    fun instant_roundTripsAsEpochMillis() {
        val instant: Instant = Instant.fromEpochMilliseconds(1_760_000_000_123)

        assertThat(converters.instantToEpochMillis(instant)).isEqualTo(1_760_000_000_123)
        assertThat(converters.instantFromEpochMillis(1_760_000_000_123)).isEqualTo(instant)
    }

    @Test
    fun localDate_roundTripsAsEpochDay_includingBefore1970() {
        val date: LocalDate = LocalDate(1969, 12, 31)

        assertThat(converters.localDateToEpochDay(date)).isEqualTo(-1)
        assertThat(converters.localDateFromEpochDay(-1)).isEqualTo(date)
    }

    @Test
    fun timeZone_roundTripsAsIanaId() {
        val zone: TimeZone = TimeZone.of("America/Toronto")

        assertThat(converters.timeZoneToId(zone)).isEqualTo("America/Toronto")
        assertThat(converters.timeZoneFromId("America/Toronto")).isEqualTo(zone)
    }
}

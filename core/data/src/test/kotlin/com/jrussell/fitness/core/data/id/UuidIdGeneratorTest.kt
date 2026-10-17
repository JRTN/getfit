package com.jrussell.fitness.core.data.id

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNotEqualTo
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import org.junit.Test

class UuidIdGeneratorTest {
    private val instant: Instant = Instant.parse("2026-10-17T12:00:00Z")

    @Test
    fun uuidV5_matchesRfc9562Vector() {
        val dnsNamespace: Uuid = Uuid.parse("6ba7b810-9dad-11d1-80b4-00c04fd430c8")

        assertThat(uuidV5(dnsNamespace, "www.example.com").toString())
            .isEqualTo("2ed6657d-e927-568b-95e1-2665a8aea6a2")
    }

    @Test
    fun nameBasedId_isStableAndNameSensitive() {
        val generator: UuidIdGenerator = UuidIdGenerator(fixedClock(instant))

        assertThat(generator.nameBasedId("a")).isEqualTo(generator.nameBasedId("a"))
        assertThat(generator.nameBasedId("a")).isNotEqualTo(generator.nameBasedId("b"))
    }

    @Test
    fun newTimeOrderedId_hasVersion7AndRfcVariant() {
        val id: Uuid = UuidIdGenerator(fixedClock(instant)).newTimeOrderedId()

        id.toLongs { mostSignificantBits: Long, leastSignificantBits: Long ->
            assertThat((mostSignificantBits ushr 12) and 0xF).isEqualTo(7L)
            assertThat(leastSignificantBits ushr 62).isEqualTo(2L)
        }
    }

    @Test
    fun newTimeOrderedId_embedsClockMillis() {
        val id: Uuid = UuidIdGenerator(fixedClock(instant)).newTimeOrderedId()

        id.toLongs { mostSignificantBits: Long, _: Long ->
            assertThat(mostSignificantBits ushr 16).isEqualTo(instant.toEpochMilliseconds())
        }
    }

    @Test
    fun newTimeOrderedId_laterClockSortsLater() {
        val earlier: Uuid = UuidIdGenerator(fixedClock(instant)).newTimeOrderedId()
        val oneMillisecondLater: Instant = Instant.fromEpochMilliseconds(instant.toEpochMilliseconds() + 1)
        val later: Uuid = UuidIdGenerator(fixedClock(oneMillisecondLater)).newTimeOrderedId()

        assertThat(earlier.toString()).isLessThan(later.toString())
    }

    private fun fixedClock(now: Instant): Clock {
        return object : Clock {
            override fun now(): Instant {
                return now
            }
        }
    }
}

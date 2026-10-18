package com.jrussell.fitness.core.domain

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jrussell.fitness.core.domain.time.WallClock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test

class TodayTest {
    private val toronto: TimeZone = TimeZone.of("America/Toronto")
    private val clock: SettableClock = SettableClock(at(2026, 10, 17, 23, 59))
    private val dayStart: MutableStateFlow<Int> = MutableStateFlow(0)
    private val dayBoundary: DayBoundaryRepository = object : DayBoundaryRepository {
        override fun observeDayStartMinutes(on: LocalDate): Flow<Int> = dayStart

        override suspend fun setDayStart(dayStartMinutes: Int, effectiveFrom: LocalDate): Outcome<Unit> {
            dayStart.value = dayStartMinutes
            return Outcome.Success(Unit)
        }
    }

    @Test
    fun observeToday_movesToTheNextDayAfterTheDayStart() = runTest {
        observeToday(clock, dayBoundary).test {
            assertThat(awaitItem()).isEqualTo(LocalDate(2026, 10, 17))

            clock.now = at(2026, 10, 18, 0, 1)

            assertThat(awaitItem()).isEqualTo(LocalDate(2026, 10, 18))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeToday_laterDayStart_keepsYesterdayUntilItPasses() = runTest {
        dayStart.value = 240
        clock.now = at(2026, 10, 18, 0, 1)
        observeToday(clock, dayBoundary).test {
            assertThat(awaitItem()).isEqualTo(LocalDate(2026, 10, 17))

            clock.now = at(2026, 10, 18, 4, 0)

            assertThat(awaitItem()).isEqualTo(LocalDate(2026, 10, 18))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeToday_dayStartChange_isAppliedAtOnce() = runTest {
        clock.now = at(2026, 10, 18, 1, 0)
        observeToday(clock, dayBoundary).test {
            assertThat(awaitItem()).isEqualTo(LocalDate(2026, 10, 18))

            dayStart.value = 240

            assertThat(awaitItem()).isEqualTo(LocalDate(2026, 10, 17))
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant {
        return LocalDateTime(year, month, day, hour, minute).toInstant(toronto)
    }

    private inner class SettableClock(var now: Instant) : WallClock {
        override fun now(): Instant = now

        override fun timeZone(): TimeZone = toronto
    }
}

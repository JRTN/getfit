package com.jrussell.fitness.core.domain

import com.jrussell.fitness.core.domain.time.WallClock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

/**
 * Today's date after the user's day start ([localDateFor]: a 01:30 visit with a 04:00 start is
 * still yesterday), emitted again whenever it changes. It re-reads [clock] every [recheckEvery],
 * so a screen left open past the day start moves to the new day, and a change of time zone or
 * day start is picked up too.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun observeToday(
    clock: WallClock,
    dayBoundary: DayBoundaryRepository,
    recheckEvery: Duration = 1.minutes,
): Flow<LocalDate> {
    val ticks: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(recheckEvery)
        }
    }
    return ticks.map { clock.now().toLocalDateTime(clock.timeZone()).date }
        .distinctUntilChanged()
        .flatMapLatest { calendarDate: LocalDate ->
            combine(dayBoundary.observeDayStartMinutes(calendarDate), ticks) { dayStartMinutes: Int, _: Unit ->
                localDateFor(clock.now(), clock.timeZone(), dayStartMinutes)
            }
        }
        .distinctUntilChanged()
}

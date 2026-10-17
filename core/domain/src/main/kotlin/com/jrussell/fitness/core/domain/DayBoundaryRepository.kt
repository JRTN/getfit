package com.jrussell.fitness.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Minutes after midnight that a day starts when the user has never set a day start. */
const val DEFAULT_DAY_START_MINUTES: Int = 0

/**
 * The user's day start, versioned by effective date so changing it never re-dates past entries.
 */
interface DayBoundaryRepository {
    /** Minutes after midnight that [on] starts, per the setting in force that day. */
    fun observeDayStartMinutes(on: LocalDate): Flow<Int>

    /**
     * Days from [effectiveFrom] on start [dayStartMinutes] after midnight. Setting the same
     * [effectiveFrom] again replaces that setting. Fails with [DomainError.Validation] unless
     * [dayStartMinutes] is in `0..1439`.
     */
    suspend fun setDayStart(dayStartMinutes: Int, effectiveFrom: LocalDate): Outcome<Unit>
}

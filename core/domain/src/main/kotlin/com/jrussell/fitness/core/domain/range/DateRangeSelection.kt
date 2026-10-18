package com.jrussell.fitness.core.domain.range

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.minus

/**
 * The date range a summary screen shows (Home, a tab's Log and Trends views): the presets from
 * design decision #38, plus All. Every range ends today; "today" already accounts for the user's
 * day start.
 */
sealed interface DateRangeSelection {
    /** Everything: from the earliest data through today. Picking it clears a narrower range. */
    data object All : DateRangeSelection

    /** Just today. */
    data object Today : DateRangeSelection

    /** The last [days] days, today included. */
    data class LastDays(val days: Int) : DateRangeSelection

    /** From [start] through today. A future [start] clamps to today. */
    data class From(val start: LocalDate) : DateRangeSelection
}

private const val DAYS_SHOWN_BY_CHARTS_ON_TODAY: Int = 7

/**
 * The range cards summarize for this selection, ending [today]. [earliest] is the date of the
 * screen's earliest data, where All starts; with no data, All is just today.
 */
fun DateRangeSelection.cardRange(today: LocalDate, earliest: LocalDate? = null): LocalDateRange {
    return when (this) {
        DateRangeSelection.All -> minOf(earliest ?: today, today)..today
        DateRangeSelection.Today -> today..today
        is DateRangeSelection.LastDays -> today.minus(DatePeriod(days = days - 1))..today
        is DateRangeSelection.From -> minOf(start, today)..today
    }
}

/**
 * The range charts draw for this selection. Like [cardRange], except that Today draws the last
 * 7 days: a one-day chart would show almost nothing (owner decision, 2026-10-17).
 */
fun DateRangeSelection.chartRange(today: LocalDate, earliest: LocalDate? = null): LocalDateRange {
    return when (this) {
        DateRangeSelection.Today -> DateRangeSelection.LastDays(DAYS_SHOWN_BY_CHARTS_ON_TODAY).cardRange(today)
        else -> cardRange(today, earliest)
    }
}

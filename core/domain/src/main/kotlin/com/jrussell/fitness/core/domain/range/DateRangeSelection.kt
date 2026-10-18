package com.jrussell.fitness.core.domain.range

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.minus

/**
 * The date range a summary screen shows (Home, a tab's Trends view): the presets from design
 * decision #38. Every range ends today; "today" already accounts for the user's day start.
 */
sealed interface DateRangeSelection {
    /** Just today (the default). */
    data object Today : DateRangeSelection

    /** The last [days] days, today included. */
    data class LastDays(val days: Int) : DateRangeSelection

    /** From [start] through today. A future [start] clamps to today. */
    data class From(val start: LocalDate) : DateRangeSelection
}

private const val DAYS_SHOWN_BY_CHARTS_ON_TODAY: Int = 7

/** The range cards summarize for this selection, ending [today]. */
fun DateRangeSelection.cardRange(today: LocalDate): LocalDateRange {
    return when (this) {
        DateRangeSelection.Today -> today..today
        is DateRangeSelection.LastDays -> today.minus(DatePeriod(days = days - 1))..today
        is DateRangeSelection.From -> minOf(start, today)..today
    }
}

/**
 * The range charts draw for this selection. Like [cardRange], except that Today draws the last
 * 7 days: a one-day chart would show almost nothing (owner decision, 2026-10-17).
 */
fun DateRangeSelection.chartRange(today: LocalDate): LocalDateRange {
    return when (this) {
        DateRangeSelection.Today -> DateRangeSelection.LastDays(DAYS_SHOWN_BY_CHARTS_ON_TODAY).cardRange(today)
        else -> cardRange(today)
    }
}

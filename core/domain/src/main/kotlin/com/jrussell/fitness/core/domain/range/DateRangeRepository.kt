package com.jrussell.fitness.core.domain.range

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** A screen whose range choice is remembered, each separately, with its default. */
enum class RangeScreen(val defaultSelection: DateRangeSelection) {
    /** Home opens on today's summary. */
    HOME(DateRangeSelection.Today),

    /** Body (Log and Trends share one range) shows all weigh-ins until the user narrows it. */
    BODY(DateRangeSelection.All),
}

/**
 * The range each screen last showed, kept across restarts so a chosen start date ("start fresh
 * from June 1") sticks. Implemented in `:core:data`.
 */
interface DateRangeRepository {
    /** The remembered range for [screen], or its default; emits again when it changes. */
    fun selection(screen: RangeScreen): Flow<DateRangeSelection>

    suspend fun setSelection(screen: RangeScreen, selection: DateRangeSelection)
}

private const val ALL: String = "ALL"
private const val TODAY: String = "TODAY"
private const val LAST_DAYS_PREFIX: String = "LAST_DAYS:"
private const val FROM_PREFIX: String = "FROM:"

/** [selection] as stable text for storage ("ALL", "TODAY", "LAST_DAYS:30", "FROM:2026-06-01"). */
fun encodeDateRangeSelection(selection: DateRangeSelection): String {
    return when (selection) {
        DateRangeSelection.All -> ALL
        DateRangeSelection.Today -> TODAY
        is DateRangeSelection.LastDays -> "$LAST_DAYS_PREFIX${selection.days}"
        is DateRangeSelection.From -> "$FROM_PREFIX${selection.start}"
    }
}

/** The selection [text] encodes, or `null` when it is missing or unreadable (callers use the default). */
fun decodeDateRangeSelection(text: String?): DateRangeSelection? {
    return when {
        text == ALL -> DateRangeSelection.All
        text == TODAY -> DateRangeSelection.Today
        text == null -> null
        text.startsWith(LAST_DAYS_PREFIX) ->
            text.removePrefix(LAST_DAYS_PREFIX).toIntOrNull()?.takeIf { it > 0 }?.let(DateRangeSelection::LastDays)
        text.startsWith(FROM_PREFIX) ->
            runCatching { LocalDate.parse(text.removePrefix(FROM_PREFIX)) }.getOrNull()?.let(DateRangeSelection::From)
        else -> null
    }
}

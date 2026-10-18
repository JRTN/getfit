package com.jrussell.fitness.core.domain.range

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

class DateRangeCodecTest {
    @Test
    fun everySelection_roundTrips() {
        val selections: List<DateRangeSelection> = listOf(
            DateRangeSelection.All,
            DateRangeSelection.Today,
            DateRangeSelection.LastDays(30),
            DateRangeSelection.From(LocalDate(2026, 6, 1)),
        )

        selections.forEach { selection: DateRangeSelection ->
            assertThat(decodeDateRangeSelection(encodeDateRangeSelection(selection))).isEqualTo(selection)
        }
    }

    @Test
    fun unreadableText_decodesToNull() {
        listOf(null, "", "SOMETIME", "LAST_DAYS:x", "LAST_DAYS:0", "FROM:2026-13-40").forEach { text: String? ->
            assertThat(decodeDateRangeSelection(text)).isNull()
        }
    }
}

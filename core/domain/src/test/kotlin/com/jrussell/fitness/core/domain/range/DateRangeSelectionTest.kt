package com.jrussell.fitness.core.domain.range

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import org.junit.jupiter.api.Test

class DateRangeSelectionTest {
    private val today: LocalDate = LocalDate(2026, 10, 17)

    @Test
    fun all_startsAtTheEarliestData() {
        val earliest: LocalDate = LocalDate(2026, 8, 22)

        assertThat(DateRangeSelection.All.cardRange(today, earliest)).isEqualTo(range(earliest, today))
        assertThat(DateRangeSelection.All.chartRange(today, earliest)).isEqualTo(range(earliest, today))
    }

    @Test
    fun all_noData_isJustToday() {
        assertThat(DateRangeSelection.All.cardRange(today, earliest = null)).isEqualTo(range(today, today))
    }

    @Test
    fun today_cardIsOneDay_chartIsLastSevenDays() {
        assertThat(DateRangeSelection.Today.cardRange(today)).isEqualTo(range(today, today))
        assertThat(DateRangeSelection.Today.chartRange(today)).isEqualTo(range(LocalDate(2026, 10, 11), today))
    }

    @Test
    fun lastThirtyDays_includesToday() {
        assertThat(DateRangeSelection.LastDays(30).cardRange(today)).isEqualTo(range(LocalDate(2026, 9, 18), today))
    }

    @Test
    fun dateRangeSelection_futureStart_clampsToToday() {
        assertThat(DateRangeSelection.From(LocalDate(2026, 10, 20)).cardRange(today)).isEqualTo(range(today, today))
    }

    @Test
    fun from_pastStart_runsToToday() {
        assertThat(DateRangeSelection.From(LocalDate(2026, 10, 1)).cardRange(today))
            .isEqualTo(range(LocalDate(2026, 10, 1), today))
    }

    private fun range(start: LocalDate, end: LocalDate): LocalDateRange {
        return start..end
    }
}

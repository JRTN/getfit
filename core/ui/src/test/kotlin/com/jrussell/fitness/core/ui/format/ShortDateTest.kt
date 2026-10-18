package com.jrussell.fitness.core.ui.format

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.util.Locale
import kotlinx.datetime.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShortDateTest {
    private val today: LocalDate = LocalDate(2026, 10, 18)

    @Test
    fun sameYear_omitsTheYear() {
        assertThat(formatShortDate(LocalDate(2026, 9, 19), today, Locale.US)).isEqualTo("Sep 19")
    }

    @Test
    fun earlierYear_includesIt() {
        assertThat(formatShortDate(LocalDate(2025, 6, 1), today, Locale.US)).isEqualTo("Jun 1, 2025")
    }

    @Test
    fun followsTheLocale() {
        assertThat(formatShortDate(LocalDate(2026, 9, 19), today, Locale.GERMANY)).isEqualTo("19. Sept.")
    }
}

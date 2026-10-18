package com.jrussell.fitness.core.model

import assertk.assertThat
import assertk.assertions.containsExactly
import org.junit.jupiter.api.Test

/**
 * These enums are stored by constant name, in `user.db` (Room's default) and in the JSON export.
 * Renaming a constant would make every existing row that holds the old name unreadable.
 *
 * If this test fails, don't just update the list: add a migration that rewrites the stored
 * names (and a JSON import mapping), or keep the old name.
 */
class StoredEnumNamesTest {
    @Test
    fun storedEnumNames_arePinned() {
        assertThat(MeasurementSource.entries.map { it.name }).containsExactly("MANUAL", "HEALTH_CONNECT", "IMPORT")
        assertThat(MeasurementCategory.entries.map { it.name })
            .containsExactly("BODY", "VITAL", "ACTIVITY", "LAB", "CUSTOM")
        assertThat(DailyAggregation.entries.map { it.name }).containsExactly("SUM", "MEAN", "LAST", "MIN", "MAX")
        assertThat(Sex.entries.map { it.name }).containsExactly("MALE", "FEMALE")
    }
}

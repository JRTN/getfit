package com.jrussell.fitness.core.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class DisplayUnitTest {
    @Test
    fun displayCodes_roundTripAndAreUnique() {
        DisplayUnit.entries.forEach { unit: DisplayUnit ->
            assertThat(displayUnitOf(unit.code)).isEqualTo(unit)
        }
        val codes: List<String> = DisplayUnit.entries.map { unit: DisplayUnit -> unit.code }
        assertThat(codes.toSet().size).isEqualTo(codes.size)
        assertThat(displayUnitOf("stone")).isNull()
    }

    @Test
    fun massUnits_mapToLbAndKg() {
        assertThat(MassUnit.POUND.displayUnit().code).isEqualTo("lb")
        assertThat(MassUnit.KILOGRAM.displayUnit().code).isEqualTo("kg")
    }
}

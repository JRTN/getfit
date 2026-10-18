package com.jrussell.fitness.core.model

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class MeasurementUnitTest {
    @Test
    fun everyStandardCode_roundTrips() {
        StandardUnit.entries.forEach { unit: StandardUnit ->
            assertThat(measurementUnitOf(unit.code)).isEqualTo(unit)
        }
    }

    @Test
    fun standardCodes_areUnique() {
        val codes: List<String> = StandardUnit.entries.map { unit: StandardUnit -> unit.code }

        assertThat(codes.toSet().size).isEqualTo(codes.size)
        assertThat(codes).containsExactlyInAnyOrder("kg", "m", "fraction", "kcal", "s", "bpm", "mmHg", "count")
    }

    @Test
    fun custom_keepsLabelAndHasNoDimension() {
        val unit: MeasurementUnit? = measurementUnitOf("custom:mg/dL")

        assertThat(unit).isEqualTo(CustomUnit("mg/dL"))
        assertThat(unit?.code).isEqualTo("custom:mg/dL")
        assertThat(unit?.dimension).isNull()
    }

    @Test
    fun unknownCode_isNull() {
        assertThat(measurementUnitOf("furlong")).isNull()
        assertThat(measurementUnitOf("")).isNull()
    }

    @Test
    fun blankCustom_isNull() {
        assertThat(measurementUnitOf("custom:")).isNull()
        assertThat(measurementUnitOf("custom:   ")).isNull()
    }
}

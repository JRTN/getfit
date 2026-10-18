package com.jrussell.fitness.core.model

import assertk.assertThat
import assertk.assertions.isCloseTo
import org.junit.jupiter.api.Test

class MassTest {
    @Test
    fun mass_poundsRoundTrip() {
        val mass: Mass = Mass.ofPounds(181.7)

        assertThat(mass.kilograms).isCloseTo(82.417733629, 1e-9)
        assertThat(mass.toPounds()).isCloseTo(181.7, 1e-9)
    }

    @Test
    fun mass_inUnit() {
        val mass: Mass = Mass.ofKilograms(100.0)

        assertThat(mass.inUnit(MassUnit.KILOGRAM)).isCloseTo(100.0, 1e-12)
        assertThat(mass.inUnit(MassUnit.POUND)).isCloseTo(220.46226, 1e-5)
    }
}

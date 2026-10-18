package com.jrussell.fitness.feature.body

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.jrussell.fitness.core.model.MassUnit
import java.util.Locale
import org.junit.Test

class WeightInputTest {
    @Test
    fun weightInput_usDecimal_parses() {
        val result: WeightInputResult.Valid =
            parseWeightInput("181.7", MassUnit.POUND, Locale.US) as WeightInputResult.Valid

        assertThat(result.mass.kilograms).isCloseTo(82.417733629, 1e-9)
        assertThat(result.entered).isEqualTo(181.7)
    }

    @Test
    fun weightInput_germanComma_parses() {
        val result: WeightInputResult.Valid =
            parseWeightInput("82,4", MassUnit.KILOGRAM, Locale.GERMANY) as WeightInputResult.Valid

        assertThat(result.mass.kilograms).isCloseTo(82.4, 1e-9)
    }

    @Test
    fun weightInput_germanDot_isNotANumber() {
        // With grouping on, German parsing would read "80.5" as 805 (a valid 365 kg in pounds).
        assertThat(parseWeightInput("80.5", MassUnit.POUND, Locale.GERMANY))
            .isEqualTo(WeightInputResult.Invalid(WeightInputError.NOT_A_NUMBER))
    }

    @Test
    fun weightInput_empty_isEmpty() {
        assertThat(parseWeightInput("  ", MassUnit.KILOGRAM, Locale.US))
            .isEqualTo(WeightInputResult.Invalid(WeightInputError.EMPTY))
    }

    @Test
    fun weightInput_letters_isNotANumber() {
        assertThat(parseWeightInput("abc", MassUnit.KILOGRAM, Locale.US))
            .isEqualTo(WeightInputResult.Invalid(WeightInputError.NOT_A_NUMBER))
    }

    @Test
    fun weightInput_zero_isOutOfRange() {
        assertThat(parseWeightInput("0", MassUnit.KILOGRAM, Locale.US))
            .isEqualTo(WeightInputResult.Invalid(WeightInputError.OUT_OF_RANGE))
    }

    @Test
    fun weightInput_negative_isOutOfRange() {
        assertThat(parseWeightInput("-5", MassUnit.KILOGRAM, Locale.US))
            .isEqualTo(WeightInputResult.Invalid(WeightInputError.OUT_OF_RANGE))
    }

    @Test
    fun weightInput_huge_isOutOfRange() {
        assertThat(parseWeightInput("9999", MassUnit.POUND, Locale.US))
            .isEqualTo(WeightInputResult.Invalid(WeightInputError.OUT_OF_RANGE))
    }

    @Test
    fun weightInput_trailingGarbage_isNotANumber() {
        assertThat(parseWeightInput("82.4kg", MassUnit.KILOGRAM, Locale.US))
            .isEqualTo(WeightInputResult.Invalid(WeightInputError.NOT_A_NUMBER))
    }

    @Test
    fun weightInput_surroundingSpaces_parse() {
        assertThat(parseWeightInput(" 82.4 ", MassUnit.KILOGRAM, Locale.US)).isInstanceOf<WeightInputResult.Valid>()
    }
}

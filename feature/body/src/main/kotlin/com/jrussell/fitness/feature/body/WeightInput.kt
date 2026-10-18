package com.jrussell.fitness.feature.body

import com.jrussell.fitness.core.model.Mass
import com.jrussell.fitness.core.model.MassUnit
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale

/** The lightest weight accepted, in kilograms. Anything lower is almost certainly a typo. */
const val WEIGHT_MIN_KILOGRAMS: Double = 20.0

/** The heaviest weight accepted, in kilograms. */
const val WEIGHT_MAX_KILOGRAMS: Double = 400.0

/** The result of reading a typed weight. */
sealed interface WeightInputResult {
    /** A usable weight: [mass] for storage, [entered] as typed (in the unit it was typed in). */
    data class Valid(val mass: Mass, val entered: Double) : WeightInputResult

    /** Not a usable weight, for the reason in [reason]. */
    data class Invalid(val reason: WeightInputError) : WeightInputResult
}

/** Why a typed weight was rejected; each maps to a field error message. */
enum class WeightInputError {
    EMPTY,
    NOT_A_NUMBER,
    OUT_OF_RANGE,
}

/**
 * Reads [text] as a weight in [unit], using [locale]'s decimal separator ("82.4" in the US,
 * "82,4" in Germany). The whole text must be a number: "82.4kg" is rejected rather than read as
 * 82.4. Grouping separators are not accepted, so a German "80.5" is rejected instead of being
 * read as 805. Valid weights are [WEIGHT_MIN_KILOGRAMS]–[WEIGHT_MAX_KILOGRAMS] kg.
 */
fun parseWeightInput(text: String, unit: MassUnit, locale: Locale): WeightInputResult {
    val trimmed: String = text.trim()
    val format: NumberFormat = NumberFormat.getInstance(locale)
    format.isGroupingUsed = false
    val position: ParsePosition = ParsePosition(0)
    val number: Number? = if (trimmed.isEmpty()) null else format.parse(trimmed, position)
    val entered: Double? = number?.toDouble()?.takeIf { position.index == trimmed.length }
    val mass: Mass? = entered?.let { value: Double -> Mass.of(value, unit) }
    return when {
        trimmed.isEmpty() -> WeightInputResult.Invalid(WeightInputError.EMPTY)
        entered == null || mass == null -> WeightInputResult.Invalid(WeightInputError.NOT_A_NUMBER)
        mass.kilograms !in WEIGHT_MIN_KILOGRAMS..WEIGHT_MAX_KILOGRAMS ->
            WeightInputResult.Invalid(WeightInputError.OUT_OF_RANGE)
        else -> WeightInputResult.Valid(mass, entered)
    }
}

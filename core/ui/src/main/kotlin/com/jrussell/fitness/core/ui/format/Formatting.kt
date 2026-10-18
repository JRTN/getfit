package com.jrussell.fitness.core.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.ui.R
import java.text.NumberFormat
import java.util.Locale

private const val MINUS_SIGN: String = "−"

/** [value] with exactly one decimal place in [locale]'s format ("212.4", or "212,4" in German). */
fun formatOneDecimal(value: Double, locale: Locale = Locale.getDefault()): String {
    val format: NumberFormat = NumberFormat.getNumberInstance(locale)
    format.minimumFractionDigits = 1
    format.maximumFractionDigits = 1
    return format.format(value)
}

/** Like [formatOneDecimal] but always signed ("+0.4", "−1.1"), for changes. Zero has no sign. */
fun formatSignedOneDecimal(value: Double, locale: Locale = Locale.getDefault()): String {
    val magnitude: String = formatOneDecimal(kotlin.math.abs(value), locale)
    return when {
        magnitude == formatOneDecimal(0.0, locale) -> magnitude
        value > 0 -> "+$magnitude"
        else -> "$MINUS_SIGN$magnitude"
    }
}

/** The short symbol shown next to a mass ("lb", "kg"). */
@Composable
fun massUnitSymbol(unit: MassUnit): String {
    return when (unit) {
        MassUnit.POUND -> stringResource(R.string.unit_symbol_pound)
        MassUnit.KILOGRAM -> stringResource(R.string.unit_symbol_kilogram)
    }
}

/** The unit as TalkBack should say it ("pounds", "kilograms"). */
@Composable
fun massUnitSpoken(unit: MassUnit): String {
    return when (unit) {
        MassUnit.POUND -> stringResource(R.string.unit_spoken_pound)
        MassUnit.KILOGRAM -> stringResource(R.string.unit_spoken_kilogram)
    }
}

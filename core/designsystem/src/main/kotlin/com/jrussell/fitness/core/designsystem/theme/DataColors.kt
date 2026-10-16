package com.jrussell.fitness.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.jrussell.fitness.core.model.ColorPalette

/**
 * App-specific semantic colours for data: chart series, macros, target states and deltas.
 * Material's `ColorScheme` covers surfaces, text and the accent; this covers everything a chart
 * or a nutrient total needs. Read it with `GetFitTheme.dataColors`.
 *
 * Colour never carries meaning alone: every use pairs it with an icon, a sign, a pattern or a label.
 */
@Immutable
data class DataColors(
    /** Six distinct colours for plotted series, in assignment order. */
    val seriesPalette: List<Color>,
    val macroProtein: Color,
    val macroCarbohydrate: Color,
    val macroFat: Color,
    val targetMet: Color,
    val targetUnder: Color,
    val targetOver: Color,
    val deltaPositive: Color,
    val deltaNegative: Color,
    /** Decorative; exempt from contrast requirements. */
    val chartGridLine: Color,
    val chartAxisLabel: Color,
    /** Drawn dashed: projected values. */
    val projectionLine: Color,
    /** Drawn solid: observed values. */
    val actualLine: Color,
)

/** The current [DataColors]; provided by `GetFitTheme`. */
val LocalDataColors: ProvidableCompositionLocal<DataColors> = staticCompositionLocalOf {
    dataColorsFor(ColorPalette.DEFAULT, dark = false)
}

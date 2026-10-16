package com.jrussell.fitness.core.designsystem.theme

import com.jrussell.fitness.core.model.ColorPalette

/** Minimum WCAG contrast ratios a palette must meet. */
data class ContrastRequirement(
    /** For text and the accent used as text. */
    val text: Double,
    /** For chart marks, icons and other non-text graphics. */
    val graphic: Double,
)

private const val STANDARD_TEXT: Double = 4.5
private const val STANDARD_GRAPHIC: Double = 3.0
private const val HIGH_CONTRAST_TEXT: Double = 7.0
private const val HIGH_CONTRAST_GRAPHIC: Double = 4.5

/** The spec's 4.5:1 / 3:1, raised to 7:1 / 4.5:1 for the high-contrast palette. */
fun contrastRequirementFor(palette: ColorPalette): ContrastRequirement {
    return when (palette) {
        ColorPalette.HIGH_CONTRAST -> ContrastRequirement(text = HIGH_CONTRAST_TEXT, graphic = HIGH_CONTRAST_GRAPHIC)
        ColorPalette.DEFAULT, ColorPalette.COLOUR_VISION_SAFE ->
            ContrastRequirement(text = STANDARD_TEXT, graphic = STANDARD_GRAPHIC)
    }
}

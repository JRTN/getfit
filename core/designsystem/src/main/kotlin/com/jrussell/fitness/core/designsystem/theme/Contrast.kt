package com.jrussell.fitness.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private const val LINEAR_THRESHOLD: Double = 0.03928
private const val LINEAR_DIVISOR: Double = 12.92
private const val GAMMA_OFFSET: Double = 0.055
private const val GAMMA_DIVISOR: Double = 1.055
private const val GAMMA: Double = 2.4
private const val RED_WEIGHT: Double = 0.2126
private const val GREEN_WEIGHT: Double = 0.7152
private const val BLUE_WEIGHT: Double = 0.0722
private const val FLARE: Double = 0.05

/** WCAG 2.x relative luminance of an opaque sRGB colour, from 0 (black) to 1 (white). */
fun relativeLuminance(color: Color): Double {
    return RED_WEIGHT * linearize(color.red) + GREEN_WEIGHT * linearize(color.green) +
        BLUE_WEIGHT * linearize(color.blue)
}

/**
 * WCAG 2.x contrast ratio between two opaque colours, from 1:1 (identical) to 21:1 (black on
 * white). Order does not matter. Text needs at least 4.5 and graphics at least 3 (stricter for
 * the high-contrast palette, see [contrastRequirementFor]).
 */
fun contrastRatio(foreground: Color, background: Color): Double {
    val first: Double = relativeLuminance(foreground)
    val second: Double = relativeLuminance(background)
    return (max(first, second) + FLARE) / (min(first, second) + FLARE)
}

private fun linearize(channel: Float): Double {
    val value: Double = channel.toDouble()
    return if (value <= LINEAR_THRESHOLD) {
        value / LINEAR_DIVISOR
    } else {
        ((value + GAMMA_OFFSET) / GAMMA_DIVISOR).pow(GAMMA)
    }
}

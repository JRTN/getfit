package com.jrussell.fitness.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.jrussell.fitness.core.model.ColorPalette

/** The Material color scheme for one palette instance. Dynamic (wallpaper) color is never used. */
fun colorSchemeFor(palette: ColorPalette, dark: Boolean): ColorScheme {
    val tokens: SchemeTokens = schemeTokensFor(palette, dark)
    return if (dark) {
        darkColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            primaryContainer = tokens.secondaryContainer,
            onPrimaryContainer = tokens.onBackground,
            secondary = tokens.primary,
            onSecondary = tokens.onPrimary,
            secondaryContainer = tokens.secondaryContainer,
            onSecondaryContainer = tokens.onBackground,
            background = tokens.background,
            onBackground = tokens.onBackground,
            surface = tokens.background,
            onSurface = tokens.onBackground,
            surfaceVariant = tokens.surfaceContainer,
            onSurfaceVariant = tokens.onSurfaceVariant,
            surfaceContainerLowest = tokens.background,
            surfaceContainerLow = tokens.surfaceContainer,
            surfaceContainer = tokens.surfaceContainer,
            surfaceContainerHigh = tokens.surfaceContainer,
            surfaceContainerHighest = tokens.surfaceContainer,
            outline = tokens.outline,
            outlineVariant = tokens.outlineVariant,
            error = dataTokensFor(palette, dark).targetOver,
            onError = tokens.onPrimary,
        )
    } else {
        lightColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            primaryContainer = tokens.secondaryContainer,
            onPrimaryContainer = tokens.onBackground,
            secondary = tokens.primary,
            onSecondary = tokens.onPrimary,
            secondaryContainer = tokens.secondaryContainer,
            onSecondaryContainer = tokens.onBackground,
            background = tokens.background,
            onBackground = tokens.onBackground,
            surface = tokens.background,
            onSurface = tokens.onBackground,
            surfaceVariant = tokens.surfaceContainer,
            onSurfaceVariant = tokens.onSurfaceVariant,
            surfaceContainerLowest = tokens.background,
            surfaceContainerLow = tokens.surfaceContainer,
            surfaceContainer = tokens.surfaceContainer,
            surfaceContainerHigh = tokens.surfaceContainer,
            surfaceContainerHighest = tokens.surfaceContainer,
            outline = tokens.outline,
            outlineVariant = tokens.outlineVariant,
            error = dataTokensFor(palette, dark).targetOver,
            onError = tokens.onPrimary,
        )
    }
}

/** The data colors for one palette instance; see [DataColors]. */
fun dataColorsFor(palette: ColorPalette, dark: Boolean): DataColors {
    val scheme: SchemeTokens = schemeTokensFor(palette, dark)
    val data: DataTokens = dataTokensFor(palette, dark)
    return DataColors(
        seriesPalette = data.series.map { color: Color? -> color ?: scheme.primary },
        macroProtein = data.macroProtein,
        macroCarbohydrate = data.macroCarbohydrate,
        macroFat = data.macroFat,
        targetMet = data.targetMet,
        targetUnder = data.targetUnder,
        targetOver = data.targetOver,
        deltaPositive = data.deltaPositive,
        deltaNegative = data.deltaNegative,
        chartGridLine = scheme.outlineVariant,
        chartAxisLabel = scheme.onSurfaceVariant,
        projectionLine = data.projectionLine ?: scheme.primary,
        actualLine = scheme.onBackground,
    )
}

private fun schemeTokensFor(palette: ColorPalette, dark: Boolean): SchemeTokens {
    return when (palette) {
        ColorPalette.DEFAULT, ColorPalette.COLOR_VISION_SAFE -> if (dark) StandardDarkScheme else StandardLightScheme
        ColorPalette.HIGH_CONTRAST -> if (dark) HighContrastDarkScheme else HighContrastLightScheme
    }
}

private fun dataTokensFor(palette: ColorPalette, dark: Boolean): DataTokens {
    return when (palette) {
        ColorPalette.DEFAULT -> if (dark) DefaultDarkData else DefaultLightData
        ColorPalette.COLOR_VISION_SAFE -> if (dark) ColorVisionSafeDarkData else ColorVisionSafeLightData
        ColorPalette.HIGH_CONTRAST -> if (dark) DefaultDarkData else HighContrastLightData
    }
}

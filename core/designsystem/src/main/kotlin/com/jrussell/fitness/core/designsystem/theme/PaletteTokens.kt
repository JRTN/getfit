package com.jrussell.fitness.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// The raw values of the six palette instances. Every pair is checked by PaletteContrastTest; when
// changing a value here, run it. Values were derived by keeping each hue and moving lightness
// until the WCAG thresholds pass (design doc: "Palette matrix"). `targetOver` doubles as Material's
// `error`, so it must also pass as text and under white `onError` text.

/** Neutral and accent values that become a Material `ColorScheme`. */
internal data class SchemeTokens(
    val background: Color,
    val surfaceContainer: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color,
    val outlineVariant: Color,
    val outline: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondaryContainer: Color,
)

/** Data colors that differ per instance; the rest are derived from [SchemeTokens]. */
internal data class DataTokens(
    /** Six series colors; `null` first entry means "use the accent". */
    val series: List<Color?>,
    val macroProtein: Color,
    val macroCarbohydrate: Color,
    val macroFat: Color,
    val targetMet: Color,
    val targetUnder: Color,
    val targetOver: Color,
    val deltaPositive: Color,
    val deltaNegative: Color,
    /** `null` means "use the accent". */
    val projectionLine: Color?,
)

internal val StandardLightScheme: SchemeTokens = SchemeTokens(
    background = Color(0xFFF7F7F8),
    surfaceContainer = Color(0xFFEFEFF2),
    onBackground = Color(0xFF1B1B1E),
    onSurfaceVariant = Color(0xFF5F5F68),
    outlineVariant = Color(0xFFE2E2E6),
    outline = Color(0xFF85858F),
    primary = Color(0xFF6941C6),
    onPrimary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0DAF0),
)

internal val StandardDarkScheme: SchemeTokens = SchemeTokens(
    background = Color(0xFF111113),
    surfaceContainer = Color(0xFF1B1B1F),
    onBackground = Color(0xFFEDEDEF),
    onSurfaceVariant = Color(0xFFA3A3AD),
    outlineVariant = Color(0xFF2A2A2F),
    outline = Color(0xFF6E6E78),
    primary = Color(0xFFC3B1FF),
    onPrimary = Color(0xFF111113),
    secondaryContainer = Color(0xFF433E55),
)

internal val HighContrastLightScheme: SchemeTokens = SchemeTokens(
    background = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF2F2F4),
    onBackground = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF3A3A40),
    outlineVariant = Color(0xFFCFCFD6),
    outline = Color(0xFF5A5A62),
    primary = Color(0xFF4B2A9E),
    onPrimary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2DDF0),
)

internal val HighContrastDarkScheme: SchemeTokens = SchemeTokens(
    background = Color(0xFF000000),
    surfaceContainer = Color(0xFF141416),
    onBackground = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFCFCFD6),
    outlineVariant = Color(0xFF3A3A40),
    outline = Color(0xFFA0A0AA),
    primary = Color(0xFFD9CCFF),
    onPrimary = Color(0xFF000000),
    secondaryContainer = Color(0xFF3D3947),
)

internal val DefaultLightData: DataTokens = DataTokens(
    series = listOf(
        null,
        Color(0xFF0F8B8D),
        Color(0xFFBD7C03),
        Color(0xFFD1477A),
        Color(0xFF3A7BD5),
        Color(0xFF6B8E23),
    ),
    macroProtein = Color(0xFF0F8B8D),
    macroCarbohydrate = Color(0xFFBD7C03),
    macroFat = Color(0xFFD1477A),
    targetMet = Color(0xFF1E7B34),
    targetUnder = Color(0xFFC77700),
    targetOver = Color(0xFFB42318),
    deltaPositive = Color(0xFF1E7B34),
    deltaNegative = Color(0xFFB42318),
    projectionLine = null,
)

/** Also used by the high-contrast dark instance, whose black background clears 4.5:1 with these. */
internal val DefaultDarkData: DataTokens = DataTokens(
    series = listOf(
        null,
        Color(0xFF4CC9C0),
        Color(0xFFF2B33D),
        Color(0xFFF27CA6),
        Color(0xFF7FB0FF),
        Color(0xFFA8C66C),
    ),
    macroProtein = Color(0xFF4CC9C0),
    macroCarbohydrate = Color(0xFFF2B33D),
    macroFat = Color(0xFFF27CA6),
    targetMet = Color(0xFF6FCF8A),
    targetUnder = Color(0xFFF5B547),
    targetOver = Color(0xFFFF8A80),
    deltaPositive = Color(0xFF6FCF8A),
    deltaNegative = Color(0xFFFF8A80),
    projectionLine = null,
)

internal val ColorVisionSafeLightData: DataTokens = DataTokens(
    series = listOf(
        Color(0xFF0072B2),
        Color(0xFFD55E00),
        Color(0xFF009B71),
        Color(0xFFC76C9F),
        Color(0xFFB87F00),
        Color(0xFF1B90D2),
    ),
    macroProtein = Color(0xFF009B71),
    macroCarbohydrate = Color(0xFF0072B2),
    macroFat = Color(0xFFD55E00),
    targetMet = Color(0xFF0072B2),
    targetUnder = Color(0xFFB87F00),
    targetOver = Color(0xFFB44F00),
    deltaPositive = Color(0xFF0072B2),
    deltaNegative = Color(0xFFD55E00),
    projectionLine = Color(0xFFC76C9F),
)

internal val ColorVisionSafeDarkData: DataTokens = DataTokens(
    series = listOf(
        Color(0xFF3D9BD6),
        Color(0xFFE36F1A),
        Color(0xFF009E73),
        Color(0xFFCC79A7),
        Color(0xFFE69F00),
        Color(0xFF56B4E9),
    ),
    macroProtein = Color(0xFF009E73),
    macroCarbohydrate = Color(0xFF3D9BD6),
    macroFat = Color(0xFFE36F1A),
    targetMet = Color(0xFF3D9BD6),
    targetUnder = Color(0xFFE69F00),
    targetOver = Color(0xFFE36F1A),
    deltaPositive = Color(0xFF3D9BD6),
    deltaNegative = Color(0xFFE36F1A),
    projectionLine = Color(0xFFCC79A7),
)

internal val HighContrastLightData: DataTokens = DataTokens(
    series = listOf(
        null,
        Color(0xFF0D7B7D),
        Color(0xFF986303),
        Color(0xFFC83269),
        Color(0xFF2B6DC8),
        Color(0xFF5A771D),
    ),
    macroProtein = Color(0xFF0D7B7D),
    macroCarbohydrate = Color(0xFF986303),
    macroFat = Color(0xFFC83269),
    targetMet = Color(0xFF1E7B34),
    targetUnder = Color(0xFF9E5F00),
    targetOver = Color(0xFF9D1F15),
    deltaPositive = Color(0xFF1E7B34),
    deltaNegative = Color(0xFFB42318),
    projectionLine = null,
)

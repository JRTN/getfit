package com.jrussell.fitness.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// M0 stub palette: neutral greys plus one placeholder accent. The real accent hue, the
// colour-vision-safe and high-contrast palettes, and DataColors arrive in M1.

private val Grey050: Color = Color(0xFFF7F7F8)
private val Grey100: Color = Color(0xFFEDEDEF)
private val Grey300: Color = Color(0xFFC4C4C8)
private val Grey600: Color = Color(0xFF6B6B72)
private val Grey800: Color = Color(0xFF2A2A2E)
private val Grey900: Color = Color(0xFF1B1B1E)
private val Grey950: Color = Color(0xFF111113)
private val AccentLight: Color = Color(0xFF2F5BD3)
private val AccentDark: Color = Color(0xFF9DB5FF)

/** Light-theme Material colour scheme for the M0 stub. */
internal val LightColors: ColorScheme = lightColorScheme(
    primary = AccentLight,
    onPrimary = Color.White,
    background = Grey050,
    onBackground = Grey900,
    surface = Grey050,
    onSurface = Grey900,
    surfaceVariant = Grey100,
    onSurfaceVariant = Grey600,
    outline = Grey300,
)

/** Dark-theme Material colour scheme for the M0 stub. */
internal val DarkColors: ColorScheme = darkColorScheme(
    primary = AccentDark,
    onPrimary = Grey950,
    background = Grey950,
    onBackground = Grey100,
    surface = Grey950,
    onSurface = Grey100,
    surfaceVariant = Grey800,
    onSurfaceVariant = Grey300,
    outline = Grey600,
)

package com.jrussell.fitness.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.jrussell.fitness.core.model.ColorPalette
import com.jrussell.fitness.core.model.ThemeMode

/**
 * Whether this preference means "use the dark theme", given the system's current setting.
 *
 * @param systemInDarkTheme the system dark-theme setting, from `isSystemInDarkTheme()`.
 */
fun ThemeMode.resolveDark(systemInDarkTheme: Boolean): Boolean {
    return when (this) {
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}

/**
 * The app's root theme. Wrap all UI in it once, at the top of `setContent`.
 *
 * Provides Material's color scheme, the Inter type scale and 2–4 dp shapes, plus GetFit's
 * [DataColors] (read with [GetFitTheme.dataColors]). Dynamic (wallpaper-derived) color is
 * deliberately never used, so chart colors stay predictable.
 *
 * @param themeMode the user's light/dark preference.
 * @param palette the user's palette preference.
 */
@Composable
fun GetFitTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: ColorPalette = ColorPalette.DEFAULT,
    content: @Composable () -> Unit,
) {
    val dark: Boolean = themeMode.resolveDark(isSystemInDarkTheme())
    CompositionLocalProvider(LocalDataColors provides dataColorsFor(palette, dark)) {
        MaterialTheme(
            colorScheme = colorSchemeFor(palette, dark),
            typography = GetFitTypography,
            shapes = GetFitShapes,
            content = content,
        )
    }
}

/** Accessors for GetFit's own theme values, alongside `MaterialTheme`. */
object GetFitTheme {
    /** The current palette's data colors (chart series, macros, targets, deltas). */
    val dataColors: DataColors
        @Composable
        @ReadOnlyComposable
        get() {
            return LocalDataColors.current
        }
}

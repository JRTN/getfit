package com.jrussell.fitness.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
 * Dynamic (wallpaper-derived) colour is deliberately off so chart colours stay predictable.
 *
 * @param themeMode the user's preference; [ThemeMode.SYSTEM] until settings exist (M1).
 */
@Composable
fun GetFitTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val colorScheme: ColorScheme = if (themeMode.resolveDark(isSystemInDarkTheme())) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}

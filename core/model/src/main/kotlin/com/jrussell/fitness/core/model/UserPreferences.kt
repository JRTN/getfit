package com.jrussell.fitness.core.model

/**
 * UI preferences: how the app looks. Settings that change how past data is evaluated (day start,
 * energy mode, targets) are not here; they are versioned rows in `user.db`.
 */
data class UserPreferences(val themeMode: ThemeMode, val palette: ColorPalette) {
    companion object {
        /** What a fresh install uses: follow the system theme, default palette. */
        val DEFAULT: UserPreferences = UserPreferences(themeMode = ThemeMode.SYSTEM, palette = ColorPalette.DEFAULT)
    }
}

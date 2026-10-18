package com.jrussell.fitness.core.model

/**
 * UI preferences: how the app looks and which units it shows (display only; storage is always
 * canonical). Settings that change how past data is evaluated (day start, energy mode, targets)
 * are not here; they are versioned rows in `user.db`.
 */
data class UserPreferences(
    val themeMode: ThemeMode,
    val palette: ColorPalette,
    val massUnit: MassUnit = MassUnit.KILOGRAM,
) {
    companion object {
        /**
         * Fallbacks: follow the system theme, default palette, kilograms. The weight-unit default
         * a fresh install actually gets comes from the device locale (chosen in `:app`).
         */
        val DEFAULT: UserPreferences = UserPreferences(themeMode = ThemeMode.SYSTEM, palette = ColorPalette.DEFAULT)
    }
}

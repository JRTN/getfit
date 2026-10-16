package com.jrussell.fitness.core.model

/** The user's light/dark preference. Stored in DataStore from M1; resolved at the root of the UI. */
enum class ThemeMode {
    /** Follow the system's dark-theme setting (the default). */
    SYSTEM,

    /** Always light. */
    LIGHT,

    /** Always dark. */
    DARK,
}

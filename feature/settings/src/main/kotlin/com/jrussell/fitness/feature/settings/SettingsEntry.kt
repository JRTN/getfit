package com.jrussell.fitness.feature.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/**
 * Registers this feature's screens with the app's Navigation 3 entry provider (called from `:app`).
 *
 * @param onOpenCharts opens Settings › Charts; `:app` decides how (as a sub-page of the overlay).
 */
fun EntryProviderScope<NavKey>.settingsEntry(onOpenCharts: () -> Unit) {
    entry<SettingsKey> {
        SettingsScreen(onOpenCharts = onOpenCharts)
    }
    entry<ChartSettingsKey> {
        ChartSettingsScreen()
    }
}

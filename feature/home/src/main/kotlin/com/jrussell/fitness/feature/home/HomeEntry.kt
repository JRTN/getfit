package com.jrussell.fitness.feature.home

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/**
 * Registers this feature's screens with the app's Navigation 3 entry provider (called from `:app`).
 *
 * @param onOpenBody switches to the Body tab when the Body card is tapped.
 */
fun EntryProviderScope<NavKey>.homeEntry(onOpenBody: () -> Unit) {
    entry<HomeKey> {
        HomeScreen(onOpenBody = onOpenBody)
    }
}

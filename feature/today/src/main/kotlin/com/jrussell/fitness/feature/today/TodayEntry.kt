package com.jrussell.fitness.feature.today

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/** Registers this feature's screens with the app's Navigation 3 entry provider (called from `:app`). */
fun EntryProviderScope<NavKey>.todayEntry() {
    entry<TodayKey> {
        TodayScreen()
    }
}

package com.jrussell.fitness.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Back stack for bottom-tab navigation: each tab keeps its own history, and one overlay screen
 * (Settings) can sit on top of whichever tab is showing.
 *
 * Navigation 3's `NavDisplay` shows a single list of keys, [backStack]. This class builds that
 * list as "start tab's stack, then the current tab's stack, then the overlay". As a result,
 * pressing back at the root of any other tab returns to the start tab (Home) instead of
 * leaving the app, and switching back to a tab restores where you were inside it.
 *
 * The overlay belongs to no tab: it appears at most once, closes when you switch tabs, and is
 * the first thing back closes.
 *
 * [topLevelKey], [overlay] and [backStack] are Compose state, so the UI recomposes when they change.
 *
 * @param startKey the root key of the start tab, which is always at the bottom of [backStack].
 */
class TopLevelBackStack<K : Any>(private val startKey: K) {
    /** Per-tab stacks, each beginning with its tab's root key. Insertion order is irrelevant. */
    private val tabStacks: LinkedHashMap<K, MutableList<K>> = linkedMapOf(startKey to mutableListOf(startKey))

    /** The root key of the currently selected tab. */
    var topLevelKey: K by mutableStateOf(startKey)
        private set

    /** The screen shown over the current tab, or `null`. */
    var overlay: K? by mutableStateOf(null)
        private set

    /** The flattened list of keys for `NavDisplay`; the last entry is the visible screen. */
    val backStack: SnapshotStateList<K> = mutableStateListOf(startKey)

    /**
     * Selects the tab whose root is [key], restoring its history, and closes any overlay.
     * Selecting the current tab only closes the overlay.
     */
    fun switchTo(key: K) {
        overlay = null
        if (key != topLevelKey) {
            tabStacks.getOrPut(key) { mutableListOf(key) }
            topLevelKey = key
        }
        rebuild()
    }

    /** Opens [key] on top of the current tab's history, closing any overlay. */
    fun push(key: K) {
        overlay = null
        currentStack().add(key)
        rebuild()
    }

    /** Shows [key] over the current tab. Opening the overlay that is already shown does nothing. */
    fun openOverlay(key: K) {
        overlay = key
        rebuild()
    }

    /** Hides the overlay, if any. */
    fun closeOverlay() {
        overlay = null
        rebuild()
    }

    /**
     * Handles a back press: closes the overlay first, then pops the current tab, then returns to
     * the start tab.
     *
     * @return `false` only when the start tab's root is the sole entry, meaning the app should
     *   leave the foreground; `true` when something was closed or popped.
     */
    fun removeLast(): Boolean {
        val current: MutableList<K> = currentStack()
        if (overlay != null) {
            overlay = null
        } else if (current.size > 1) {
            current.removeAt(current.lastIndex)
        } else if (topLevelKey != startKey) {
            tabStacks.remove(topLevelKey)
            topLevelKey = startKey
        } else {
            return false
        }
        rebuild()
        return true
    }

    private fun currentStack(): MutableList<K> {
        return tabStacks.getValue(topLevelKey)
    }

    private fun rebuild() {
        val flattened: MutableList<K> = tabStacks.getValue(startKey).toMutableList()
        if (topLevelKey != startKey) {
            flattened.addAll(currentStack())
        }
        overlay?.let { key: K -> flattened.add(key) }
        backStack.clear()
        backStack.addAll(flattened)
    }
}

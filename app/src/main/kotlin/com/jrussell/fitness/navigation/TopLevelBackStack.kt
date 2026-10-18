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
 * The screens shown are "start tab's stack, then the current tab's stack, then the overlay"
 * ([backStack]). As a result, pressing back at the root of any other tab returns to the start tab
 * (Home) instead of leaving the app, and switching back to a tab restores where you were inside it.
 * Each tab's own stack ([stackFor]) stays available while the tab is hidden, so the UI can keep that
 * tab's screen state (see `TabNavDisplay`).
 *
 * The overlay belongs to no tab: it appears at most once, closes when you switch tabs, and is
 * the first thing back closes.
 *
 * [topLevelKey], [overlay], [backStack] and the tab stacks are Compose state, so the UI recomposes
 * when they change.
 *
 * @param startKey the root key of the start tab, which is always at the bottom of [backStack].
 * @param tabs the root keys of every tab, [startKey] included.
 */
class TopLevelBackStack<K : Any>(private val startKey: K, val tabs: List<K> = listOf(startKey)) {
    /** Per-tab stacks, each beginning with its tab's root key; one for every tab, from the start. */
    private val tabStacks: Map<K, SnapshotStateList<K>> = (listOf(startKey) + tabs).distinct()
        .associateWith { root: K -> mutableStateListOf(root) }

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
        require(key in tabStacks) { "$key is not one of the tabs" }
        overlay = null
        topLevelKey = key
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
        val current: SnapshotStateList<K> = currentStack()
        if (overlay != null) {
            overlay = null
        } else if (current.size > 1) {
            current.removeAt(current.lastIndex)
        } else if (topLevelKey != startKey) {
            topLevelKey = startKey
        } else {
            return false
        }
        rebuild()
        return true
    }

    /** The history of the tab whose root is [tab] (just its root until something is pushed). */
    fun stackFor(tab: K): List<K> {
        return tabStacks.getValue(tab)
    }

    /** The tabs whose stacks are on screen, bottom first: the start tab, then the current tab. */
    val visibleTabs: List<K>
        get() {
            return if (topLevelKey == startKey) listOf(startKey) else listOf(startKey, topLevelKey)
        }

    /** The overlay as a stack of zero or one screens, to draw after [visibleTabs]. */
    val overlayStack: List<K>
        get() {
            return listOfNotNull(overlay)
        }

    private fun currentStack(): SnapshotStateList<K> {
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

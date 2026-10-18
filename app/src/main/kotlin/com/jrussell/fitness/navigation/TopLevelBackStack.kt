package com.jrussell.fitness.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Back stack for bottom-tab navigation: each tab keeps its own history, and an overlay screen
 * (Settings, with any of its sub-pages) can sit on top of whichever tab is showing.
 *
 * Navigation 3's `NavDisplay` shows a single list of keys, [backStack]. This class builds that
 * list as "start tab's stack, then the current tab's stack, then the overlay and its sub-pages". So
 * pressing back at the root of any other tab returns to the start tab (Home) instead of
 * leaving the app, and switching back to a tab restores where you were inside it.
 *
 * The overlay belongs to no tab: it appears at most once, closes (with its sub-pages) when you
 * switch tabs, and back closes its top sub-page first, then the overlay itself.
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

    /** The overlay (first) and its open sub-pages, in order. */
    private val overlays: SnapshotStateList<K> = mutableStateListOf()

    /** The visible overlay screen (a sub-page if one is open), or `null`. */
    val overlay: K?
        get() {
            return overlays.lastOrNull()
        }

    /** The flattened list of keys for `NavDisplay`; the last entry is the visible screen. */
    val backStack: SnapshotStateList<K> = mutableStateListOf(startKey)

    /**
     * Selects the tab whose root is [key], restoring its history, and closes any overlay.
     * Selecting the current tab only closes the overlay.
     */
    fun switchTo(key: K) {
        overlays.clear()
        if (key != topLevelKey) {
            tabStacks.getOrPut(key) { mutableListOf(key) }
            topLevelKey = key
        }
        rebuild()
    }

    /** Opens [key] on top of the current tab's history, closing any overlay. */
    fun push(key: K) {
        overlays.clear()
        currentStack().add(key)
        rebuild()
    }

    /** Shows [key] over the current tab, closing any sub-pages. Opening the shown overlay again changes nothing. */
    fun openOverlay(key: K) {
        overlays.clear()
        overlays.add(key)
        rebuild()
    }

    /** Closes the top overlay screen: a sub-page returns to its parent, the overlay to the tab. */
    fun closeOverlay() {
        overlays.removeLastOrNull()
        rebuild()
    }

    /** Opens [key] as a sub-page of the overlay (Settings › Charts). Back returns to the overlay. */
    fun pushOverlay(key: K) {
        overlays.add(key)
        rebuild()
    }

    /**
     * Handles a back press: closes the top overlay screen first, then pops the current tab, then
     * returns to the start tab.
     *
     * @return `false` only when the start tab's root is the sole entry, meaning the app should
     *   leave the foreground; `true` when something was closed or popped.
     */
    fun removeLast(): Boolean {
        val current: MutableList<K> = currentStack()
        if (overlays.isNotEmpty()) {
            overlays.removeAt(overlays.lastIndex)
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
        flattened.addAll(overlays)
        backStack.clear()
        backStack.addAll(flattened)
    }
}

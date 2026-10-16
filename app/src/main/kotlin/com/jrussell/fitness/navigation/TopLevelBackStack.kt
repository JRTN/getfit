package com.jrussell.fitness.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Back stack for bottom-tab navigation: each tab keeps its own history.
 *
 * Navigation 3's `NavDisplay` shows a single list of keys, [backStack]. This class builds that
 * list from per-tab stacks as "start tab's stack, then the current tab's stack". As a result,
 * pressing back at the root of any other tab returns to the start tab (Today) instead of
 * leaving the app, and switching back to a tab restores where you were inside it.
 *
 * Both [topLevelKey] and [backStack] are Compose state, so the UI recomposes when they change.
 *
 * @param startKey the root key of the start tab, which is always at the bottom of [backStack].
 */
class TopLevelBackStack<K : Any>(private val startKey: K) {
    /** Per-tab stacks, each beginning with its tab's root key. Insertion order is irrelevant. */
    private val tabStacks: LinkedHashMap<K, MutableList<K>> = linkedMapOf(startKey to mutableListOf(startKey))

    /** The root key of the currently selected tab. */
    var topLevelKey: K by mutableStateOf(startKey)
        private set

    /** The flattened list of keys for `NavDisplay`; the last entry is the visible screen. */
    val backStack: SnapshotStateList<K> = mutableStateListOf(startKey)

    /** Selects the tab whose root is [key], restoring its history. Selecting the current tab does nothing. */
    fun switchTo(key: K) {
        if (key == topLevelKey) {
            return
        }
        tabStacks.getOrPut(key) { mutableListOf(key) }
        topLevelKey = key
        rebuild()
    }

    /** Opens [key] on top of the current tab's history. */
    fun push(key: K) {
        currentStack().add(key)
        rebuild()
    }

    /**
     * Handles a back press.
     *
     * @return `false` only when the start tab's root is the sole entry, meaning the app should
     *   exit; `true` when something was popped.
     */
    fun removeLast(): Boolean {
        val current: MutableList<K> = currentStack()
        if (current.size > 1) {
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
        backStack.clear()
        backStack.addAll(flattened)
    }
}

package com.jrussell.fitness.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.jrussell.fitness.navigation.TopLevelBackStack

/**
 * Shows [navigation]'s screens with Navigation 3, keeping every tab's screen state while the tab
 * is hidden (Navigation 3's "multiple back stacks" pattern).
 *
 * A single `NavDisplay` over the flattened back stack would forget a tab's saved UI state and
 * ViewModels as soon as you switched away, because its entries leave the list. Here each tab's
 * stack, and the overlay, is decorated separately with its own saveable-state and ViewModel
 * decorators; all of them stay in composition, and only the visible ones are handed to `NavDisplay`.
 * An entry's state is dropped only when it is popped from its own stack.
 *
 * @param onExit called if a back press reaches [TopLevelBackStack.removeLast] with nothing left to
 *   close. Navigation 3 1.2.0 stops intercepting back at the start tab's root, so in practice
 *   Android's default runs there (moving the app to the background); this is the fallback.
 */
@Composable
fun <K : Any> TabNavDisplay(
    navigation: TopLevelBackStack<K>,
    onExit: () -> Unit,
    entryProvider: (K) -> NavEntry<K>,
    modifier: Modifier = Modifier,
) {
    val entriesByTab: Map<K, List<NavEntry<K>>> = navigation.tabs.associateWith { tab: K ->
        key(tab) {
            rememberDecoratedNavEntries(
                backStack = navigation.stackFor(tab),
                entryDecorators = rememberEntryDecorators(),
                entryProvider = entryProvider,
            )
        }
    }
    val overlayEntries: List<NavEntry<K>> = rememberDecoratedNavEntries(
        backStack = navigation.overlayStack,
        entryDecorators = rememberEntryDecorators(),
        entryProvider = entryProvider,
    )
    NavDisplay(
        entries = navigation.visibleTabs.flatMap { tab: K -> entriesByTab.getValue(tab) } + overlayEntries,
        modifier = modifier,
        onBack = {
            if (!navigation.removeLast()) {
                onExit()
            }
        },
    )
}

/** One stack's decorators: saved UI state (`rememberSaveable`) and ViewModels, per entry. */
@Composable
private fun <K : Any> rememberEntryDecorators(): List<NavEntryDecorator<K>> {
    return listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator())
}

package com.jrussell.fitness.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.jrussell.fitness.R
import com.jrussell.fitness.feature.body.bodyEntry
import com.jrussell.fitness.feature.nutrition.nutritionEntry
import com.jrussell.fitness.feature.settings.R as SettingsR
import com.jrussell.fitness.feature.settings.SettingsKey
import com.jrussell.fitness.feature.settings.settingsEntry
import com.jrussell.fitness.feature.today.todayEntry
import com.jrussell.fitness.feature.training.trainingEntry
import com.jrussell.fitness.feature.trends.trendsEntry
import com.jrussell.fitness.navigation.TopLevelBackStack
import com.jrussell.fitness.navigation.TopLevelDestination

/**
 * The app shell: a top bar with the screen title and a Settings action, the five-tab bottom
 * bar, and Navigation 3's `NavDisplay` showing the current screen between them.
 *
 * @param navigation the tab back stack (owned by `MainViewModel`).
 * @param onExit called when back is pressed on Today's root screen; the activity finishes.
 */
@Composable
fun GetFitApp(navigation: TopLevelBackStack<NavKey>, onExit: () -> Unit) {
    val currentDestination: TopLevelDestination = TopLevelDestination.forKey(navigation.topLevelKey)
    val showingSettings: Boolean = navigation.backStack.lastOrNull() == SettingsKey

    Scaffold(
        topBar = {
            GetFitTopBar(
                title = if (showingSettings) {
                    stringResource(SettingsR.string.feature_settings_title)
                } else {
                    stringResource(currentDestination.label)
                },
                showSettingsAction = !showingSettings,
                onOpenSettings = { navigation.push(SettingsKey) },
            )
        },
        bottomBar = {
            GetFitBottomBar(
                currentDestination = currentDestination,
                onSelect = { destination: TopLevelDestination -> navigation.switchTo(destination.key) },
            )
        },
    ) { innerPadding: PaddingValues ->
        NavDisplay(
            backStack = navigation.backStack,
            modifier = Modifier.padding(innerPadding),
            onBack = {
                val popped: Boolean = navigation.removeLast()
                if (!popped) {
                    onExit()
                }
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                todayEntry()
                nutritionEntry()
                trainingEntry()
                bodyEntry()
                trendsEntry()
                settingsEntry()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GetFitTopBar(title: String, showSettingsAction: Boolean, onOpenSettings: () -> Unit) {
    TopAppBar(
        title = { Text(text = title) },
        actions = {
            if (showSettingsAction) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.action_open_settings),
                    )
                }
            }
        },
    )
}

@Composable
private fun GetFitBottomBar(currentDestination: TopLevelDestination, onSelect: (TopLevelDestination) -> Unit) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination: TopLevelDestination ->
            NavigationBarItem(
                selected = destination == currentDestination,
                onClick = { onSelect(destination) },
                // The label below already names the tab, so the icon is decorative for TalkBack.
                icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                label = { Text(text = stringResource(destination.label)) },
            )
        }
    }
}

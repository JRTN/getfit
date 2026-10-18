package com.jrussell.fitness.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.jrussell.fitness.R
import com.jrussell.fitness.core.designsystem.icon.GetFitIcons
import com.jrussell.fitness.feature.body.BodyKey
import com.jrussell.fitness.feature.body.bodyEntry
import com.jrussell.fitness.feature.home.homeEntry
import com.jrussell.fitness.feature.nutrition.nutritionEntry
import com.jrussell.fitness.feature.settings.R as SettingsR
import com.jrussell.fitness.feature.settings.SettingsKey
import com.jrussell.fitness.feature.settings.settingsEntry
import com.jrussell.fitness.feature.training.trainingEntry
import com.jrussell.fitness.navigation.TopLevelBackStack
import com.jrussell.fitness.navigation.TopLevelDestination

/**
 * The app shell: a top bar with the screen title and a Settings action, the four-tab bottom
 * bar, and [TabNavDisplay] showing the current screen between them (each tab keeps its state).
 *
 * @param navigation the tab back stack (owned by `MainViewModel`).
 * @param onExit the fallback for a back press with nothing left to close (see [TabNavDisplay]).
 */
@Composable
fun GetFitApp(navigation: TopLevelBackStack<NavKey>, onExit: () -> Unit) {
    val currentDestination: TopLevelDestination = TopLevelDestination.forKey(navigation.topLevelKey)
    val showingSettings: Boolean = navigation.overlay == SettingsKey

    Scaffold(
        topBar = {
            GetFitTopBar(
                title = if (showingSettings) {
                    stringResource(SettingsR.string.feature_settings_title)
                } else {
                    stringResource(currentDestination.label)
                },
                showingOverlay = showingSettings,
                onOpenSettings = { navigation.openOverlay(SettingsKey) },
                onCloseOverlay = { navigation.closeOverlay() },
            )
        },
        bottomBar = {
            GetFitBottomBar(
                currentDestination = currentDestination,
                onSelect = { destination: TopLevelDestination -> navigation.switchTo(destination.key) },
            )
        },
    ) { innerPadding: PaddingValues ->
        TabNavDisplay(
            navigation = navigation,
            onExit = onExit,
            modifier = Modifier.padding(innerPadding),
            entryProvider = entryProvider {
                homeEntry(onOpenBody = { navigation.switchTo(BodyKey) })
                nutritionEntry()
                trainingEntry()
                bodyEntry()
                settingsEntry()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GetFitTopBar(
    title: String,
    showingOverlay: Boolean,
    onOpenSettings: () -> Unit,
    onCloseOverlay: () -> Unit,
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            if (showingOverlay) {
                IconButton(onClick = onCloseOverlay) {
                    Icon(
                        painter = painterResource(GetFitIcons.Back),
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            }
        },
        actions = {
            if (!showingOverlay) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        painter = painterResource(GetFitIcons.Settings),
                        contentDescription = stringResource(R.string.action_open_settings),
                    )
                }
            }
        },
    )
}

@Composable
private fun GetFitBottomBar(currentDestination: TopLevelDestination, onSelect: (TopLevelDestination) -> Unit) {
    Column {
        // Flat design: a hairline separates the bar from content instead of tonal elevation.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
            TopLevelDestination.entries.forEach { destination: TopLevelDestination ->
                NavigationBarItem(
                    selected = destination == currentDestination,
                    onClick = { onSelect(destination) },
                    // The label below already names the tab, so the icon is decorative for TalkBack.
                    icon = {
                        val selected: Boolean = destination == currentDestination
                        Icon(
                            painter = painterResource(if (selected) destination.selectedIcon else destination.icon),
                            contentDescription = null,
                        )
                    },
                    label = {
                        // Four labels can't fit side by side at very large font scales, and
                        // breaking a word across lines is worse than a slightly smaller label. Cap
                        // the scale for these labels only; screen content keeps the full scale and
                        // TalkBack reads the full label.
                        val density: Density = LocalDensity.current
                        val cappedScale: Float = density.fontScale.coerceAtMost(MAX_TAB_LABEL_FONT_SCALE)
                        CompositionLocalProvider(LocalDensity provides Density(density.density, cappedScale)) {
                            Text(text = stringResource(destination.label), maxLines = 1, softWrap = false)
                        }
                    },
                )
            }
        }
    }
}

/** Largest font scale applied to bottom-bar labels; at 1.3× "Nutrition" fits a 412 dp-wide phone. */
private const val MAX_TAB_LABEL_FONT_SCALE: Float = 1.3f

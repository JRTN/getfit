package com.jrussell.fitness.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jrussell.fitness.core.designsystem.icon.GetFitIcons
import com.jrussell.fitness.core.designsystem.theme.GetFitTheme
import com.jrussell.fitness.core.model.ColorPalette
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.model.ThemeMode
import com.jrussell.fitness.core.model.UserPreferences

/**
 * The Settings screen: Appearance and Units, then rows that open sub-pages (Charts).
 *
 * @param onOpenCharts opens Settings › Charts.
 */
@Composable
fun SettingsScreen(
    onOpenCharts: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state: UserPreferences? by viewModel.uiState.collectAsStateWithLifecycle()
    // Nothing is drawn until the saved choices load (a few ms), so no default flashes first.
    state?.let { loaded: UserPreferences ->
        Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            AppearanceSettings(
                state = loaded,
                onThemeModeSelected = viewModel::onThemeModeSelected,
                onPaletteSelected = viewModel::onPaletteSelected,
            )
            UnitsSettings(state = loaded, onMassUnitSelected = viewModel::onMassUnitSelected)
            ChartsRow(onClick = onOpenCharts)
        }
    }
}

/**
 * Theme (System / Light / Dark) and color palette choices, as two radio groups. Each row is a
 * single selectable target at least 48 dp tall, announced by TalkBack as a radio button.
 */
@Composable
fun AppearanceSettings(
    state: UserPreferences,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onPaletteSelected: (ColorPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OptionGroup(
            title = R.string.settings_theme_title,
            options = ThemeMode.entries,
            selected = state.themeMode,
            label = ::themeModeLabel,
            onSelect = onThemeModeSelected,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        OptionGroup(
            title = R.string.settings_palette_title,
            options = ColorPalette.entries,
            selected = state.palette,
            label = ::paletteLabel,
            onSelect = onPaletteSelected,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** The unit weights are shown and typed in. Stored values are always kilograms, so switching is safe. */
@Composable
fun UnitsSettings(state: UserPreferences, onMassUnitSelected: (MassUnit) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        OptionGroup(
            title = R.string.settings_weight_unit_title,
            options = MassUnit.entries,
            selected = state.massUnit,
            label = ::massUnitLabel,
            onSelect = onMassUnitSelected,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Opens Settings › Charts: a full-width row with a chevron, at least 48 dp tall. */
@Composable
fun ChartsRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MIN_ROW_HEIGHT)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.settings_charts_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.settings_charts_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(painter = painterResource(GetFitIcons.ChevronRight), contentDescription = null)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun <T> OptionGroup(
    @StringRes title: Int,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().selectableGroup().padding(vertical = 8.dp)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { heading() },
        )
        options.forEach { option: T ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MIN_ROW_HEIGHT)
                    .selectable(
                        selected = option == selected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    .padding(horizontal = 16.dp),
            ) {
                RadioButton(selected = option == selected, onClick = null)
                Text(text = stringResource(label(option)), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@StringRes
private fun themeModeLabel(themeMode: ThemeMode): Int {
    return when (themeMode) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }
}

@StringRes
private fun paletteLabel(palette: ColorPalette): Int {
    return when (palette) {
        ColorPalette.DEFAULT -> R.string.settings_palette_default
        ColorPalette.COLOR_VISION_SAFE -> R.string.settings_palette_color_vision_safe
        ColorPalette.HIGH_CONTRAST -> R.string.settings_palette_high_contrast
    }
}

@StringRes
private fun massUnitLabel(unit: MassUnit): Int {
    return when (unit) {
        MassUnit.POUND -> R.string.settings_weight_unit_pounds
        MassUnit.KILOGRAM -> R.string.settings_weight_unit_kilograms
    }
}

internal val MIN_ROW_HEIGHT: Dp = 48.dp

@PreviewLightDark
@PreviewFontScale
@Composable
private fun AppearanceSettingsPreview() {
    GetFitTheme {
        AppearanceSettings(
            state = UserPreferences(ThemeMode.SYSTEM, ColorPalette.HIGH_CONTRAST),
            onThemeModeSelected = {},
            onPaletteSelected = {},
        )
    }
}

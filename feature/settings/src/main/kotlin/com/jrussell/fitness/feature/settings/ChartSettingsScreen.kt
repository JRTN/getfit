package com.jrussell.fitness.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jrussell.fitness.core.designsystem.theme.GetFitTheme
import com.jrussell.fitness.core.model.UserPreferences

/** Settings › Charts: how the built-in Nutrition, Training and Body charts are drawn. */
@Composable
fun ChartSettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val state: UserPreferences? by viewModel.uiState.collectAsStateWithLifecycle()
    // Like Settings, nothing is drawn until the saved choices load, so the switch never flips on open.
    state?.let { loaded: UserPreferences ->
        ChartSettings(
            state = loaded,
            onShowTrendLinesChanged = viewModel::onShowTrendLinesChanged,
            modifier = modifier.fillMaxSize(),
        )
    }
}

/**
 * The trend-line switch. The whole row is one toggle target, announced by TalkBack as a switch
 * with its on/off state. Custom charts (M2) carry their own setting on their edit screen.
 */
@Composable
fun ChartSettings(state: UserPreferences, onShowTrendLinesChanged: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MIN_ROW_HEIGHT)
                .toggleable(value = state.showTrendLines, role = Role.Switch, onValueChange = onShowTrendLinesChanged)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_trend_lines_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.settings_trend_lines_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = state.showTrendLines, onCheckedChange = null)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun ChartSettingsPreview() {
    GetFitTheme {
        ChartSettings(state = UserPreferences.DEFAULT, onShowTrendLinesChanged = {})
    }
}

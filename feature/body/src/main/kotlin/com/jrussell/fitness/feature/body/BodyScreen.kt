package com.jrussell.fitness.feature.body

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jrussell.fitness.core.designsystem.theme.GetFitTheme
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.model.ids.MeasurementId
import java.util.Locale
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private val SCREEN_PADDING: Dp = 16.dp

/** The two views of the Body tab (decision #38); Plan joins them in M5. */
private enum class BodyView(val label: Int) {
    LOG(R.string.body_view_log),
    TRENDS(R.string.body_view_trends),
}

/**
 * Everything the Body tab's content can ask for. The ViewModel supplies the real handlers;
 * tests and previews use [None].
 */
class BodyActions(
    val onAddWeight: () -> Unit = {},
    val onDismissEntry: () -> Unit = {},
    val onEntryEdited: () -> Unit = {},
    val onSaveWeight: (text: String, pickedDate: LocalDate?, pickedTime: LocalTime?, locale: Locale) -> Unit =
        { _, _, _, _ -> },
    val onDelete: (MeasurementId) -> Unit = {},
    val onRangeSelected: (DateRangeSelection) -> Unit = {},
) {
    companion object {
        /** Ignores every action. */
        val None: BodyActions = BodyActions()
    }
}

/** The Body tab: weigh-ins (Log) and the weight chart (Trends). Nothing is drawn until the first state loads. */
@Composable
fun BodyScreen(modifier: Modifier = Modifier, viewModel: BodyViewModel = hiltViewModel()) {
    val state: BodyUiState? by viewModel.uiState.collectAsStateWithLifecycle()
    val actions: BodyActions = BodyActions(
        onAddWeight = viewModel::onAddWeight,
        onDismissEntry = viewModel::onDismissEntry,
        onEntryEdited = viewModel::onEntryEdited,
        onSaveWeight = viewModel::onSaveWeight,
        onDelete = viewModel::onDelete,
        onRangeSelected = viewModel::onRangeSelected,
    )
    state?.let { loaded: BodyUiState -> BodyContent(state = loaded, actions = actions, modifier = modifier) }
}

/**
 * The Body tab's stateless content: a Log / Trends switch, then that view. The chosen view is
 * saved, so it survives switching tabs and recreation.
 */
@Composable
fun BodyContent(state: BodyUiState, actions: BodyActions, modifier: Modifier = Modifier) {
    var view: BodyView by rememberSaveable { mutableStateOf(BodyView.LOG) }
    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(start = SCREEN_PADDING, end = SCREEN_PADDING, top = 8.dp),
        ) {
            BodyView.entries.forEachIndexed { index: Int, option: BodyView ->
                SegmentedButton(
                    selected = view == option,
                    onClick = { view = option },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = BodyView.entries.size),
                ) {
                    Text(stringResource(option.label))
                }
            }
        }
        when (view) {
            BodyView.LOG -> BodyLogContent(state = state, actions = actions)
            BodyView.TRENDS -> BodyTrendsContent(state = state, onRangeSelected = actions.onRangeSelected)
        }
    }
    state.entry?.let { entry: WeightEntryState ->
        WeightEntryDialog(
            entry = entry,
            unit = state.unit,
            today = state.today,
            onDismiss = actions.onDismissEntry,
            onEdited = actions.onEntryEdited,
            onSave = actions.onSaveWeight,
        )
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun BodyContentPreview() {
    val today: LocalDate = LocalDate(2026, 10, 17)
    GetFitTheme {
        BodyContent(
            state = BodyUiState(
                today = today,
                unit = MassUnit.POUND,
                latest = LatestWeight(212.4, today, perWeek = -1.2, since = LocalDate(2026, 10, 11)),
                history = listOf(WeightRow(MeasurementId(Uuid.random()), today, LocalTime(7, 5), 212.4)),
                selection = DateRangeSelection.All,
                chartRange = LocalDate(2026, 10, 11)..today,
                chartWeighIns = emptyList(),
                entry = null,
            ),
            actions = BodyActions.None,
        )
    }
}

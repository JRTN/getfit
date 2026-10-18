package com.jrussell.fitness.feature.body

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jrussell.fitness.core.designsystem.icon.GetFitIcons
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.model.ids.MeasurementId
import com.jrussell.fitness.core.ui.format.formatOneDecimal
import com.jrussell.fitness.core.ui.format.formatShortDate
import com.jrussell.fitness.core.ui.format.formatSignedOneDecimal
import com.jrussell.fitness.core.ui.format.massUnitSpoken
import com.jrussell.fitness.core.ui.format.massUnitSymbol
import com.jrussell.fitness.core.ui.range.RangeChips
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime

private val SCREEN_PADDING: Dp = 16.dp
private val CARD_PADDING: Dp = 16.dp
private val HAIRLINE: Dp = 1.dp
private val MIN_ROW_HEIGHT: Dp = 56.dp

/**
 * The Log view: the range chips (shared with Trends), the latest weigh-in with its rate over the
 * range, "Add weight", then the weigh-ins inside the range with delete.
 */
@Composable
internal fun BodyLogContent(state: BodyUiState, actions: BodyActions, modifier: Modifier = Modifier) {
    var pendingDelete: WeightRow? by remember { mutableStateOf(null) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RangeChips(selection = state.selection, onSelect = actions.onRangeSelected, today = state.today)
        }
        state.latest?.let { latest: LatestWeight ->
            item { LatestWeightCard(latest = latest, unit = state.unit, today = state.today) }
        }
        item {
            Button(onClick = actions.onAddWeight, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.body_add_weight))
            }
        }
        item {
            Text(
                text = stringResource(R.string.body_history_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (state.history.isEmpty()) {
            item {
                Text(
                    text = stringResource(
                        if (state.latest == null) R.string.body_history_empty else R.string.body_history_empty_range,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(items = state.history, key = { row: WeightRow -> row.id.value.toString() }) { row: WeightRow ->
            Column {
                HistoryRow(row = row, unit = state.unit, today = state.today, onDelete = { pendingDelete = row })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
    pendingDelete?.let { row: WeightRow ->
        DeleteConfirmation(
            row = row,
            unit = state.unit,
            today = state.today,
            onConfirm = { id: MeasurementId ->
                actions.onDelete(id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** "212.4 lb · today" over "−1.2 lb/week since Oct 11", read by TalkBack as one sentence. */
@Composable
private fun LatestWeightCard(latest: LatestWeight, unit: MassUnit, today: LocalDate) {
    val value: String = formatOneDecimal(latest.value)
    val date: String = dateLabel(latest.date, today)
    val rate: String = formatSignedOneDecimal(latest.perWeek)
    val since: String = formatShortDate(latest.since, today)
    val spoken: String = stringResource(R.string.body_latest_spoken, value, massUnitSpoken(unit), date, rate, since)
    Surface(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(HAIRLINE, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = spoken },
    ) {
        Column(modifier = Modifier.padding(CARD_PADDING), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.body_latest_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.body_latest_value, value, massUnitSymbol(unit), date),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.body_latest_rate, rate, massUnitSymbol(unit), since),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * One weigh-in. The row reads as one sentence and offers Delete as a TalkBack custom action;
 * the visible delete button does the same for touch.
 */
@Composable
private fun HistoryRow(row: WeightRow, unit: MassUnit, today: LocalDate, onDelete: () -> Unit) {
    val value: String = formatOneDecimal(row.value)
    val date: String = dateLabel(row.date, today)
    val time: String = timeLabel(row.time)
    val spoken: String = stringResource(R.string.body_row_spoken, value, massUnitSpoken(unit), date, time)
    val deleteLabel: String = stringResource(R.string.body_delete)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MIN_ROW_HEIGHT)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                customActions = listOf(
                    CustomAccessibilityAction(deleteLabel) {
                        onDelete()
                        true
                    },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "$value ${massUnitSymbol(unit)}", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.body_row_when, date, time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(GetFitIcons.Delete),
                contentDescription = stringResource(R.string.body_delete_described, value, massUnitSpoken(unit), date),
            )
        }
    }
}

@Composable
private fun DeleteConfirmation(
    row: WeightRow,
    unit: MassUnit,
    today: LocalDate,
    onConfirm: (MeasurementId) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.body_delete_title)) },
        text = {
            Text(
                stringResource(
                    R.string.body_delete_text,
                    formatOneDecimal(row.value),
                    massUnitSymbol(unit),
                    dateLabel(row.date, today),
                    timeLabel(row.time),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(row.id) }) { Text(stringResource(R.string.body_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.body_cancel)) }
        },
    )
}

/** "today", or the date in the locale's medium format. */
@Composable
internal fun dateLabel(date: LocalDate, today: LocalDate): String {
    return if (date == today) {
        stringResource(R.string.body_today)
    } else {
        date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}

/** The time in the locale's short format ("8:05 AM", "08:05"). */
internal fun timeLabel(time: LocalTime): String {
    return time.toJavaLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
}

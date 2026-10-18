package com.jrussell.fitness.core.ui.range

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.ui.R
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate

private const val MILLIS_PER_DAY: Long = 86_400_000L
private const val SEVEN_DAYS: Int = 7
private const val THIRTY_DAYS: Int = 30

/**
 * The range presets shared by Home and the tabs (decision #38): All (clears any narrower range),
 * Today (only when [showToday]), 7 days, 30 days, and From…, which opens a date picker for the
 * start date. Dates after [today] can't be picked.
 * The chips form a single-choice group for TalkBack (radio role with selected state).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeChips(
    selection: DateRangeSelection,
    onSelect: (DateRangeSelection) -> Unit,
    today: LocalDate,
    modifier: Modifier = Modifier,
    showToday: Boolean = true,
) {
    var pickingStart: Boolean by rememberSaveable { mutableStateOf(false) }
    // Wraps onto a second line instead of scrolling, so every choice (and the From date) stays visible.
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RangeChip(
            label = stringResource(R.string.range_all),
            selected = selection == DateRangeSelection.All,
        ) { onSelect(DateRangeSelection.All) }
        if (showToday) {
            RangeChip(
                label = stringResource(R.string.range_today),
                selected = selection == DateRangeSelection.Today,
            ) { onSelect(DateRangeSelection.Today) }
        }
        RangeChip(
            label = stringResource(R.string.range_7_days),
            selected = selection == DateRangeSelection.LastDays(SEVEN_DAYS),
        ) { onSelect(DateRangeSelection.LastDays(SEVEN_DAYS)) }
        RangeChip(
            label = stringResource(R.string.range_30_days),
            selected = selection == DateRangeSelection.LastDays(THIRTY_DAYS),
        ) { onSelect(DateRangeSelection.LastDays(THIRTY_DAYS)) }
        val fromLabel: String = if (selection is DateRangeSelection.From) {
            val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            stringResource(R.string.range_from_date, selection.start.toJavaLocalDate().format(formatter))
        } else {
            stringResource(R.string.range_from)
        }
        RangeChip(label = fromLabel, selected = selection is DateRangeSelection.From) { pickingStart = true }
    }
    if (pickingStart) {
        PastDatePickerDialog(
            today = today,
            onPicked = { start: LocalDate ->
                pickingStart = false
                onSelect(DateRangeSelection.From(start))
            },
            onDismiss = { pickingStart = false },
        )
    }
}

@Composable
private fun RangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        modifier = Modifier.semantics { role = Role.RadioButton },
    )
}

/**
 * A Material 3 date picker dialog where nothing after [today] can be picked (no future range
 * starts or backfilled entries). It opens on [initial].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDatePickerDialog(
    today: LocalDate,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    initial: LocalDate = today,
) {
    val todayMillis: Long = today.toEpochDays() * MILLIS_PER_DAY
    val notAfterToday: SelectableDates = remember(todayMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= todayMillis
            }
        }
    }
    val state: DatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = minOf(initial, today).toEpochDays() * MILLIS_PER_DAY,
        selectableDates = notAfterToday,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis: Long = state.selectedDateMillis ?: todayMillis
                    onPicked(LocalDate.fromEpochDays(millis / MILLIS_PER_DAY))
                },
            ) { Text(text = stringResource(R.string.range_picker_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.range_picker_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

package com.jrussell.fitness.feature.body

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jrussell.fitness.core.model.Mass
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.ui.format.massUnitSymbol
import com.jrussell.fitness.core.ui.range.PastDatePickerDialog
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private const val MINUTES_PER_HOUR: Int = 60

/**
 * Adds a weigh-in: the weight in the user's unit, plus date and time rows that default to now.
 * Nothing after today can be picked. The date and time are passed on only if the user changed
 * them, so an untouched entry is filed by the day-start rule. Typed text survives rotation.
 */
@Composable
internal fun WeightEntryDialog(
    entry: WeightEntryState,
    unit: MassUnit,
    today: LocalDate,
    onDismiss: () -> Unit,
    onEdited: () -> Unit,
    onSave: (text: String, pickedDate: LocalDate?, pickedTime: LocalTime?, locale: Locale) -> Unit,
) {
    var text: String by rememberSaveable { mutableStateOf("") }
    var pickedEpochDay: Long? by rememberSaveable { mutableStateOf(null) }
    var pickedMinuteOfDay: Int? by rememberSaveable { mutableStateOf(null) }
    val pickedDate: LocalDate? = pickedEpochDay?.let { day: Long -> LocalDate.fromEpochDays(day) }
    val pickedTime: LocalTime? = pickedMinuteOfDay?.let { minutes: Int ->
        LocalTime(minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
    }
    val locale: Locale = LocalConfiguration.current.locales[0]
    val save: () -> Unit = { onSave(text, pickedDate, pickedTime, locale) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.body_entry_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WeightField(
                    text = text,
                    unit = unit,
                    error = entry.error,
                    onTextChange = { changed: String ->
                        text = changed
                        onEdited()
                    },
                    onDone = save,
                )
                WhenRows(
                    today = today,
                    pickedDate = pickedDate,
                    pickedTime = pickedTime,
                    onDatePicked = { date: LocalDate -> pickedEpochDay = date.toEpochDays() },
                    onTimePicked = { time: LocalTime ->
                        pickedMinuteOfDay = time.hour * MINUTES_PER_HOUR + time.minute
                    },
                )
            }
        },
        confirmButton = { TextButton(onClick = save) { Text(stringResource(R.string.body_entry_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.body_cancel)) } },
    )
}

/** The date and time rows ("today", "Now" until changed) and the pickers they open. */
@Composable
private fun WhenRows(
    today: LocalDate,
    pickedDate: LocalDate?,
    pickedTime: LocalTime?,
    onDatePicked: (LocalDate) -> Unit,
    onTimePicked: (LocalTime) -> Unit,
) {
    var pickingDate: Boolean by rememberSaveable { mutableStateOf(false) }
    var pickingTime: Boolean by rememberSaveable { mutableStateOf(false) }
    PickerRow(
        label = stringResource(R.string.body_entry_date),
        value = dateLabel(pickedDate ?: today, today),
        onClick = { pickingDate = true },
    )
    PickerRow(
        label = stringResource(R.string.body_entry_time),
        value = pickedTime?.let { time: LocalTime -> timeLabel(time) } ?: stringResource(R.string.body_entry_now),
        onClick = { pickingTime = true },
    )
    if (pickingDate) {
        PastDatePickerDialog(
            today = today,
            initial = pickedDate ?: today,
            onPicked = { date: LocalDate ->
                onDatePicked(date)
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
    if (pickingTime) {
        TimePickerDialog(
            initial = pickedTime,
            onPicked = { time: LocalTime ->
                onTimePicked(time)
                pickingTime = false
            },
            onDismiss = { pickingTime = false },
        )
    }
}

/** The weight field. Its error message is also set as the field's error semantics for TalkBack. */
@Composable
private fun WeightField(
    text: String,
    unit: MassUnit,
    error: WeightInputError?,
    onTextChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val errorText: String? = error?.let { reason: WeightInputError -> errorMessage(reason, unit) }
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        label = { Text(stringResource(R.string.body_entry_field, massUnitSymbol(unit))) },
        singleLine = true,
        isError = errorText != null,
        supportingText = errorText?.let { message: String -> { Text(message) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth().semantics {
            if (errorText != null) {
                error(errorText)
            }
        },
    )
}

@Composable
private fun PickerRow(label: String, value: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(value) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initial: LocalTime?, onPicked: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val start: java.time.LocalTime = java.time.LocalTime.now()
    val state: TimePickerState = rememberTimePickerState(
        initialHour = initial?.hour ?: start.hour,
        initialMinute = initial?.minute ?: start.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onPicked(LocalTime(state.hour, state.minute)) }) {
                Text(stringResource(R.string.body_picker_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.body_cancel)) } },
    )
}

/** The field error for [reason]; out-of-range names the accepted span in [unit], rounded inward. */
@Composable
private fun errorMessage(reason: WeightInputError, unit: MassUnit): String {
    return when (reason) {
        WeightInputError.EMPTY -> stringResource(R.string.body_error_empty)
        WeightInputError.NOT_A_NUMBER -> stringResource(R.string.body_error_not_a_number)
        WeightInputError.OUT_OF_RANGE -> stringResource(
            R.string.body_error_out_of_range,
            ceil(Mass.ofKilograms(WEIGHT_MIN_KILOGRAMS).inUnit(unit)).toInt(),
            floor(Mass.ofKilograms(WEIGHT_MAX_KILOGRAMS).inUnit(unit)).toInt(),
            massUnitSymbol(unit),
        )
    }
}

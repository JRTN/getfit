package com.jrussell.fitness.core.ui.chart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.ui.R
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate

private val ROW_VERTICAL_PADDING: Dp = 6.dp

/**
 * The chart's data as a table: one row per date (newest first), one column per series.
 * The accessible alternative to the drawn chart, and the "View as table" view in the spec.
 */
@Composable
internal fun ChartTable(series: List<ChartSeries>, formatValue: (Double) -> String, modifier: Modifier = Modifier) {
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val dates: List<LocalDate> = series.flatMap { one: ChartSeries ->
        one.points.map { point: DataPoint -> point.date }
    }
        .distinct()
        .sortedDescending()
    val byDate: List<Map<LocalDate, Double>> = series.map { one: ChartSeries ->
        one.points.associate { point: DataPoint -> point.date to point.value }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        TableRow(
            cells = listOf(stringResource(R.string.chart_table_date)) +
                series.map { one: ChartSeries -> "${one.label} (${one.unitSymbol})" },
            header = true,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        dates.forEach { date: LocalDate ->
            val values: List<String> = byDate.map { values: Map<LocalDate, Double> ->
                values[date]?.let(formatValue) ?: "–"
            }
            TableRow(cells = listOf(date.toJavaLocalDate().format(dateFormatter)) + values, header = false)
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean) {
    // One announcement per row ("Oct 17, 2026, 209.9, 210.3"), not one per cell.
    val rowModifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = ROW_VERTICAL_PADDING)
        .semantics(mergeDescendants = true) {}
    Row(modifier = rowModifier) {
        cells.forEachIndexed { index: Int, cell: String ->
            Text(
                text = cell,
                modifier = Modifier.weight(1f),
                style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textAlign = if (index == 0) TextAlign.Start else TextAlign.End,
            )
        }
    }
}

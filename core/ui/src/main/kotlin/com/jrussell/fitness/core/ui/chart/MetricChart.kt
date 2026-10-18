package com.jrussell.fitness.core.ui.chart

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jrussell.fitness.core.designsystem.theme.GetFitTheme
import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.ui.R
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toJavaLocalDate

private val LINE_THICKNESS: Dp = 2.5.dp
private val POINT_SIZE: Dp = 6.dp
private val LEGEND_SWATCH: Dp = 16.dp
private const val MIN_Y_PADDING: Double = 0.5
private const val Y_PADDING_FRACTION: Double = 0.1
private const val TARGET_DATE_LABELS: Int = 4

/**
 * The app's one chart component (spec: "The UI never calls a charting library directly"). It
 * draws [series] over [range] with Vico, and always offers:
 * - a spoken summary of the last series as the chart's content description;
 * - a "View as table" toggle showing the same data as rows;
 * - a legend, so series are told apart by label and shape (line vs dots), never by color alone.
 *
 * Raw observations ([SeriesStyle.POINTS]) are drawn as muted dots; derived series
 * ([SeriesStyle.LINE]) as solid lines in the palette's series colors.
 *
 * @param series values already in display units; the last series is the one summarized.
 * @param formatValue formats a value for axis labels, the table and the summary (locale-aware).
 */
@Composable
fun MetricChart(
    title: String,
    series: List<ChartSeries>,
    range: LocalDateRange,
    formatValue: (Double) -> String,
    modifier: Modifier = Modifier,
) {
    var showTable: Boolean by rememberSaveable { mutableStateOf(false) }
    val drawable: List<ChartSeries> = series.filter { one: ChartSeries -> one.points.isNotEmpty() }
    Column(modifier = modifier.fillMaxWidth()) {
        if (drawable.isEmpty()) {
            Text(
                text = stringResource(R.string.chart_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val summary: String? = spokenSummary(title, drawable.last(), range, formatValue)
            if (showTable) {
                ChartTable(series = drawable, formatValue = formatValue)
            } else {
                VicoChart(series = drawable, range = range, summary = summary, formatValue = formatValue)
            }
            Legend(series = drawable)
            TextButton(onClick = { showTable = !showTable }) {
                Text(text = stringResource(if (showTable) R.string.chart_view_chart else R.string.chart_view_table))
            }
        }
    }
}

@Composable
private fun VicoChart(
    series: List<ChartSeries>,
    range: LocalDateRange,
    summary: String?,
    formatValue: (Double) -> String,
) {
    val lines: List<LineCartesianLayer.Line> = rememberSeriesLines(series)
    val rangeProvider: CartesianLayerRangeProvider = rememberAxisRange(series, range)
    val model: CartesianChartModel = remember(series) { chartModel(series) }
    val dateLabelFormatter: DateTimeFormatter = remember {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMd"))
    }
    val labelSpacing: Int = max(1, (range.start.daysUntil(range.endInclusive) + 1) / TARGET_DATE_LABELS)
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(lines),
                rangeProvider = rangeProvider,
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = remember(formatValue) {
                    CartesianValueFormatter { _, value, _ -> formatValue(value) }
                },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = remember(dateLabelFormatter) {
                    CartesianValueFormatter { _, value, _ ->
                        LocalDate.fromEpochDays(value.toLong()).toJavaLocalDate().format(dateLabelFormatter)
                    }
                },
                itemPlacer = remember(labelSpacing) { HorizontalAxis.ItemPlacer.aligned(spacing = { labelSpacing }) },
            ),
        ),
        model = model,
        modifier = Modifier.semantics { summary?.let { text: String -> contentDescription = text } },
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
    )
}

/** Vico line styles: LINE series as solid colored lines, POINTS series as muted dots with no line. */
@Composable
private fun rememberSeriesLines(series: List<ChartSeries>): List<LineCartesianLayer.Line> {
    val colors: List<Color> = seriesColors(series)
    val pointColor: Color = MaterialTheme.colorScheme.outline
    return series.mapIndexed { index: Int, one: ChartSeries ->
        when (one.style) {
            SeriesStyle.LINE -> LineCartesianLayer.rememberLine(
                fill = remember(colors[index]) { LineCartesianLayer.LineFill.single(Fill(colors[index])) },
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = LINE_THICKNESS),
            )
            SeriesStyle.POINTS -> LineCartesianLayer.rememberLine(
                fill = remember { LineCartesianLayer.LineFill.single(Fill.Transparent) },
                pointProvider = LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(
                        rememberShapeComponent(fill = Fill(pointColor), shape = CircleShape),
                        POINT_SIZE,
                    ),
                ),
            )
        }
    }
}

/**
 * The plot bounds: x spans the whole [range] (so a sparse series still shows where it sits in
 * time), and y hugs the data with a little padding instead of starting at zero.
 */
@Composable
private fun rememberAxisRange(series: List<ChartSeries>, range: LocalDateRange): CartesianLayerRangeProvider {
    val values: List<Double> = series.flatMap { one: ChartSeries -> one.points.map { point: DataPoint -> point.value } }
    val minValue: Double = values.min()
    val maxValue: Double = values.max()
    val padding: Double = max(MIN_Y_PADDING, (maxValue - minValue) * Y_PADDING_FRACTION)
    return remember(range, minValue, maxValue) {
        CartesianLayerRangeProvider.fixed(
            minX = range.start.toEpochDays().toDouble(),
            maxX = range.endInclusive.toEpochDays().toDouble(),
            minY = minValue - padding,
            maxY = maxValue + padding,
        )
    }
}

/** One Vico series per [ChartSeries]; x is the epoch day, y the display value. */
private fun chartModel(series: List<ChartSeries>): CartesianChartModel {
    return CartesianChartModel(
        LineCartesianLayerModel.build {
            series.forEach { one: ChartSeries ->
                series(
                    x = one.points.map { point: DataPoint -> point.date.toEpochDays() },
                    y = one.points.map { point: DataPoint -> point.value },
                )
            }
        },
    )
}

/** One legend entry per series: a dot or line sample in the series' color, then its label. */
@Composable
private fun Legend(series: List<ChartSeries>) {
    val colors: List<Color> = seriesColors(series)
    val pointColor: Color = MaterialTheme.colorScheme.outline
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        series.forEachIndexed { index: Int, one: ChartSeries ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Canvas(modifier = Modifier.size(LEGEND_SWATCH)) {
                    when (one.style) {
                        SeriesStyle.LINE -> drawLine(
                            color = colors[index],
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = LINE_THICKNESS.toPx(),
                        )
                        SeriesStyle.POINTS -> drawCircle(color = pointColor, radius = POINT_SIZE.toPx() / 2)
                    }
                }
                Text(text = one.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Palette colors assigned in order to the LINE series; POINTS series take none. */
@Composable
private fun seriesColors(series: List<ChartSeries>): List<Color> {
    val palette: List<Color> = GetFitTheme.dataColors.seriesPalette
    var lineIndex: Int = 0
    return series.map { one: ChartSeries ->
        if (one.style == SeriesStyle.LINE) {
            palette[lineIndex++ % palette.size]
        } else {
            Color.Unspecified
        }
    }
}

@Preview(name = "Weight chart", fontScale = 1f)
@Preview(name = "Weight chart, 200% font", fontScale = 2f)
@Composable
private fun MetricChartPreview() {
    val start: LocalDate = LocalDate(2026, 9, 18)
    val weighIns: List<DataPoint> = (0 until 30 step 2).map { day: Int ->
        DataPoint(LocalDate.fromEpochDays(start.toEpochDays() + day), 212.0 - day * 0.08 + (day % 3) * 0.4, null)
    }
    val trend: List<DataPoint> = weighIns.map { point: DataPoint ->
        point.copy(value = 212.4 - (point.value - 212) * 0.1)
    }
    GetFitTheme {
        MetricChart(
            title = "Weight",
            series = listOf(
                ChartSeries("Weight", "lb", "pounds", weighIns, SeriesStyle.POINTS),
                ChartSeries("Trend", "lb", "pounds", trend, SeriesStyle.LINE),
            ),
            range = start..LocalDate(2026, 10, 17),
            formatValue = { value: Double -> String.format(Locale.US, "%.1f", value) },
        )
    }
}

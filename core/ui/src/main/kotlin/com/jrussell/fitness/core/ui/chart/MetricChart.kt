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
import androidx.compose.ui.graphics.PathEffect
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
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerPadding
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
private val EDGE_PADDING: Dp = 24.dp

/** The dash and gap of a dotted (TREND) line; with the line's round caps, short dashes read as dots. */
private val DOT_LENGTH: Dp = 3.dp
private val DOT_GAP: Dp = 5.dp
private const val MIN_Y_PADDING: Double = 0.5
private const val Y_PADDING_FRACTION: Double = 0.1
private const val TARGET_DATE_LABELS: Int = 4

/** How many value labels the y-axis shows (the default step placer can leave just one on a narrow range). */
private const val VALUE_LABELS: Int = 5

/**
 * The app's one chart component (spec: "The UI never calls a charting library directly"). It
 * draws [series] over [range] with Vico, and always offers:
 * - a spoken summary of the main series as the chart's content description;
 * - a "View as table" toggle showing the recorded values as rows, with the summary above them;
 * - a legend, so series are told apart by label and shape (solid vs dotted), never by color alone.
 *
 * Recorded values ([SeriesStyle.OBSERVED]) are drawn as a solid line with a dot on each point;
 * fitted lines ([SeriesStyle.TREND]) as dotted lines. Each series takes the next palette color.
 *
 * @param series values already in display units; the first [SeriesStyle.OBSERVED] series is the
 *   main one, summarized and listed first in the table.
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
            val observed: List<ChartSeries> = drawable.filter { one: ChartSeries -> one.style == SeriesStyle.OBSERVED }
            val summary: String? = spokenSummary(title, observed.firstOrNull() ?: drawable.first(), range, formatValue)
            if (showTable) {
                summary?.let { text: String ->
                    Text(text = text, style = MaterialTheme.typography.bodyMedium)
                }
                ChartTable(series = observed, formatValue = formatValue)
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
                itemPlacer = remember { VerticalAxis.ItemPlacer.count(count = { VALUE_LABELS }) },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = remember(dateLabelFormatter) {
                    CartesianValueFormatter { _, value, _ ->
                        LocalDate.fromEpochDays(value.toLong()).toJavaLocalDate().format(dateLabelFormatter)
                    }
                },
                itemPlacer = remember(labelSpacing) { HorizontalAxis.ItemPlacer.aligned(spacing = { labelSpacing }) },
            ),
            // Room past both ends so the first and last date labels and edge points are not clipped.
            layerPadding = { CartesianLayerPadding(unscalableStart = EDGE_PADDING, unscalableEnd = EDGE_PADDING) },
            // One x step is one day. Left to itself, Vico infers the step from the gaps between points
            // (7 days for weekly weigh-ins), and the date labels then land a step apart, often off-screen.
            getXStep = { 1.0 },
        ),
        model = model,
        modifier = Modifier.semantics { summary?.let { text: String -> contentDescription = text } },
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
    )
}

/** Vico line styles: OBSERVED as a solid line with a dot per point, TREND as a dotted line. */
@Composable
private fun rememberSeriesLines(series: List<ChartSeries>): List<LineCartesianLayer.Line> {
    val colors: List<Color> = seriesColors(series)
    return series.mapIndexed { index: Int, one: ChartSeries ->
        val fill: LineCartesianLayer.LineFill = remember(colors[index]) {
            LineCartesianLayer.LineFill.single(Fill(colors[index]))
        }
        when (one.style) {
            SeriesStyle.OBSERVED -> LineCartesianLayer.rememberLine(
                fill = fill,
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = LINE_THICKNESS),
                pointProvider = LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(
                        rememberShapeComponent(fill = Fill(colors[index]), shape = CircleShape),
                        POINT_SIZE,
                    ),
                ),
            )
            SeriesStyle.TREND -> LineCartesianLayer.rememberLine(
                fill = fill,
                stroke = LineCartesianLayer.LineStroke.Dashed(
                    thickness = LINE_THICKNESS,
                    dashLength = DOT_LENGTH,
                    gapLength = DOT_GAP,
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

/** One legend entry per series: a sample of its line (solid with a dot, or dotted), then its label. */
@Composable
private fun Legend(series: List<ChartSeries>) {
    val colors: List<Color> = seriesColors(series)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        series.forEachIndexed { index: Int, one: ChartSeries ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Canvas(modifier = Modifier.size(LEGEND_SWATCH)) {
                    val middle: Float = size.height / 2
                    drawLine(
                        color = colors[index],
                        start = Offset(0f, middle),
                        end = Offset(size.width, middle),
                        strokeWidth = LINE_THICKNESS.toPx(),
                        pathEffect = when (one.style) {
                            SeriesStyle.OBSERVED -> null
                            SeriesStyle.TREND -> PathEffect.dashPathEffect(
                                floatArrayOf(DOT_LENGTH.toPx(), DOT_GAP.toPx()),
                            )
                        },
                    )
                    if (one.style == SeriesStyle.OBSERVED) {
                        drawCircle(color = colors[index], radius = POINT_SIZE.toPx() / 2)
                    }
                }
                Text(text = one.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Palette colors assigned to the series in order. */
@Composable
private fun seriesColors(series: List<ChartSeries>): List<Color> {
    val palette: List<Color> = GetFitTheme.dataColors.seriesPalette
    return series.indices.map { index: Int -> palette[index % palette.size] }
}

@Preview(name = "Weight chart", fontScale = 1f)
@Preview(name = "Weight chart, 200% font", fontScale = 2f)
@Composable
private fun MetricChartPreview() {
    val start: LocalDate = LocalDate(2026, 9, 18)
    val weighIns: List<DataPoint> = (0 until 30 step 2).map { day: Int ->
        DataPoint(LocalDate.fromEpochDays(start.toEpochDays() + day), 212.0 - day * 0.08 + (day % 3) * 0.4, null)
    }
    val trend: List<DataPoint> = listOf(
        DataPoint(weighIns.first().date, 212.3, null),
        DataPoint(weighIns.last().date, 210.4, null),
    )
    GetFitTheme {
        MetricChart(
            title = "Weight",
            series = listOf(
                ChartSeries("Weight", "lb", "pounds", weighIns, SeriesStyle.OBSERVED),
                ChartSeries("Trend", "lb", "pounds", trend, SeriesStyle.TREND),
            ),
            range = start..LocalDate(2026, 10, 17),
            formatValue = { value: Double -> String.format(Locale.US, "%.1f", value) },
        )
    }
}

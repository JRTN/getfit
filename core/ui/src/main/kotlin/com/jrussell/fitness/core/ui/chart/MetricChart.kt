package com.jrussell.fitness.core.ui.chart

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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

/** The dash and gap of the dotted trend line; with the line's round caps, short dashes read as dots. */
private val DOT_LENGTH: Dp = 3.dp
private val DOT_GAP: Dp = 5.dp
private const val MIN_Y_PADDING: Double = 0.5
private const val Y_PADDING_FRACTION: Double = 0.1
private const val TARGET_DATE_LABELS: Int = 4

/** How many value labels the y-axis shows (the default step placer can leave just one on a narrow range). */
private const val VALUE_LABELS: Int = 5

/**
 * The app's one chart component (spec: "The UI never calls a charting library directly"). It
 * draws [series] over [range] with Vico. Every chart follows the same rules:
 * - each series is its recorded values connected by a solid line, with a dot on each point, in
 *   the next palette color; this is the main line;
 * - each series also gets its trend: the dotted least-squares line through its
 *   points ([trendLine]), in the same color, drawn under the main line;
 * - the spoken summary (the chart's content description) reads the first series: first and last
 *   values and the trend per week, whether or not the trend line is drawn;
 * - "View as table" lists the recorded values, with the summary above them;
 * - a legend tells lines apart by label and shape (solid vs dotted), never by color alone.
 *
 * Next to "View as table", "Hide trend line" / "Show trend line" turns the dotted lines off and on
 * for this chart (kept across rotation; the trend line is on whenever the chart is first shown).
 *
 * @param series values already in display units; the first is the one summarized.
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
    var showTrend: Boolean by rememberSaveable { mutableStateOf(true) }
    val drawable: List<ChartSeries> = series.filter { one: ChartSeries -> one.points.isNotEmpty() }
    Column(modifier = modifier.fillMaxWidth()) {
        if (drawable.isEmpty()) {
            Text(
                text = stringResource(R.string.chart_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val summary: String? = spokenSummary(title, drawable.first(), range, formatValue)
            val lines: List<DrawnLine> = drawnLines(drawable, showTrend)
            if (showTable) {
                summary?.let { text: String ->
                    Text(text = text, style = MaterialTheme.typography.bodyMedium)
                }
                ChartTable(series = drawable, formatValue = formatValue)
            } else {
                VicoChart(lines = lines, range = range, summary = summary, formatValue = formatValue)
            }
            Legend(lines = lines)
            val hasTrend: Boolean = drawable.any { one: ChartSeries -> trendLine(one.points).isNotEmpty() }
            ChartActions(
                showTable = showTable,
                onToggleTable = { showTable = !showTable },
                // The table never shows trend lines, so the toggle only appears with the chart.
                trendToggle = if (hasTrend && !showTable) TrendToggle(showTrend) { showTrend = !showTrend } else null,
            )
        }
    }
}

/** The trend-line button's state: whether the line is [shown], and what a tap does. */
private class TrendToggle(val shown: Boolean, val onToggle: () -> Unit)

/** "View as table" / "View as chart", then (when there is a trend to toggle) "Hide/Show trend line". */
@Composable
private fun ChartActions(showTable: Boolean, onToggleTable: () -> Unit, trendToggle: TrendToggle?) {
    // Wraps instead of clipping when large fonts make the two buttons wider than the screen.
    FlowRow {
        TextButton(onClick = onToggleTable) {
            Text(text = stringResource(if (showTable) R.string.chart_view_chart else R.string.chart_view_table))
        }
        trendToggle?.let { toggle: TrendToggle ->
            TextButton(onClick = toggle.onToggle) {
                Text(text = stringResource(if (toggle.shown) R.string.chart_hide_trend else R.string.chart_show_trend))
            }
        }
    }
}

/** One line on the chart: a series' recorded values (solid, with dots) or its trend (dotted). */
private data class DrawnLine(val label: String, val points: List<DataPoint>, val color: Color, val dotted: Boolean)

/**
 * The lines to draw, trends first so each main line sits on top of its trend. A series whose
 * points fall on a single date has no trend. With one series the trend is labeled "Trend"; with
 * several, "Weight trend" and so on.
 */
@Composable
private fun drawnLines(series: List<ChartSeries>, showTrend: Boolean): List<DrawnLine> {
    val palette: List<Color> = GetFitTheme.dataColors.seriesPalette
    val trendLabel: String = stringResource(R.string.chart_trend)
    val main: List<DrawnLine> = series.mapIndexed { index: Int, one: ChartSeries ->
        DrawnLine(one.label, one.points, palette[index % palette.size], dotted = false)
    }
    if (!showTrend) {
        return main
    }
    val trends: List<DrawnLine> = main.mapNotNull { line: DrawnLine ->
        val label: String = if (series.size ==
            1
        ) {
            trendLabel
        } else {
            stringResource(R.string.chart_series_trend, line.label)
        }
        trendLine(line.points).takeIf { it.isNotEmpty() }?.let { points: List<DataPoint> ->
            DrawnLine(label, points, line.color, dotted = true)
        }
    }
    return trends + main
}

@Composable
private fun VicoChart(
    lines: List<DrawnLine>,
    range: LocalDateRange,
    summary: String?,
    formatValue: (Double) -> String,
) {
    val vicoLines: List<LineCartesianLayer.Line> = rememberVicoLines(lines)
    val rangeProvider: CartesianLayerRangeProvider = rememberAxisRange(lines, range)
    val model: CartesianChartModel = remember(lines) { chartModel(lines) }
    val dateLabelFormatter: DateTimeFormatter = remember {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMd"))
    }
    val labelSpacing: Int = max(1, (range.start.daysUntil(range.endInclusive) + 1) / TARGET_DATE_LABELS)
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(vicoLines),
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

/** Vico line styles: main lines solid with a dot per point, trends dotted with no dots. */
@Composable
private fun rememberVicoLines(lines: List<DrawnLine>): List<LineCartesianLayer.Line> {
    return lines.map { line: DrawnLine ->
        val fill: LineCartesianLayer.LineFill = remember(line.color) {
            LineCartesianLayer.LineFill.single(Fill(line.color))
        }
        if (line.dotted) {
            LineCartesianLayer.rememberLine(
                fill = fill,
                stroke = LineCartesianLayer.LineStroke.Dashed(
                    thickness = LINE_THICKNESS,
                    dashLength = DOT_LENGTH,
                    gapLength = DOT_GAP,
                ),
            )
        } else {
            LineCartesianLayer.rememberLine(
                fill = fill,
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = LINE_THICKNESS),
                pointProvider = LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(
                        rememberShapeComponent(fill = Fill(line.color), shape = CircleShape),
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
private fun rememberAxisRange(lines: List<DrawnLine>, range: LocalDateRange): CartesianLayerRangeProvider {
    val values: List<Double> = lines.flatMap { line: DrawnLine -> line.points.map { point: DataPoint -> point.value } }
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

/** One Vico series per line; x is the epoch day, y the display value. */
private fun chartModel(lines: List<DrawnLine>): CartesianChartModel {
    return CartesianChartModel(
        LineCartesianLayerModel.build {
            lines.forEach { line: DrawnLine ->
                series(
                    x = line.points.map { point: DataPoint -> point.date.toEpochDays() },
                    y = line.points.map { point: DataPoint -> point.value },
                )
            }
        },
    )
}

/** One legend entry per line, main lines first: a sample of the line (solid with a dot, or dotted), then its label. */
@Composable
private fun Legend(lines: List<DrawnLine>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        lines.sortedBy { line: DrawnLine -> line.dotted }.forEach { line: DrawnLine ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Canvas(modifier = Modifier.size(LEGEND_SWATCH)) {
                    val middle: Float = size.height / 2
                    drawLine(
                        color = line.color,
                        start = Offset(0f, middle),
                        end = Offset(size.width, middle),
                        strokeWidth = LINE_THICKNESS.toPx(),
                        pathEffect = if (line.dotted) {
                            PathEffect.dashPathEffect(floatArrayOf(DOT_LENGTH.toPx(), DOT_GAP.toPx()))
                        } else {
                            null
                        },
                    )
                    if (!line.dotted) {
                        drawCircle(color = line.color, radius = POINT_SIZE.toPx() / 2)
                    }
                }
                Text(text = line.label, style = MaterialTheme.typography.labelMedium)
            }
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
    GetFitTheme {
        MetricChart(
            title = "Weight",
            series = listOf(ChartSeries("Weight", "lb", "pounds", weighIns)),
            range = start..LocalDate(2026, 10, 17),
            formatValue = { value: Double -> String.format(Locale.US, "%.1f", value) },
        )
    }
}

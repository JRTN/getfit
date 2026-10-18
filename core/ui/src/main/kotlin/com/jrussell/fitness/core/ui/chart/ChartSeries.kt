package com.jrussell.fitness.core.ui.chart

import com.jrussell.fitness.core.domain.metric.DataPoint

/** How a series is drawn. The two styles differ in shape, never only in color. */
enum class SeriesStyle {
    /** A solid line through the points (smoothed or derived data, e.g. trend weight). */
    LINE,

    /** Unconnected dots (raw observations, e.g. individual weigh-ins). */
    POINTS,
}

/**
 * One series for [MetricChart], with values already converted to the display unit.
 *
 * @property label shown in the legend and the table header, e.g. "Trend".
 * @property unitSymbol shown next to numbers, e.g. "lb".
 * @property unitSpoken read by TalkBack in the summary, e.g. "pounds".
 */
data class ChartSeries(
    val label: String,
    val unitSymbol: String,
    val unitSpoken: String,
    val points: List<DataPoint>,
    val style: SeriesStyle,
)

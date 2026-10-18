package com.jrussell.fitness.core.ui.chart

import com.jrussell.fitness.core.domain.metric.DataPoint

/** How a series is drawn. The two styles differ in shape (solid with dots vs dotted), never only in color. */
enum class SeriesStyle {
    /**
     * Recorded values (e.g. weigh-ins): a solid line through the points, with a dot on each.
     * The chart's main series; listed in the table and read in the spoken summary.
     */
    OBSERVED,

    /**
     * A fitted line drawn over the data (e.g. the best-fit weight trend): dotted, with no dots.
     * Its values were never recorded, so the table leaves it out; the summary reads its rate.
     */
    TREND,
}

/**
 * One series for [MetricChart], with values already converted to the display unit.
 *
 * @property label shown in the legend and the table header, e.g. "Weight".
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

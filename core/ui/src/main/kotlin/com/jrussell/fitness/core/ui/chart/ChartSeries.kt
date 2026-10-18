package com.jrussell.fitness.core.ui.chart

import com.jrussell.fitness.core.domain.metric.DataPoint

/**
 * One series for [MetricChart]: recorded values, already converted to the display unit. The
 * chart connects them as its main line and derives the trend line from them.
 *
 * @property label shown in the legend and the table header, e.g. "Weight".
 * @property unitSymbol shown next to numbers, e.g. "lb".
 * @property unitSpoken read by TalkBack in the summary, e.g. "pounds".
 */
data class ChartSeries(val label: String, val unitSymbol: String, val unitSpoken: String, val points: List<DataPoint>)

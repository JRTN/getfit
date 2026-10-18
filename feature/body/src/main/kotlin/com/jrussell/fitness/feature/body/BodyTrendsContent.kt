package com.jrussell.fitness.feature.body

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.ui.chart.ChartSeries
import com.jrussell.fitness.core.ui.chart.MetricChart
import com.jrussell.fitness.core.ui.chart.SeriesStyle
import com.jrussell.fitness.core.ui.format.formatOneDecimal
import com.jrussell.fitness.core.ui.format.massUnitSpoken
import com.jrussell.fitness.core.ui.format.massUnitSymbol
import com.jrussell.fitness.core.ui.range.RangeChips

private val SCREEN_PADDING: Dp = 16.dp

/**
 * The Trends view: range chips, then the body-weight chart: a solid line through each day's
 * weigh-in and the dotted best-fit trend line. Today charts the last 7 days (decision #38).
 */
@Composable
internal fun BodyTrendsContent(
    state: BodyUiState,
    onRangeSelected: (DateRangeSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val symbol: String = massUnitSymbol(state.unit)
    val spoken: String = massUnitSpoken(state.unit)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RangeChips(selection = state.selection, onSelect = onRangeSelected, today = state.today)
        MetricChart(
            title = stringResource(R.string.body_chart_title),
            series = listOf(
                ChartSeries(
                    label = stringResource(R.string.body_series_weight),
                    unitSymbol = symbol,
                    unitSpoken = spoken,
                    points = state.chartWeighIns,
                    style = SeriesStyle.OBSERVED,
                ),
                ChartSeries(
                    label = stringResource(R.string.body_series_trend),
                    unitSymbol = symbol,
                    unitSpoken = spoken,
                    points = state.chartTrend,
                    style = SeriesStyle.TREND,
                ),
            ),
            range = state.chartRange,
            formatValue = { value: Double -> formatOneDecimal(value) },
        )
    }
}

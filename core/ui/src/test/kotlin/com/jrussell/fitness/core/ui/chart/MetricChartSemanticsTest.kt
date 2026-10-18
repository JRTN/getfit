package com.jrussell.fitness.core.ui.chart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jrussell.fitness.core.designsystem.theme.GetFitTheme
import com.jrussell.fitness.core.domain.metric.DataPoint
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetricChartSemanticsTest {
    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

    private val range: LocalDateRange = LocalDate(2026, 9, 18)..LocalDate(2026, 10, 17)
    private val weighIns: ChartSeries = ChartSeries(
        label = "Weight",
        unitSymbol = "lb",
        unitSpoken = "pounds",
        points = listOf(
            DataPoint(LocalDate(2026, 9, 19), 212.4, null),
            DataPoint(LocalDate(2026, 10, 17), 209.9, null),
        ),
        style = SeriesStyle.OBSERVED,
    )
    private val trendLine: ChartSeries = weighIns.copy(
        label = "Trend",
        points = listOf(
            DataPoint(LocalDate(2026, 9, 19), 213.0, null),
            DataPoint(LocalDate(2026, 10, 17), 209.5, null),
        ),
        style = SeriesStyle.TREND,
    )

    @Test
    fun chart_exposesSpokenSummary() {
        show(listOf(weighIns))

        rule.onNodeWithContentDescription("Weight, 30 days, 212.4 to 209.9 pounds", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun viewAsTable_togglesTableRows() {
        show(listOf(weighIns))
        rule.onNodeWithText("209.9").assertDoesNotExist()

        rule.onNodeWithText("View as table").performClick()
        rule.onNodeWithText("209.9").assertIsDisplayed()

        rule.onNodeWithText("View as chart").performClick()
        rule.onNodeWithText("209.9").assertDoesNotExist()
    }

    @Test
    fun emptySeries_showsNoDataMessage() {
        show(listOf(weighIns.copy(points = emptyList())))

        rule.onNodeWithText("No data in this range").assertIsDisplayed()
    }

    @Test
    fun chartWithTrendLine_summarizesTheWeighIns() {
        show(listOf(weighIns, trendLine))

        rule.onNodeWithContentDescription("Weight, 30 days, 212.4 to 209.9 pounds", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun table_leavesOutTheTrendLineAndShowsTheRate() {
        show(listOf(weighIns, trendLine))

        rule.onNodeWithText("View as table").performClick()

        rule.onNodeWithText("Weight (lb)").assertIsDisplayed()
        rule.onNodeWithText("Trend (lb)").assertDoesNotExist()
        rule.onNodeWithText("213.0").assertDoesNotExist()
        rule.onNodeWithText("decreasing 0.6 pounds per week", substring = true).assertIsDisplayed()
    }

    private fun show(series: List<ChartSeries>) {
        rule.setContent {
            GetFitTheme {
                MetricChart(
                    title = "Weight",
                    series = series,
                    range = range,
                    formatValue = { value: Double -> String.format(Locale.US, "%.1f", value) },
                )
            }
        }
    }
}

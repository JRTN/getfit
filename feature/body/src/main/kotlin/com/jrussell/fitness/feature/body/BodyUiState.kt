package com.jrussell.fitness.feature.body

import com.jrussell.fitness.core.domain.metric.DataPoint
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.model.ids.MeasurementId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.LocalTime

/**
 * What the Body tab shows. Every weight is already in [unit]; storage stays in kilograms.
 *
 * @property today the current day, after the user's day start.
 * @property latest the most recent day's weigh-in, or `null` before the first one.
 * @property history individual weigh-ins from the last 90 days, newest first.
 * @property selection the Trends range; [chartRange] is the dates the chart spans for it.
 * @property chartWeighIns one point per day (that day's last weigh-in) inside [chartRange].
 * @property chartTrend the dotted trend line: the best-fit line through [chartWeighIns], from the
 *   first to the last weigh-in date; empty with fewer than two days of weigh-ins.
 * @property entry the add-weight dialog, or `null` while it is closed.
 */
data class BodyUiState(
    val today: LocalDate,
    val unit: MassUnit,
    val latest: LatestWeight?,
    val history: List<WeightRow>,
    val selection: DateRangeSelection,
    val chartRange: LocalDateRange,
    val chartWeighIns: List<DataPoint>,
    val chartTrend: List<DataPoint>,
    val entry: WeightEntryState?,
)

/**
 * The latest weigh-in: [value] in the display unit, on [date]. [perWeek] is the weight trend: the
 * slope of the best-fit line through the last 30 days of weigh-ins, per week (0 with fewer than
 * two days of weigh-ins).
 */
data class LatestWeight(val value: Double, val date: LocalDate, val perWeek: Double)

/** One weigh-in in the history list: [value] in the display unit, taken at [time] on [date]. */
data class WeightRow(val id: MeasurementId, val date: LocalDate, val time: LocalTime, val value: Double)

/** The open add-weight dialog; [error] is why the last save was refused, if it was. */
data class WeightEntryState(val error: WeightInputError? = null)

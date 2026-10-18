package com.jrussell.fitness.feature.home

import com.jrussell.fitness.core.domain.metric.RangeChange
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.model.MassUnit
import kotlinx.datetime.LocalDate

/**
 * What Home shows. Cards for domains with no data yet are `null` and hidden (decision #38), so
 * a new install sees only the range chips and a short empty-state line.
 *
 * @property today the current day, after the user's day start.
 */
data class HomeUiState(val today: LocalDate, val selection: DateRangeSelection, val body: BodyCard?)

/** The Body card, in the user's weight unit. */
sealed interface BodyCard {
    val unit: MassUnit

    /**
     * Range "Today": the latest weigh-in (with its date, which may be earlier) and the weight
     * trend [perWeek] over the last 7 days, the span Today's charts show (0 with too few weigh-ins).
     */
    data class Today(val latest: Double, val latestDate: LocalDate, val perWeek: Double, override val unit: MassUnit) :
        BodyCard

    /**
     * Any multi-day range: the first and last weigh-ins in it and the weight trend per week. With
     * no weigh-in inside the range, the latest one at both ends and a trend of 0.
     */
    data class Range(val change: RangeChange, override val unit: MassUnit) : BodyCard
}

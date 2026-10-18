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

    /** Range "Today": the latest weigh-in (with its date, which may be earlier) and the trend. */
    data class Today(val latest: Double, val latestDate: LocalDate, val trend: Double, override val unit: MassUnit) :
        BodyCard

    /** Any multi-day range: trend weight start → now and rate per week; `null` with too little data. */
    data class Range(val change: RangeChange?, override val unit: MassUnit) : BodyCard
}

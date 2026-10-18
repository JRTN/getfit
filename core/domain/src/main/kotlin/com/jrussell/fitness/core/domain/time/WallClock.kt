package com.jrussell.fitness.core.domain.time

import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/**
 * The current moment and the device's current time zone, together: "today" depends on both.
 * ViewModels take this instead of `Clock` and `TimeZone.currentSystemDefault()` so they depend
 * only on `:core:domain` interfaces and tests control time.
 */
interface WallClock {
    fun now(): Instant

    /** The zone the user is in right now; it changes when they travel. */
    fun timeZone(): TimeZone
}

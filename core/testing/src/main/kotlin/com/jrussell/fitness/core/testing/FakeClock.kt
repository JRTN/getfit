package com.jrussell.fitness.core.testing

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** A [Clock] that stands still until a test moves it, so timestamps are exact and repeatable. */
class FakeClock(var now: Instant) : Clock {
    override fun now(): Instant {
        return now
    }

    /** Moves the clock forward by [duration]. */
    fun advanceBy(duration: Duration) {
        now += duration
    }
}

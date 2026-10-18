package com.jrussell.fitness.core.testing

import com.jrussell.fitness.core.domain.time.WallClock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** A [WallClock] that tests set directly: [now] and [zone] change only when a test changes them. */
class FakeWallClock(var now: Instant, var zone: TimeZone = TimeZone.UTC) : WallClock {
    override fun now(): Instant {
        return now
    }

    override fun timeZone(): TimeZone {
        return zone
    }
}

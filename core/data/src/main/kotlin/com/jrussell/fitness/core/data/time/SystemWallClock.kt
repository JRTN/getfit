package com.jrussell.fitness.core.data.time

import com.jrussell.fitness.core.domain.time.WallClock
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** [WallClock] on the injected [Clock] and the device's zone, read on every call (it can change). */
class SystemWallClock @Inject constructor(private val clock: Clock) : WallClock {
    override fun now(): Instant {
        return clock.now()
    }

    override fun timeZone(): TimeZone {
        return TimeZone.currentSystemDefault()
    }
}

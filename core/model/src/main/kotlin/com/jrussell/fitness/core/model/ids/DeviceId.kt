package com.jrussell.fitness.core.model.ids

import kotlin.uuid.Uuid

/**
 * The install that made a row's last write: the tie-breaker for future last-writer-wins sync.
 * One per install, never backed up.
 */
@JvmInline
value class DeviceId(val value: Uuid)

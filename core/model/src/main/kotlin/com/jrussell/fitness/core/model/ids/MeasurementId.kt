package com.jrussell.fitness.core.model.ids

import kotlin.uuid.Uuid

/** Identifies one [com.jrussell.fitness.core.model.Measurement] row (UUIDv7). */
@JvmInline
value class MeasurementId(val value: Uuid)

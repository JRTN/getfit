package com.jrussell.fitness.core.model.ids

import kotlin.uuid.Uuid

/** Identifies a measurement type: UUIDv5 for built-ins (identical on every device), UUIDv7 for user-defined types. */
@JvmInline
value class MeasurementTypeId(val value: Uuid)

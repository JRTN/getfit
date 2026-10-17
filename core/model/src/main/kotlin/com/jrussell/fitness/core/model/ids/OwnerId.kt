package com.jrussell.fitness.core.model.ids

import kotlin.uuid.Uuid

/**
 * The profile that owns a row. Every user-owned row carries one; repositories obtain it from the
 * current-owner provider, never from callers.
 */
@JvmInline
value class OwnerId(val value: Uuid)

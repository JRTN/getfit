package com.jrussell.fitness.core.domain

import kotlin.uuid.Uuid

/** Creates primary keys for new rows. */
interface IdGenerator {
    /** A UUIDv7: time-ordered, so new rows sort after old ones and index well. */
    fun newTimeOrderedId(): Uuid

    /**
     * A UUIDv5 derived from [name] in the app's fixed namespace: the same name always gives the
     * same ID on every device (copied reference foods, built-in rows).
     */
    fun nameBasedId(name: String): Uuid
}

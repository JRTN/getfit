package com.jrussell.fitness.core.database.entity

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * The sync columns every user-owned row carries (embedded, so the columns sit on the row itself).
 *
 * @property createdAt when the row was first written.
 * @property updatedAt the last write; the last-writer-wins key for future sync.
 * @property deletedAt tombstone: non-null means deleted. DAOs filter `deletedAt IS NULL`.
 * @property localRevision the `SyncSequence` value of the transaction that last wrote the row.
 * @property originDeviceId the install that made the last write; the sync tie-breaker.
 */
data class SyncMetadata(
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val localRevision: Long,
    val originDeviceId: Uuid,
)

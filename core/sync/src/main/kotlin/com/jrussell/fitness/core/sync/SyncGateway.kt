package com.jrussell.fitness.core.sync

import kotlin.uuid.Uuid

/**
 * The sync seam: how local changes would reach a remote copy and remote changes come back.
 * v1 binds [NoOpSyncGateway]; a future Spring Boot backend implements the same contract over
 * HTTP. A new remote starts at `SyncCursor.lastPushedRevision = 0`, so its first push is every
 * row with `localRevision > 0` (tombstones included): a complete initial upload with no special
 * code. Conflicts resolve last-writer-wins per row on `(updatedAt, originDeviceId)`.
 */
interface SyncGateway {
    suspend fun push(changes: List<SyncableChange>): PushResult

    suspend fun pull(sinceRemoteCursor: String?): PullResult
}

/**
 * One row's latest state, as it would travel to or from a remote.
 *
 * @property table the `user.db` table name, e.g. `"Measurement"`.
 * @property payloadJson the row in the JSON export format (defined in M1d).
 */
data class SyncableChange(
    val table: String,
    val rowId: Uuid,
    val localRevision: Long,
    val updatedAtEpochMillis: Long,
    val originDeviceId: Uuid,
    val deleted: Boolean,
    val payloadJson: String,
)

/** What happened to a [SyncGateway.push]. */
sealed interface PushResult {
    /** The remote stored every change up to [highestRevision]. */
    data class Accepted(val highestRevision: Long) : PushResult

    /** Nothing was sent, deliberately (e.g. sync is disabled). */
    data class Skipped(val reason: String) : PushResult

    /** The push failed and should be retried later. */
    data class Failed(val reason: String) : PushResult
}

/** What a [SyncGateway.pull] brought back. */
sealed interface PullResult {
    /** Nothing new on the remote. */
    data object Empty : PullResult

    /** Remote changes to apply, and the cursor to pull from next time. */
    data class Changes(val changes: List<SyncableChange>, val nextCursor: String) : PullResult
}

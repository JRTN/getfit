package com.jrussell.fitness.core.sync

import javax.inject.Inject

/** v1's [SyncGateway]: sync is disabled, so pushes are skipped and pulls bring nothing. */
class NoOpSyncGateway @Inject constructor() : SyncGateway {
    override suspend fun push(changes: List<SyncableChange>): PushResult {
        return PushResult.Skipped(reason = "Sync disabled")
    }

    override suspend fun pull(sinceRemoteCursor: String?): PullResult {
        return PullResult.Empty
    }
}

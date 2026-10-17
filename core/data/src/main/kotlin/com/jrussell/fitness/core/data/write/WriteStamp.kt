package com.jrussell.fitness.core.data.write

import com.jrussell.fitness.core.database.entity.SyncMetadata
import com.jrussell.fitness.core.model.ids.DeviceId
import kotlin.time.Instant

/**
 * What one write transaction stamps onto every row it writes.
 *
 * @property revision the new value of the global `SyncSequence`.
 * @property now the write time, used as `updatedAt` (and `createdAt` for new rows).
 * @property deviceId this install, used as `originDeviceId`.
 */
data class WriteStamp(val revision: Long, val now: Instant, val deviceId: DeviceId) {
    /**
     * The sync columns for a live row written by this transaction. Updates pass the row's
     * original [createdAt]; new rows take the default.
     */
    fun toSyncMetadata(createdAt: Instant = now): SyncMetadata {
        return SyncMetadata(
            createdAt = createdAt,
            updatedAt = now,
            deletedAt = null,
            localRevision = revision,
            originDeviceId = deviceId.value,
        )
    }
}

package com.jrussell.fitness.core.data.write

import androidx.room.withTransaction
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.model.ids.DeviceId
import javax.inject.Inject
import kotlin.time.Clock

/**
 * The only way repositories write to `user.db`. Each call is one transaction that increments the
 * global revision counter and hands the block a [WriteStamp] (revision, time, device) to put on
 * every row it writes. If the block throws, its rows and the revision increment roll back together.
 */
class StampedWriter @Inject constructor(
    private val database: GetFitDatabase,
    private val clock: Clock,
    private val deviceIdProvider: DeviceIdProvider,
) {
    suspend fun <T> write(block: suspend (WriteStamp) -> T): T {
        // Resolved before the transaction: the first call reads a file, which must not hold the
        // database lock.
        val deviceId: DeviceId = deviceIdProvider.deviceId()
        return database.withTransaction {
            val revision: Long = database.syncSequenceDao().incrementAndGet()
            block(WriteStamp(revision = revision, now = clock.now(), deviceId = deviceId))
        }
    }
}

package com.jrussell.fitness.core.database.infrastructure

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/** The global revision counter. Owner-less by design: one counter per database. */
@Dao
abstract class SyncSequenceDao {
    /**
     * Increments the counter and returns the new value (1 on first use). Call it inside the write
     * transaction it stamps, so a rolled-back write also rolls back its revision.
     */
    @Transaction
    open suspend fun incrementAndGet(): Long {
        ensureRow()
        increment()
        return current()
    }

    @Query("INSERT OR IGNORE INTO SyncSequence (id, value) VALUES (1, 0)")
    protected abstract suspend fun ensureRow()

    @Query("UPDATE SyncSequence SET value = value + 1 WHERE id = 1")
    protected abstract suspend fun increment()

    @Query("SELECT value FROM SyncSequence WHERE id = 1")
    protected abstract suspend fun current(): Long
}

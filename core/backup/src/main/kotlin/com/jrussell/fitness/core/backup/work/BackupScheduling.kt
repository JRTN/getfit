package com.jrussell.fitness.core.backup.work

import kotlin.time.Instant

/** One daily-backup run: writes a snapshot into the chosen folder; `false` if it failed. */
fun interface SnapshotJob {
    suspend fun run(): Boolean
}

/**
 * Keeps the daily snapshot scheduled. Implemented with WorkManager in this module; `:core:data`
 * calls it when the folder changes and the app calls it at startup.
 */
interface BackupScheduler {
    /** Schedules the daily snapshot if it isn't already (keeps an existing schedule). */
    fun ensureScheduled()

    /** Schedules the daily snapshot afresh, e.g. after a new folder is chosen. */
    fun reschedule()

    /**
     * Runs one snapshot soon when the last success is older than 36 hours (or there has never
     * been one), because a phone's battery management can hold back periodic work for days.
     */
    fun catchUpIfStale(lastSuccess: Instant?, now: Instant)
}

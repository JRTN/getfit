package com.jrussell.fitness.core.backup.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** Unique name of the daily snapshot work. */
const val DAILY_SNAPSHOT_WORK: String = "getfit-daily-snapshot"

/** Unique name of the one-off catch-up snapshot. */
const val CATCH_UP_SNAPSHOT_WORK: String = "getfit-catch-up-snapshot"

/**
 * [BackupScheduler] on WorkManager (spec: "Daily, while charging"): one unique periodic request
 * that needs the charger and enough free storage, plus a unique one-off catch-up when the daily
 * one has fallen behind.
 */
class WorkManagerBackupScheduler @Inject constructor(@param:ApplicationContext private val context: Context) :
    BackupScheduler {
    override fun ensureScheduled() {
        enqueueDaily(ExistingPeriodicWorkPolicy.KEEP)
    }

    override fun reschedule() {
        enqueueDaily(ExistingPeriodicWorkPolicy.UPDATE)
    }

    override fun catchUpIfStale(lastSuccess: Instant?, now: Instant) {
        if (lastSuccess == null || now - lastSuccess > STALE_AFTER) {
            val request = OneTimeWorkRequestBuilder<ScheduledExportWorker>()
                .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(CATCH_UP_SNAPSHOT_WORK, ExistingWorkPolicy.KEEP, request)
        }
    }

    private fun enqueueDaily(policy: ExistingPeriodicWorkPolicy) {
        val request: PeriodicWorkRequest = PeriodicWorkRequestBuilder<ScheduledExportWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresCharging(true).setRequiresStorageNotLow(true).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(DAILY_SNAPSHOT_WORK, policy, request)
    }

    private companion object {
        val STALE_AFTER = 36.hours
    }
}

package com.jrussell.fitness.core.backup.work

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WorkManagerBackupSchedulerTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val now: Instant = Instant.parse("2026-10-18T12:00:00Z")

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun ensureScheduled_enqueuesDailyChargingWork() {
        WorkManagerBackupScheduler(context).ensureScheduled()

        val info: WorkInfo = work(DAILY_SNAPSHOT_WORK).single()
        assertThat(info.periodicityInfo?.repeatIntervalMillis).isEqualTo(1.days.inWholeMilliseconds)
        assertThat(info.constraints.requiresCharging()).isTrue()
        assertThat(info.constraints.requiresStorageNotLow()).isTrue()
    }

    @Test
    fun ensureScheduled_twice_keepsOneWork() {
        val scheduler: WorkManagerBackupScheduler = WorkManagerBackupScheduler(context)

        scheduler.ensureScheduled()
        scheduler.ensureScheduled()
        scheduler.reschedule()

        assertThat(work(DAILY_SNAPSHOT_WORK).filter { !it.state.isFinished }).hasSize(1)
    }

    @Test
    fun catchUp_lastSuccess40HoursAgo_enqueuesOneTimeWork() {
        WorkManagerBackupScheduler(context).catchUpIfStale(now - 40.hours, now)

        assertThat(work(CATCH_UP_SNAPSHOT_WORK)).hasSize(1)
    }

    @Test
    fun catchUp_neverSucceeded_enqueuesOneTimeWork() {
        WorkManagerBackupScheduler(context).catchUpIfStale(null, now)

        assertThat(work(CATCH_UP_SNAPSHOT_WORK)).hasSize(1)
    }

    @Test
    fun catchUp_lastSuccess10HoursAgo_enqueuesNothing() {
        WorkManagerBackupScheduler(context).catchUpIfStale(now - 10.hours, now)

        assertThat(work(CATCH_UP_SNAPSHOT_WORK)).isEmpty()
    }

    @Test
    fun catchUp_twice_keepsOneWork() {
        val scheduler: WorkManagerBackupScheduler = WorkManagerBackupScheduler(context)

        scheduler.catchUpIfStale(null, now)
        scheduler.catchUpIfStale(null, now)

        assertThat(work(CATCH_UP_SNAPSHOT_WORK)).hasSize(1)
    }

    private fun work(name: String): List<WorkInfo> {
        return WorkManager.getInstance(context).getWorkInfosForUniqueWork(name).get()
    }
}

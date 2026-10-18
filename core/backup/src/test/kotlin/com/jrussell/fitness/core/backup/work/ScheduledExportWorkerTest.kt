package com.jrussell.fitness.core.backup.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.jrussell.fitness.core.testing.RecordingLogger
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ScheduledExportWorkerTest {
    private val logger: RecordingLogger = RecordingLogger()

    @Test
    fun worker_jobSucceeds_returnsSuccess() = runTest {
        assertThat(workerRunning { true }.doWork()).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun worker_jobFails_returnsFailureWithoutThrowing() = runTest {
        assertThat(workerRunning { false }.doWork()).isEqualTo(ListenableWorker.Result.failure())
        assertThat(logger.entries).containsExactly("WARN Backup: daily snapshot failed; see Settings › Backup")
    }

    @Test
    fun worker_jobThrows_returnsFailure_andLogsError() = runTest {
        assertThat(workerRunning { error("disk full") }.doWork()).isEqualTo(ListenableWorker.Result.failure())
        assertThat(logger.entries).containsExactly("ERROR Backup: daily snapshot crashed: disk full")
    }

    private fun workerRunning(job: SnapshotJob): ScheduledExportWorker {
        val context: Context = RuntimeEnvironment.getApplication()
        return TestListenableWorkerBuilder<ScheduledExportWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker {
                        return ScheduledExportWorker(appContext, workerParameters, job, logger)
                    }
                },
            )
            .build()
    }
}

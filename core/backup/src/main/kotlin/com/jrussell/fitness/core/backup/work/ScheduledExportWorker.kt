package com.jrussell.fitness.core.backup.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jrussell.fitness.core.model.logging.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs one daily (or catch-up) backup through [job]. The job records its own outcome for Settings;
 * a failed run returns [Result.failure] rather than retrying, so a missing folder never turns into
 * a tight retry loop: the periodic schedule simply tries again the next day.
 */
@HiltWorker
class ScheduledExportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val job: SnapshotJob,
    private val logger: Logger,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            if (job.run()) {
                Result.success()
            } else {
                logger.warn(TAG, "daily snapshot failed; see Settings › Backup")
                Result.failure()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (
            // Whatever went wrong, the worker reports a failure instead of crashing the process.
            @Suppress("TooGenericExceptionCaught") crashed: Exception,
        ) {
            logger.error(TAG, "daily snapshot crashed", crashed)
            Result.failure()
        }
    }

    private companion object {
        const val TAG: String = "Backup"
    }
}

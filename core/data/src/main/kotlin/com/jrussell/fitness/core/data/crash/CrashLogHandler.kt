package com.jrussell.fitness.core.data.crash

import java.io.File
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Process-wide handler for uncaught exceptions: writes the crash to a text file in [directory],
 * then hands the crash to [previous] so Android still shows its crash dialog and ends the process.
 *
 * Each crash becomes `crash-<epoch millis, zero-padded to 13 digits>.txt`, so file names sort
 * chronologically, and only the newest [MAX_FILES] are kept. The files are app-private; a crash
 * log viewer and export in Settings read them (M1).
 *
 * Installed once in `GetFitApplication.onCreate()` via `Thread.setDefaultUncaughtExceptionHandler`.
 *
 * @param directory where crash files go; created on first crash.
 * @param clock source of the crash time.
 * @param previous the handler that was installed before this one (normally Android's), or `null`.
 */
class CrashLogHandler(
    private val directory: File,
    private val clock: Clock,
    private val previous: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            writeCrashFile(thread, throwable)
            deleteOldestBeyondLimit()
        } catch (
            // The process is already dying: a failure while logging (full disk, unwritable
            // directory) must never replace or hide the original crash, so it is swallowed.
            @Suppress("TooGenericExceptionCaught", "SwallowedException") loggingFailure: Throwable,
        ) {
            // Intentionally empty; see above.
        } finally {
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrashFile(thread: Thread, throwable: Throwable) {
        val now: Instant = clock.now()
        directory.mkdirs()
        val file: File = File(
            directory,
            FILE_PREFIX + now.toEpochMilliseconds().toString().padStart(EPOCH_DIGITS, '0') + FILE_SUFFIX,
        )
        file.writeText("thread: ${thread.name}\ntime: $now\n\n${throwable.stackTraceToString()}")
    }

    private fun deleteOldestBeyondLimit() {
        val crashFiles: List<File> = directory.listFiles().orEmpty()
            .filter { file: File -> file.name.startsWith(FILE_PREFIX) && file.name.endsWith(FILE_SUFFIX) }
            .sortedByDescending { file: File -> file.name }
        crashFiles.drop(MAX_FILES).forEach { file: File -> file.delete() }
    }

    companion object {
        /** How many crash files are kept; older ones are deleted on the next crash. */
        const val MAX_FILES: Int = 10

        /** Every crash file name starts with this. */
        const val FILE_PREFIX: String = "crash-"

        private const val FILE_SUFFIX: String = ".txt"

        /** Epoch milliseconds have 13 digits until the year 2286. */
        private const val EPOCH_DIGITS: Int = 13
    }
}

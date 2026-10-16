package com.jrussell.fitness

import android.app.Application
import com.jrussell.fitness.core.data.crash.CrashLogHandler
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import kotlin.time.Clock

/** Process-wide entry point; `@HiltAndroidApp` creates the application-level Hilt component. */
@HiltAndroidApp
class GetFitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashLogHandler()
    }

    /** Records every uncaught exception to `files/crash-logs/` before the process dies. */
    private fun installCrashLogHandler() {
        val handler: CrashLogHandler = CrashLogHandler(
            directory = File(filesDir, CRASH_LOG_DIRECTORY),
            clock = Clock.System,
            previous = Thread.getDefaultUncaughtExceptionHandler(),
        )
        Thread.setDefaultUncaughtExceptionHandler(handler)
    }

    private companion object {
        const val CRASH_LOG_DIRECTORY: String = "crash-logs"
    }
}

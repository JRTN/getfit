package com.jrussell.fitness

import android.app.Application
import com.jrussell.fitness.core.data.crash.CrashLogHandler
import com.jrussell.fitness.core.data.di.ApplicationScope
import com.jrussell.fitness.core.data.seed.DataBootstrap
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Process-wide entry point; `@HiltAndroidApp` creates the application-level Hilt component. */
@HiltAndroidApp
class GetFitApplication : Application() {
    @Inject
    lateinit var dataBootstrap: DataBootstrap

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        installCrashLogHandler()
        // Seeds built-in rows and creates the local profile on first launch; idempotent afterwards.
        applicationScope.launch { dataBootstrap.run() }
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

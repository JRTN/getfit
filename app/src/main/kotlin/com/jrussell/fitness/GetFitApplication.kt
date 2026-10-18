package com.jrussell.fitness

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jrussell.fitness.core.data.backup.BackupStartup
import com.jrussell.fitness.core.data.crash.CrashLogHandler
import com.jrussell.fitness.core.data.di.ApplicationScope
import com.jrussell.fitness.core.data.seed.DataBootstrap
import com.jrussell.fitness.core.model.logging.Logger
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Process-wide entry point; `@HiltAndroidApp` creates the application-level Hilt component. It
 * also gives WorkManager the Hilt worker factory (the default initializer is removed in the
 * manifest), so the daily backup worker gets its dependencies injected.
 */
@HiltAndroidApp
class GetFitApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var dataBootstrap: DataBootstrap

    @Inject
    lateinit var backupStartup: BackupStartup

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() {
            return Configuration.Builder().setWorkerFactory(workerFactory).build()
        }

    override fun onCreate() {
        super.onCreate()
        installCrashLogHandler()
        applicationScope.launch {
            // Seeds built-in rows and creates the local profile on first launch; idempotent afterwards.
            runGuarded(logger, task = "Data bootstrap") { dataBootstrap.run() }
            // Keeps the daily backup scheduled, with a catch-up if it has fallen behind.
            runGuarded(logger, task = "Backup scheduling") { backupStartup.run() }
        }
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

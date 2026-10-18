package com.jrussell.fitness.restart

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Process
import kotlin.system.exitProcess

/**
 * A blank activity in its own `:restart` process (the ProcessPhoenix pattern). Started by
 * [ProcessAppRestarter], it kills the main process, starts the launcher activity in a new task
 * (which starts a fresh main process), and then ends its own process. Doing this from a second
 * process avoids racing `startActivity` against the main process's own exit.
 */
class RestartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainProcessId: Int = intent.getIntExtra(EXTRA_MAIN_PROCESS_ID, NO_PROCESS)
        if (mainProcessId != NO_PROCESS) {
            Process.killProcess(mainProcessId)
        }
        packageManager.getLaunchIntentForPackage(packageName)?.let { launch: Intent ->
            startActivity(Intent.makeRestartActivityTask(launch.component))
        }
        finish()
        exitProcess(0)
    }

    companion object {
        /** The process to replace, from `Process.myPid()` in the main process. */
        const val EXTRA_MAIN_PROCESS_ID: String = "com.jrussell.fitness.restart.MAIN_PROCESS_ID"

        private const val NO_PROCESS: Int = -1
    }
}

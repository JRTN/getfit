package com.jrussell.fitness.restart

import android.content.Context
import android.content.Intent
import android.os.Process
import com.jrussell.fitness.core.domain.backup.AppRestarter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Restarts the app after a restore replaced `user.db`: it hands over to [RestartActivity], which
 * runs in its own process, kills this one and launches the app again. A fresh process is what
 * installs the staged database (before Room opens it) and drops every cached value.
 */
class ProcessAppRestarter @Inject constructor(@param:ApplicationContext private val context: Context) : AppRestarter {
    override fun restart() {
        val intent: Intent = Intent(context, RestartActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(RestartActivity.EXTRA_MAIN_PROCESS_ID, Process.myPid())
        context.startActivity(intent)
    }
}

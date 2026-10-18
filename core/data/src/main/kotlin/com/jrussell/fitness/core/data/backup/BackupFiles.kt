package com.jrussell.fitness.core.data.backup

import android.content.Context
import com.jrussell.fitness.core.backup.snapshot.PendingRestore
import com.jrussell.fitness.core.database.USER_DATABASE_NAME
import java.io.File

/**
 * Where backup work keeps its files: [cacheDirectory] for snapshots and chosen documents on their
 * way in or out, and the [pendingRestore] a full restore stages for the next start.
 */
data class BackupFiles(val cacheDirectory: File, val pendingRestore: PendingRestore)

/** The app's restore staging area: `files/restore/`, replacing `databases/user.db`. */
fun pendingRestoreFor(context: Context): PendingRestore {
    return PendingRestore(File(context.filesDir, "restore"), context.getDatabasePath(USER_DATABASE_NAME))
}

/**
 * Installs a restore staged before the last restart. `:app` calls this before Room opens
 * `user.db`; returns whether a restore was installed.
 */
fun installPendingRestore(context: Context): Boolean {
    return pendingRestoreFor(context).installIfPending()
}

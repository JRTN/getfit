package com.jrussell.fitness.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import java.io.File
import java.io.IOException
import kotlin.time.Clock

/**
 * Keeps a copy of a corrupt `user.db` before SQLite's default handling deletes it.
 *
 * When SQLite reports corruption, the default callback deletes the database file and the app
 * starts empty. For the only copy of someone's data that is the worst outcome, so this first
 * copies the file to `corruptDirectory/user-<epochMillis>.db`, where it can be inspected or
 * recovered, and then lets [delegate] handle the corruption as usual. A failed copy never stops
 * the default handling.
 */
class CorruptionPreservingCallback(
    private val delegate: SupportSQLiteOpenHelper.Callback,
    private val corruptDirectory: File,
    private val clock: Clock,
) : SupportSQLiteOpenHelper.Callback(delegate.version) {
    override fun onConfigure(db: SupportSQLiteDatabase) {
        delegate.onConfigure(db)
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        delegate.onCreate(db)
    }

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        delegate.onUpgrade(db, oldVersion, newVersion)
    }

    override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        delegate.onDowngrade(db, oldVersion, newVersion)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        delegate.onOpen(db)
    }

    override fun onCorruption(db: SupportSQLiteDatabase) {
        preserve(db.path)
        delegate.onCorruption(db)
    }

    private fun preserve(path: String?) {
        val source: File = path?.let(::File)?.takeIf { file: File -> file.exists() } ?: return
        try {
            corruptDirectory.mkdirs()
            source.copyTo(File(corruptDirectory, "user-${clock.now().toEpochMilliseconds()}.db"), overwrite = true)
        } catch (
            // Keeping the copy is best effort; the default handling must still run.
            @Suppress("SwallowedException") ignored: IOException,
        ) {
            // Nothing else to do.
        }
    }
}

/** Builds the framework helper with [CorruptionPreservingCallback] wrapped around Room's callback. */
internal class CorruptionPreservingOpenHelperFactory(
    private val corruptDirectory: File,
    private val clock: Clock,
    private val delegate: SupportSQLiteOpenHelper.Factory,
) : SupportSQLiteOpenHelper.Factory {
    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val wrapped: SupportSQLiteOpenHelper.Configuration = SupportSQLiteOpenHelper.Configuration.builder(
            configuration.context,
        )
            .name(configuration.name)
            .callback(CorruptionPreservingCallback(configuration.callback, corruptDirectory, clock))
            .noBackupDirectory(configuration.useNoBackupDirectory)
            .allowDataLossOnRecovery(configuration.allowDataLossOnRecovery)
            .build()
        return delegate.create(wrapped)
    }
}

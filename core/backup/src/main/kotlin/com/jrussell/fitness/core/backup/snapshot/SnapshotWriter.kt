package com.jrussell.fitness.core.backup.snapshot

import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

/**
 * Writes a consistent copy of [database] to [target] with SQLite's `VACUUM INTO`: one
 * self-contained file (no WAL), taken in a single read transaction while the app keeps running.
 * Any existing [target] is replaced, because `VACUUM INTO` refuses to write over a file.
 */
fun writeSnapshot(database: SupportSQLiteDatabase, target: File) {
    target.parentFile?.mkdirs()
    target.delete()
    database.execSQL("VACUUM INTO ?", arrayOf<Any?>(target.path))
}

package com.jrussell.fitness.core.backup.snapshot

import android.database.Cursor
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.jrussell.fitness.core.model.measurementUnitOf
import java.io.File
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** What a database file chosen for restore turned out to be. */
sealed interface SnapshotCheck {
    /** A GetFit database that can replace `user.db`: [measurementCount] live readings, last changed [newestChange]. */
    data class Valid(val measurementCount: Int, val newestChange: Instant?) : SnapshotCheck

    /** Not a SQLite file at all. */
    data object NotSqlite : SnapshotCheck

    /** A SQLite file that fails its integrity check or can't be opened. */
    data object Damaged : SnapshotCheck

    /** A SQLite file, but not one any version of GetFit wrote. */
    data object NotGetFit : SnapshotCheck

    /** A GetFit database from a newer version, schema [found]; this version can't read it. */
    data class NewerVersion(val found: Int) : SnapshotCheck

    /** A GetFit database with rows this version can't use (bad day start, time zone or unit). */
    data object InvalidRows : SnapshotCheck
}

private val SQLITE_HEADER: ByteArray = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
private const val MAX_DAY_START_MINUTES: Int = 1439

/** Whether [head] (a file's first bytes) starts with SQLite's 16-byte file header. */
fun isSqliteFile(head: ByteArray): Boolean {
    return head.size >= SQLITE_HEADER.size &&
        SQLITE_HEADER.indices.all { index: Int -> head[index] == SQLITE_HEADER[index] }
}

/**
 * Checks [file] before it may replace `user.db`, without changing it.
 *
 * In order:
 * - the SQLite header;
 * - `PRAGMA quick_check`;
 * - the schema version: above [currentVersion] is a newer GetFit, anything else must be a version
 *   in [identityHashes] whose Room identity hash matches (an older version is migrated by Room
 *   after the restore);
 * - the rows Room's converters and the app's rules depend on: day starts in 0..1439, known time
 *   zones and unit codes.
 */
fun inspectSnapshot(file: File, currentVersion: Int, identityHashes: Map<Int, String>): SnapshotCheck {
    val head: ByteArray = if (file.isFile) {
        file.inputStream().use { stream ->
            stream.readNBytes(SQLITE_HEADER.size)
        }
    } else {
        ByteArray(0)
    }
    if (!isSqliteFile(head)) {
        return SnapshotCheck.NotSqlite
    }
    return try {
        // A no-op error handler: the default one deletes a file it finds corrupt.
        val db: SQLiteDatabase =
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY, DatabaseErrorHandler { })
        try {
            check(db, currentVersion, identityHashes)
        } finally {
            db.close()
        }
    } catch (
        @Suppress("SwallowedException") unreadable: SQLiteException,
    ) {
        SnapshotCheck.Damaged
    }
}

private fun check(db: SQLiteDatabase, currentVersion: Int, identityHashes: Map<Int, String>): SnapshotCheck {
    val version: Int = db.version
    val expectedHash: String? = identityHashes[version]
    return when {
        db.firstString("PRAGMA quick_check") != "ok" -> SnapshotCheck.Damaged
        version > currentVersion -> SnapshotCheck.NewerVersion(version)
        expectedHash == null || !db.hasTable("room_master_table") -> SnapshotCheck.NotGetFit
        db.firstString("SELECT identity_hash FROM room_master_table LIMIT 1") != expectedHash -> SnapshotCheck.NotGetFit
        !rowsAreUsable(db) -> SnapshotCheck.InvalidRows
        else -> SnapshotCheck.Valid(
            measurementCount = db.firstLong("SELECT COUNT(*) FROM Measurement WHERE deletedAt IS NULL")?.toInt() ?: 0,
            newestChange = db.firstLong("SELECT MAX(updatedAt) FROM Measurement")?.let(Instant::fromEpochMilliseconds),
        )
    }
}

private fun rowsAreUsable(db: SQLiteDatabase): Boolean {
    val dayStartsOk: Boolean = db.firstLong(
        "SELECT COUNT(*) FROM DayBoundarySetting WHERE dayStartMinutes NOT BETWEEN 0 AND $MAX_DAY_START_MINUTES",
    ) == 0L
    val zonesOk: Boolean = db.strings("SELECT DISTINCT timeZone FROM Measurement").all { zone: String ->
        runCatching { TimeZone.of(zone) }.isSuccess
    }
    val unitsOk: Boolean = db.strings("SELECT DISTINCT canonicalUnit FROM MeasurementType").all { code: String ->
        measurementUnitOf(code) != null
    }
    return dayStartsOk && zonesOk && unitsOk
}

private fun SQLiteDatabase.hasTable(name: String): Boolean {
    return rawQuery(
        "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
        arrayOf(name),
    ).use { cursor: Cursor ->
        cursor.moveToFirst()
    }
}

private fun SQLiteDatabase.firstString(sql: String): String? {
    return rawQuery(sql, null).use { cursor: Cursor ->
        if (cursor.moveToFirst() &&
            !cursor.isNull(0)
        ) {
            cursor.getString(0)
        } else {
            null
        }
    }
}

private fun SQLiteDatabase.firstLong(sql: String): Long? {
    return rawQuery(sql, null).use { cursor: Cursor ->
        if (cursor.moveToFirst() &&
            !cursor.isNull(0)
        ) {
            cursor.getLong(0)
        } else {
            null
        }
    }
}

private fun SQLiteDatabase.strings(sql: String): List<String> {
    return rawQuery(sql, null).use { cursor: Cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(cursor.getString(0))
            }
        }
    }
}

package com.jrussell.fitness.core.backup.snapshot

import android.database.sqlite.SQLiteDatabase
import java.io.File

internal const val TEST_HASH_V1: String = "0cf6a91b7d0c77f034d4018880010478"
internal const val TEST_HASH_V2: String = "aaaabbbbccccddddeeeeffff00001111"

/**
 * Writes a SQLite file shaped like `user.db` as far as the snapshot checks read it: the Room
 * identity table, `user_version`, and the columns the row checks look at.
 */
internal fun aGetFitFile(
    file: File,
    userVersion: Int = 1,
    identityHash: String? = TEST_HASH_V1,
    dayStartMinutes: Int = 240,
    timeZone: String = "America/Toronto",
    canonicalUnit: String = "kg",
    source: String = "MANUAL",
    enteredUnit: String = "lb",
) {
    file.parentFile?.mkdirs()
    val db: SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(file, null)
    try {
        if (identityHash != null) {
            db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, '$identityHash')")
        }
        db.execSQL("CREATE TABLE UserProfile (id TEXT PRIMARY KEY, sex TEXT)")
        db.execSQL("INSERT INTO UserProfile VALUES ('p1', 'MALE')")
        db.execSQL("CREATE TABLE DayBoundarySetting (id TEXT PRIMARY KEY, dayStartMinutes INTEGER NOT NULL)")
        db.execSQL("INSERT INTO DayBoundarySetting VALUES ('d1', $dayStartMinutes)")
        db.execSQL(
            "CREATE TABLE MeasurementType (id TEXT PRIMARY KEY, canonicalUnit TEXT NOT NULL, " +
                "category TEXT NOT NULL, dailyAggregation TEXT NOT NULL)",
        )
        db.execSQL("INSERT INTO MeasurementType VALUES ('t1', '$canonicalUnit', 'BODY', 'LAST')")
        db.execSQL(
            "CREATE TABLE Measurement (id TEXT PRIMARY KEY, timeZone TEXT NOT NULL, source TEXT NOT NULL, " +
                "enteredUnit TEXT, updatedAt INTEGER NOT NULL, deletedAt INTEGER)",
        )
        db.execSQL("INSERT INTO Measurement VALUES ('m1', '$timeZone', '$source', '$enteredUnit', 1000, NULL)")
        db.execSQL("INSERT INTO Measurement VALUES ('m2', 'UTC', 'MANUAL', NULL, 3000, NULL)")
        db.execSQL("INSERT INTO Measurement VALUES ('m3', 'UTC', 'MANUAL', NULL, 5000, 5000)")
        db.version = userVersion
    } finally {
        db.close()
    }
}

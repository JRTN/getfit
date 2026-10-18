package com.jrussell.fitness.core.backup.snapshot

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SnapshotWriterTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun writeSnapshot_copiesRowsIntoSingleFile() {
        val target: File = File(temporaryFolder.root, "out/snapshot.db")

        withOpenSource { db: SupportSQLiteDatabase -> writeSnapshot(db, target) }

        assertThat(rowCount(target)).isEqualTo(2)
        assertThat(File(target.path + "-wal").exists()).isEqualTo(false)
    }

    @Test
    fun writeSnapshot_overwritesExistingTarget() {
        val target: File = File(temporaryFolder.root, "snapshot.db").apply { writeText("old") }

        withOpenSource { db: SupportSQLiteDatabase -> writeSnapshot(db, target) }

        assertThat(rowCount(target)).isEqualTo(2)
    }

    /** Opens a small WAL-mode database through SupportSQLite, as Room does, and runs [block] on it. */
    private fun withOpenSource(block: (SupportSQLiteDatabase) -> Unit) {
        val path: String = File(temporaryFolder.root, "source.db").path
        val callback: SupportSQLiteOpenHelper.Callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE Measurement (id TEXT PRIMARY KEY, value REAL NOT NULL)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                // Never upgraded.
            }
        }
        val helper: SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(RuntimeEnvironment.getApplication())
                .name(path)
                .callback(callback)
                .build(),
        )
        try {
            val db: SupportSQLiteDatabase = helper.writableDatabase
            db.enableWriteAheadLogging()
            db.execSQL("INSERT INTO Measurement VALUES ('a', 82.4)")
            db.execSQL("INSERT INTO Measurement VALUES ('b', 82.1)")
            block(db)
        } finally {
            helper.close()
        }
    }

    private fun rowCount(file: File): Int {
        val db: SQLiteDatabase = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        return try {
            db.rawQuery("SELECT COUNT(*) FROM Measurement", null).use { cursor: Cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
        } finally {
            db.close()
        }
    }
}

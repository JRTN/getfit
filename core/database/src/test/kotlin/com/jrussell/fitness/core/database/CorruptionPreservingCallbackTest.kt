package com.jrussell.fitness.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import java.io.File
import java.lang.reflect.Proxy
import kotlin.time.Clock
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CorruptionPreservingCallbackTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val now: Instant = Instant.fromEpochMilliseconds(1_760_000_000_123)
    private val clock: Clock = object : Clock {
        override fun now(): Instant {
            return now
        }
    }

    @Test
    fun onCorruption_copiesFileBeforeDelegate() {
        val databaseFile: File = temporaryFolder.newFile("user.db").apply { writeText("corrupt bytes") }
        val corruptDirectory: File = File(temporaryFolder.root, "corrupt")
        val copy: File = File(corruptDirectory, "user-1760000000123.db")
        var copyExistedWhenDelegateRan: Boolean = false
        val delegate: SupportSQLiteOpenHelper.Callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                // Not exercised.
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                // Not exercised.
            }

            override fun onCorruption(db: SupportSQLiteDatabase) {
                copyExistedWhenDelegateRan = copy.exists()
            }
        }

        CorruptionPreservingCallback(delegate, corruptDirectory, clock).onCorruption(databaseAt(databaseFile))

        assertThat(copyExistedWhenDelegateRan).isTrue()
        assertThat(copy.readText()).isEqualTo("corrupt bytes")
    }

    @Test
    fun onCorruption_alsoKeepsWalAndShm() {
        // In WAL mode the newest commits live only in -wal, which the default handling deletes too.
        val databaseFile: File = temporaryFolder.newFile("user.db").apply { writeText("main") }
        File(databaseFile.path + "-wal").writeText("recent commits")
        File(databaseFile.path + "-shm").writeText("index")
        val corruptDirectory: File = File(temporaryFolder.root, "corrupt")
        val delegate: SupportSQLiteOpenHelper.Callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                // Not exercised.
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                // Not exercised.
            }

            override fun onCorruption(db: SupportSQLiteDatabase) {
                // The default would delete the files here.
            }
        }

        CorruptionPreservingCallback(delegate, corruptDirectory, clock).onCorruption(databaseAt(databaseFile))

        assertThat(File(corruptDirectory, "user-1760000000123.db-wal").readText()).isEqualTo("recent commits")
        assertThat(File(corruptDirectory, "user-1760000000123.db-shm").readText()).isEqualTo("index")
    }

    /** A stand-in database whose only answered call is its file path. */
    private fun databaseAt(file: File): SupportSQLiteDatabase {
        return Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getPath" -> file.path
                "isOpen" -> false
                else -> null
            }
        } as SupportSQLiteDatabase
    }
}

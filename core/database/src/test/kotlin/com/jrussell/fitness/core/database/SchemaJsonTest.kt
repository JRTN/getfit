package com.jrussell.fitness.core.database

import android.database.Cursor
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import java.io.File
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The committed schema JSON for the current version must describe the database Room compiled.
 * (From `v0.1.0` on, M1d also pins the hash so a changed schema forces a version bump.)
 */
@RunWith(RobolectricTestRunner::class)
class SchemaJsonTest {
    private val database: GetFitDatabase = inMemoryGetFitDatabase()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun committedSchema_matchesCompiledDatabase() {
        val json: String = File("schemas/com.jrussell.fitness.core.database.GetFitDatabase/1.json").readText()
        val committedHash: String? = IDENTITY_HASH.find(json)?.groupValues?.get(1)

        val cursor: Cursor = database.openHelper.readableDatabase.query("SELECT identity_hash FROM room_master_table")
        val compiledHash: String = cursor.use { rows: Cursor ->
            rows.moveToFirst()
            rows.getString(0)
        }

        assertThat(committedHash).isNotNull().isEqualTo(compiledHash)
    }

    private companion object {
        val IDENTITY_HASH: Regex = Regex(""""identityHash"\s*:\s*"([0-9a-f]+)"""")
    }
}

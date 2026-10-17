package com.jrussell.fitness.core.data

import android.database.Cursor
import androidx.room.Room
import com.jrussell.fitness.core.data.write.DeviceIdProvider
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.model.ids.DeviceId
import kotlin.time.Instant
import kotlin.uuid.Uuid
import org.robolectric.RuntimeEnvironment

val TEST_NOW: Instant = Instant.parse("2026-10-17T12:00:00Z")
val TEST_DEVICE: DeviceId = DeviceId(Uuid.parse("5b1d7e9a-0c44-4f7a-9c2e-3f6b2c1e8a4d"))

/** A fresh in-memory [GetFitDatabase] for one test; close it in `@After`. */
fun inMemoryGetFitDatabase(): GetFitDatabase {
    return Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), GetFitDatabase::class.java)
        .allowMainThreadQueries()
        .build()
}

/** Runs a `SELECT COUNT(*) ...` query and returns the count. */
fun GetFitDatabase.count(sql: String): Int {
    return openHelper.readableDatabase.query(sql).use { cursor: Cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }
}

/** A [DeviceIdProvider] that always returns [id]. */
class FixedDeviceIdProvider(private val id: DeviceId = TEST_DEVICE) : DeviceIdProvider {
    override suspend fun deviceId(): DeviceId {
        return id
    }
}

package com.jrussell.fitness.core.data.write

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.jrussell.fitness.core.data.FixedDeviceIdProvider
import com.jrussell.fitness.core.data.TEST_DEVICE
import com.jrussell.fitness.core.data.TEST_NOW
import com.jrussell.fitness.core.data.inMemoryGetFitDatabase
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.entity.UserProfileEntity
import com.jrussell.fitness.core.testing.FakeClock
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StampedWriterTest {
    private val database: GetFitDatabase = inMemoryGetFitDatabase()
    private val writer: StampedWriter = StampedWriter(database, FakeClock(TEST_NOW), FixedDeviceIdProvider())

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun write_stampsIncreasingRevisions() = runTest {
        val first: Long = writer.write { stamp: WriteStamp -> stamp.revision }
        val second: Long = writer.write { stamp: WriteStamp -> stamp.revision }

        assertThat(listOf(first, second)).containsExactly(1L, 2L)
    }

    @Test
    fun write_passesClockTimeAndDeviceId() = runTest {
        val stamp: WriteStamp = writer.write { stamp: WriteStamp -> stamp }

        assertThat(stamp).isEqualTo(WriteStamp(revision = 1, now = TEST_NOW, deviceId = TEST_DEVICE))
    }

    @Test
    fun write_blockThrows_rollsBackRowsAndRevision() = runTest {
        val result: Result<Unit> = runCatching {
            writer.write { stamp: WriteStamp ->
                val id: Uuid = Uuid.random()
                database.localProfileDao().insert(
                    UserProfileEntity(id, id, null, null, null, null, stamp.toSyncMetadata()),
                )
                throw IllegalStateException("failure after the insert")
            }
        }

        assertThat(result.exceptionOrNull()?.message).isEqualTo("failure after the insert")
        assertThat(database.localProfileDao().findLocalProfile()).isNull()
        assertThat(writer.write { stamp: WriteStamp -> stamp.revision }).isEqualTo(1L)
    }
}

package com.jrussell.fitness.core.data.owner

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.jrussell.fitness.core.data.FixedDeviceIdProvider
import com.jrussell.fitness.core.data.TEST_NOW
import com.jrussell.fitness.core.data.count
import com.jrussell.fitness.core.data.id.UuidIdGenerator
import com.jrussell.fitness.core.data.inMemoryGetFitDatabase
import com.jrussell.fitness.core.data.write.StampedWriter
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.entity.UserProfileEntity
import com.jrussell.fitness.core.model.ids.OwnerId
import com.jrussell.fitness.core.testing.FakeClock
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SingleLocalOwnerProviderTest {
    private val database: GetFitDatabase = inMemoryGetFitDatabase()
    private val clock: FakeClock = FakeClock(TEST_NOW)

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun currentOwnerId_firstCall_createsProfileOwnedByItself() = runTest {
        val ownerId: OwnerId = newProvider().currentOwnerId()

        val profile: UserProfileEntity? = database.localProfileDao().findLocalProfile()
        assertThat(profile).isNotNull()
        assertThat(profile?.id).isEqualTo(ownerId.value)
        assertThat(profile?.ownerId).isEqualTo(ownerId.value)
        assertThat(profile?.sync?.localRevision).isEqualTo(1L)
    }

    @Test
    fun currentOwnerId_secondCall_returnsSameId() = runTest {
        val provider: SingleLocalOwnerProvider = newProvider()

        assertThat(provider.currentOwnerId()).isEqualTo(provider.currentOwnerId())
    }

    @Test
    fun currentOwnerId_newInstance_reusesExistingProfile() = runTest {
        val first: OwnerId = newProvider().currentOwnerId()

        val second: OwnerId = newProvider().currentOwnerId()

        assertThat(second).isEqualTo(first)
        assertThat(database.count("SELECT COUNT(*) FROM UserProfile")).isEqualTo(1)
    }

    @Test
    fun currentOwnerId_concurrentFirstCalls_createOneProfile() = runTest {
        val provider: SingleLocalOwnerProvider = newProvider()

        val calls: List<Deferred<OwnerId>> = List(10) { async { provider.currentOwnerId() } }
        val ids: Set<OwnerId> = calls.awaitAll().toSet()

        assertThat(ids).hasSize(1)
        assertThat(database.count("SELECT COUNT(*) FROM UserProfile")).isEqualTo(1)
    }

    private fun newProvider(): SingleLocalOwnerProvider {
        return SingleLocalOwnerProvider(
            database = database,
            stampedWriter = StampedWriter(database, clock, FixedDeviceIdProvider()),
            idGenerator = UuidIdGenerator(clock),
        )
    }
}

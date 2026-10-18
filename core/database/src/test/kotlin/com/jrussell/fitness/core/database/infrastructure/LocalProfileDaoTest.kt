package com.jrussell.fitness.core.database.infrastructure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.entity.SyncMetadata
import com.jrussell.fitness.core.database.entity.UserProfileEntity
import com.jrussell.fitness.core.database.inMemoryGetFitDatabase
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalProfileDaoTest {
    private val database: GetFitDatabase = inMemoryGetFitDatabase()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun findLocalProfile_empty_isNull() = runTest {
        assertThat(database.localProfileDao().findLocalProfile()).isNull()
    }

    @Test
    fun findLocalProfile_returnsInsertedProfile() = runTest {
        val id: Uuid = Uuid.parse("0199f0a1-2b3c-7d4e-8f50-6a7b8c9d0e1f")
        val now: Instant = Instant.parse("2026-10-17T12:00:00Z")
        val profile: UserProfileEntity = UserProfileEntity(
            id = id,
            ownerId = id,
            displayName = null,
            sex = null,
            birthDate = null,
            heightM = null,
            sync = SyncMetadata(
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                localRevision = 1,
                originDeviceId = Uuid.parse("5b1d7e9a-0c44-4f7a-9c2e-3f6b2c1e8a4d"),
            ),
        )

        database.localProfileDao().insert(profile)

        assertThat(database.localProfileDao().findLocalProfile()).isEqualTo(profile)
    }

    @Test
    fun findLocalProfile_sameCreatedAt_picksLowestId() = runTest {
        val now: Instant = Instant.parse("2026-10-17T12:00:00Z")
        val lower: Uuid = Uuid.parse("0199f0a1-0000-7000-8000-000000000001")
        val higher: Uuid = Uuid.parse("0199f0a1-0000-7000-8000-000000000002")
        database.localProfileDao().insert(profile(higher, now))
        database.localProfileDao().insert(profile(lower, now))

        assertThat(database.localProfileDao().findLocalProfile()?.id).isEqualTo(lower)
    }

    @Test
    fun update_replacesProfile() = runTest {
        val now: Instant = Instant.parse("2026-10-17T12:00:00Z")
        val id: Uuid = Uuid.parse("0199f0a1-0000-7000-8000-000000000001")
        database.localProfileDao().insert(profile(id, now))

        database.localProfileDao().update(profile(id, now).copy(heightM = 1.88))

        assertThat(database.localProfileDao().findLocalProfile()?.heightM).isEqualTo(1.88)
    }

    private fun profile(id: Uuid, createdAt: Instant): UserProfileEntity {
        return UserProfileEntity(
            id = id,
            ownerId = id,
            displayName = null,
            sex = null,
            birthDate = null,
            heightM = null,
            sync = SyncMetadata(
                createdAt = createdAt,
                updatedAt = createdAt,
                deletedAt = null,
                localRevision = 1,
                originDeviceId = Uuid.parse("5b1d7e9a-0c44-4f7a-9c2e-3f6b2c1e8a4d"),
            ),
        )
    }
}

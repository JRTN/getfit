package com.jrussell.fitness.core.data.owner

import com.jrussell.fitness.core.data.write.StampedWriter
import com.jrussell.fitness.core.data.write.WriteStamp
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.entity.UserProfileEntity
import com.jrussell.fitness.core.database.infrastructure.LocalProfileDao
import com.jrussell.fitness.core.domain.IdGenerator
import com.jrussell.fitness.core.model.ids.OwnerId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * v1's owner: the single local profile. The first call creates it (personal fields empty) in a
 * stamped write; later calls, and new instances, reuse the existing row, including one that
 * arrived with a restored `user.db`. Concurrent first calls create exactly one profile.
 */
@Singleton
class SingleLocalOwnerProvider @Inject constructor(
    database: GetFitDatabase,
    private val stampedWriter: StampedWriter,
    private val idGenerator: IdGenerator,
) : CurrentOwnerProvider {
    private val profileDao: LocalProfileDao = database.localProfileDao()
    private val mutex: Mutex = Mutex()
    private var cached: OwnerId? = null

    override suspend fun currentOwnerId(): OwnerId {
        return mutex.withLock {
            cached ?: resolve().also { ownerId: OwnerId -> cached = ownerId }
        }
    }

    private suspend fun resolve(): OwnerId {
        val existing: UserProfileEntity? = profileDao.findLocalProfile()
        if (existing != null) {
            return OwnerId(existing.id)
        }
        return stampedWriter.write { stamp: WriteStamp ->
            // Checked again inside the transaction in case another instance created it meanwhile.
            val raced: UserProfileEntity? = profileDao.findLocalProfile()
            if (raced != null) {
                OwnerId(raced.id)
            } else {
                val id: Uuid = idGenerator.newTimeOrderedId()
                profileDao.insert(
                    UserProfileEntity(
                        id = id,
                        ownerId = id,
                        displayName = null,
                        sex = null,
                        birthDate = null,
                        heightM = null,
                        sync = stamp.toSyncMetadata(),
                    ),
                )
                OwnerId(id)
            }
        }
    }
}

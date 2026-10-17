package com.jrussell.fitness.core.data.repository

import com.jrussell.fitness.core.data.owner.CurrentOwnerProvider
import com.jrussell.fitness.core.data.write.StampedWriter
import com.jrussell.fitness.core.data.write.WriteStamp
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.dao.DayBoundarySettingDao
import com.jrussell.fitness.core.database.entity.DayBoundarySettingEntity
import com.jrussell.fitness.core.domain.DEFAULT_DAY_START_MINUTES
import com.jrussell.fitness.core.domain.DayBoundaryRepository
import com.jrussell.fitness.core.domain.DomainError
import com.jrussell.fitness.core.domain.IdGenerator
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.model.ids.OwnerId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/** [DayBoundaryRepository] over the `DayBoundarySetting` table, scoped to the current owner. */
class DefaultDayBoundaryRepository @Inject constructor(
    database: GetFitDatabase,
    private val ownerProvider: CurrentOwnerProvider,
    private val stampedWriter: StampedWriter,
    private val idGenerator: IdGenerator,
) : DayBoundaryRepository {
    private val dao: DayBoundarySettingDao = database.dayBoundarySettingDao()

    override fun observeDayStartMinutes(on: LocalDate): Flow<Int> {
        return flow {
            val ownerId: OwnerId = ownerProvider.currentOwnerId()
            emitAll(dao.observeInForce(ownerId.value, on))
        }.map { setting: DayBoundarySettingEntity? -> setting?.dayStartMinutes ?: DEFAULT_DAY_START_MINUTES }
    }

    override suspend fun setDayStart(dayStartMinutes: Int, effectiveFrom: LocalDate): Outcome<Unit> {
        if (dayStartMinutes !in VALID_MINUTES) {
            return Outcome.Failure(DomainError.Validation("dayStartMinutes", "must be between 0 and 1439"))
        }
        val ownerId: OwnerId = ownerProvider.currentOwnerId()
        stampedWriter.write { stamp: WriteStamp ->
            val existing: DayBoundarySettingEntity? = dao.findByEffectiveFrom(ownerId.value, effectiveFrom)
            if (existing != null) {
                dao.update(
                    existing.copy(
                        dayStartMinutes = dayStartMinutes,
                        sync = stamp.toSyncMetadata(createdAt = existing.sync.createdAt),
                    ),
                )
            } else {
                dao.insert(
                    DayBoundarySettingEntity(
                        id = idGenerator.newTimeOrderedId(),
                        ownerId = ownerId.value,
                        dayStartMinutes = dayStartMinutes,
                        effectiveFrom = effectiveFrom,
                        sync = stamp.toSyncMetadata(),
                    ),
                )
            }
        }
        return Outcome.Success(Unit)
    }

    private companion object {
        val VALID_MINUTES: IntRange = 0..1439
    }
}

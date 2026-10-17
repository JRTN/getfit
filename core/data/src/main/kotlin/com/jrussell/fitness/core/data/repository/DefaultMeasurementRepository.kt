package com.jrussell.fitness.core.data.repository

import com.jrussell.fitness.core.data.owner.CurrentOwnerProvider
import com.jrussell.fitness.core.data.seed.ReferenceSeeder
import com.jrussell.fitness.core.data.write.StampedWriter
import com.jrussell.fitness.core.data.write.WriteStamp
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.dao.DayBoundarySettingDao
import com.jrussell.fitness.core.database.dao.MeasurementDao
import com.jrussell.fitness.core.database.dao.MeasurementTypeDao
import com.jrussell.fitness.core.database.entity.MeasurementEntity
import com.jrussell.fitness.core.database.entity.MeasurementTypeEntity
import com.jrussell.fitness.core.domain.DEFAULT_DAY_START_MINUTES
import com.jrussell.fitness.core.domain.DomainError
import com.jrussell.fitness.core.domain.IdGenerator
import com.jrussell.fitness.core.domain.ManualMeasurement
import com.jrussell.fitness.core.domain.MeasurementRepository
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.domain.localDateFor
import com.jrussell.fitness.core.model.Measurement
import com.jrussell.fitness.core.model.MeasurementSource
import com.jrussell.fitness.core.model.ids.MeasurementId
import com.jrussell.fitness.core.model.ids.OwnerId
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

/** [MeasurementRepository] over the `Measurement` table, scoped to the current owner. */
class DefaultMeasurementRepository @Inject constructor(
    database: GetFitDatabase,
    private val ownerProvider: CurrentOwnerProvider,
    private val stampedWriter: StampedWriter,
    private val idGenerator: IdGenerator,
    private val referenceSeeder: ReferenceSeeder,
) : MeasurementRepository {
    private val measurementDao: MeasurementDao = database.measurementDao()
    private val typeDao: MeasurementTypeDao = database.measurementTypeDao()
    private val dayBoundaryDao: DayBoundarySettingDao = database.dayBoundarySettingDao()

    /**
     * Seeds reference rows first (the startup bootstrap may still be running), then follows the
     * type reactively: if the type does not exist yet, this emits an empty list and switches to
     * its readings as soon as it is created.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMeasurements(typeKey: String, from: LocalDate, to: LocalDate): Flow<List<Measurement>> {
        return flow {
            referenceSeeder.seed()
            val ownerId: OwnerId = ownerProvider.currentOwnerId()
            emitAll(
                typeDao.observeByKey(ownerId.value, typeKey).flatMapLatest { type: MeasurementTypeEntity? ->
                    if (type == null) {
                        flowOf(emptyList())
                    } else {
                        measurementDao.observeByTypeAndDates(ownerId.value, type.id, from, to)
                            .map { rows: List<MeasurementEntity> ->
                                rows.map { row: MeasurementEntity -> row.toDomain() }
                            }
                    }
                },
            )
        }
    }

    override suspend fun addManual(entry: ManualMeasurement): Outcome<MeasurementId> {
        referenceSeeder.seed()
        val ownerId: OwnerId = ownerProvider.currentOwnerId()
        val type: MeasurementTypeEntity? = typeDao.findByKey(ownerId.value, entry.typeKey)
        return when {
            !entry.value.isFinite() -> Outcome.Failure(DomainError.Validation("value", "must be a finite number"))
            type == null -> Outcome.Failure(DomainError.NotFound("MeasurementType", entry.typeKey))
            else -> Outcome.Success(insertManual(ownerId, type, entry))
        }
    }

    override suspend fun delete(id: MeasurementId): Outcome<Unit> {
        val ownerId: OwnerId = ownerProvider.currentOwnerId()
        val changed: Int = stampedWriter.write { stamp: WriteStamp ->
            measurementDao.tombstone(ownerId.value, id.value, stamp.now, stamp.revision, stamp.deviceId.value)
        }
        return if (changed > 0) {
            Outcome.Success(Unit)
        } else {
            Outcome.Failure(DomainError.NotFound("Measurement", id.value.toString()))
        }
    }

    private suspend fun insertManual(
        ownerId: OwnerId,
        type: MeasurementTypeEntity,
        entry: ManualMeasurement,
    ): MeasurementId {
        val id: Uuid = idGenerator.newTimeOrderedId()
        stampedWriter.write { stamp: WriteStamp ->
            measurementDao.insert(newManualRow(id, ownerId, type, entry, localDateOf(ownerId, entry), stamp))
        }
        return MeasurementId(id)
    }

    /** The day [entry] counts toward, using the day start in force on its calendar date. */
    private suspend fun localDateOf(ownerId: OwnerId, entry: ManualMeasurement): LocalDate {
        val calendarDate: LocalDate = entry.takenAt.toLocalDateTime(entry.timeZone).date
        val dayStartMinutes: Int =
            dayBoundaryDao.findInForce(ownerId.value, calendarDate)?.dayStartMinutes ?: DEFAULT_DAY_START_MINUTES
        return localDateFor(entry.takenAt, entry.timeZone, dayStartMinutes)
    }

    private fun newManualRow(
        id: Uuid,
        ownerId: OwnerId,
        type: MeasurementTypeEntity,
        entry: ManualMeasurement,
        localDate: LocalDate,
        stamp: WriteStamp,
    ): MeasurementEntity {
        return MeasurementEntity(
            id = id,
            ownerId = ownerId.value,
            measurementTypeId = type.id,
            value = entry.value,
            secondaryValue = null,
            startTime = entry.takenAt,
            endTime = entry.takenAt,
            timeZone = entry.timeZone,
            localDate = localDate,
            source = MeasurementSource.MANUAL,
            sourcePackage = null,
            externalId = null,
            enteredValue = entry.enteredValue,
            enteredUnit = entry.enteredUnit,
            note = entry.note,
            sync = stamp.toSyncMetadata(),
        )
    }
}

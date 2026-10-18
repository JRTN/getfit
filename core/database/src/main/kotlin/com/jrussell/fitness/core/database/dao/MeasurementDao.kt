package com.jrussell.fitness.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jrussell.fitness.core.database.entity.MeasurementEntity
import com.jrussell.fitness.core.model.MeasurementSource
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Measurements, always scoped to one owner; reads never return tombstoned rows. */
@Dao
interface MeasurementDao {
    @Insert
    suspend fun insert(entity: MeasurementEntity)

    /** Live readings of one type with `localDate` in `fromDay..toDay` (inclusive), oldest first. */
    @Query(
        """
        SELECT * FROM Measurement
        WHERE ownerId = :ownerId AND measurementTypeId = :measurementTypeId
            AND localDate BETWEEN :fromDay AND :toDay AND deletedAt IS NULL
        ORDER BY startTime
        """,
    )
    fun observeByTypeAndDates(
        ownerId: Uuid,
        measurementTypeId: Uuid,
        fromDay: LocalDate,
        toDay: LocalDate,
    ): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM Measurement WHERE ownerId = :ownerId AND id = :id AND deletedAt IS NULL")
    suspend fun findById(ownerId: Uuid, id: Uuid): MeasurementEntity?

    /**
     * Marks a live row deleted and stamps the write. Returns the number of rows changed: 0 when
     * the row is missing, already deleted, or belongs to another owner.
     */
    @Query(
        """
        UPDATE Measurement
        SET deletedAt = :deletedAt, updatedAt = :deletedAt, localRevision = :revision, originDeviceId = :deviceId
        WHERE ownerId = :ownerId AND id = :id AND deletedAt IS NULL
        """,
    )
    suspend fun tombstone(ownerId: Uuid, id: Uuid, deletedAt: Instant, revision: Long, deviceId: Uuid): Int

    /** Every row of the owner, tombstones included, for the JSON export. */
    @Query("SELECT * FROM Measurement WHERE ownerId = :ownerId ORDER BY id")
    suspend fun exportAll(ownerId: Uuid): List<MeasurementEntity>

    /** The row with [id], tombstoned or not; the JSON merge compares against it. */
    @Query("SELECT * FROM Measurement WHERE ownerId = :ownerId AND id = :id")
    suspend fun findAnyById(ownerId: Uuid, id: Uuid): MeasurementEntity?

    /** The row with this natural key, tombstoned or not (imported rows only; manual rows have none). */
    @Query(
        """
        SELECT * FROM Measurement
        WHERE ownerId = :ownerId AND source = :source AND externalId = :externalId
        """,
    )
    suspend fun findAnyByExternalId(ownerId: Uuid, source: MeasurementSource, externalId: String): MeasurementEntity?

    @Update
    suspend fun update(entity: MeasurementEntity)

    /** How many live readings of any type the owner has (a restore preview's entry count). */
    @Query("SELECT COUNT(*) FROM Measurement WHERE ownerId = :ownerId AND deletedAt IS NULL")
    suspend fun countLive(ownerId: Uuid): Int
}

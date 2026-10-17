package com.jrussell.fitness.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jrussell.fitness.core.database.entity.MeasurementEntity
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
}

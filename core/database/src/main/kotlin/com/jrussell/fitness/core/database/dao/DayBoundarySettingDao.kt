package com.jrussell.fitness.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jrussell.fitness.core.database.entity.DayBoundarySettingEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Versioned day-start settings, always scoped to one owner. */
@Dao
interface DayBoundarySettingDao {
    /** The setting in force on [day] (latest `effectiveFrom` on or before it); null if none. */
    @Query(
        """
        SELECT * FROM DayBoundarySetting
        WHERE ownerId = :ownerId AND effectiveFrom <= :day AND deletedAt IS NULL
        ORDER BY effectiveFrom DESC LIMIT 1
        """,
    )
    fun observeInForce(ownerId: Uuid, day: LocalDate): Flow<DayBoundarySettingEntity?>

    /** One-shot form of [observeInForce], for use inside write transactions. */
    @Query(
        """
        SELECT * FROM DayBoundarySetting
        WHERE ownerId = :ownerId AND effectiveFrom <= :day AND deletedAt IS NULL
        ORDER BY effectiveFrom DESC LIMIT 1
        """,
    )
    suspend fun findInForce(ownerId: Uuid, day: LocalDate): DayBoundarySettingEntity?

    /** The live row starting exactly on [effectiveFrom], if any. */
    @Query(
        """
        SELECT * FROM DayBoundarySetting
        WHERE ownerId = :ownerId AND effectiveFrom = :effectiveFrom AND deletedAt IS NULL
        """,
    )
    suspend fun findByEffectiveFrom(ownerId: Uuid, effectiveFrom: LocalDate): DayBoundarySettingEntity?

    @Insert
    suspend fun insert(entity: DayBoundarySettingEntity)

    @Update
    suspend fun update(entity: DayBoundarySettingEntity)
}

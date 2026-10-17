package com.jrussell.fitness.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jrussell.fitness.core.database.entity.MeasurementTypeEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

/** Measurement types visible to one owner: the built-ins plus that owner's own. */
@Dao
interface MeasurementTypeDao {
    /** The live type with [key], preferring the owner's own row over a built-in. */
    @Query(
        """
        SELECT * FROM MeasurementType
        WHERE `key` = :key AND (ownerId = :ownerId OR ownerId IS NULL) AND deletedAt IS NULL
        ORDER BY ownerId IS NULL LIMIT 1
        """,
    )
    suspend fun findByKey(ownerId: Uuid, key: String): MeasurementTypeEntity?

    /** Reactive form of [findByKey]: emits again when the type is created, changed or deleted. */
    @Query(
        """
        SELECT * FROM MeasurementType
        WHERE `key` = :key AND (ownerId = :ownerId OR ownerId IS NULL) AND deletedAt IS NULL
        ORDER BY ownerId IS NULL LIMIT 1
        """,
    )
    fun observeByKey(ownerId: Uuid, key: String): Flow<MeasurementTypeEntity?>

    /** Inserts rows, skipping any whose ID already exists (so re-seeding is harmless). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<MeasurementTypeEntity>)
}

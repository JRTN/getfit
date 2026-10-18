package com.jrussell.fitness.core.database.infrastructure

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.jrussell.fitness.core.database.entity.MeasurementTypeEntity
import kotlin.uuid.Uuid

/**
 * The built-in rows of mixed-catalog tables (`ownerId IS NULL`). They belong to no owner, which
 * is why this DAO lives with the other owner-less infrastructure: the seeder writes through it,
 * and the import check reads it to know which measurement types every install has.
 */
@Dao
interface ReferenceCatalogDao {
    @Query("SELECT id FROM MeasurementType WHERE ownerId IS NULL")
    suspend fun builtInMeasurementTypeIds(): List<Uuid>

    /**
     * Inserts or replaces built-in rows by ID, so a corrected built-in ships with a raised dataset
     * version. Built-ins are never user-edited, so nothing the user owns is overwritten.
     */
    @Upsert
    suspend fun upsertBuiltIns(entities: List<MeasurementTypeEntity>)
}

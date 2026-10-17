package com.jrussell.fitness.core.database.infrastructure

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.jrussell.fitness.core.database.entity.DatasetVersionEntity

/** Records which bundled seeds have been applied. Owner-less: seeds are shared reference data. */
@Dao
interface DatasetVersionDao {
    /** The applied version of [datasetKey], or null if it was never seeded. */
    @Query("SELECT version FROM DatasetVersion WHERE datasetKey = :datasetKey")
    suspend fun version(datasetKey: String): Int?

    @Upsert
    suspend fun upsert(entity: DatasetVersionEntity)
}

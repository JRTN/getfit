package com.jrussell.fitness.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jrussell.fitness.core.database.entity.DatasetVersionEntity
import com.jrussell.fitness.core.database.entity.SyncCursorEntity
import com.jrussell.fitness.core.database.entity.SyncSequenceEntity
import com.jrussell.fitness.core.database.entity.UserProfileEntity
import com.jrussell.fitness.core.database.infrastructure.DatasetVersionDao
import com.jrussell.fitness.core.database.infrastructure.LocalProfileDao
import com.jrussell.fitness.core.database.infrastructure.SyncSequenceDao

/**
 * `user.db`: everything the user owns. Its schema JSON is exported to `schemas/` and committed;
 * migrations are explicit only (never destructive; detekt forbids the fallback). Version 1 may
 * still change until `v0.1.0`, because no real data exists before then.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        SyncSequenceEntity::class,
        SyncCursorEntity::class,
        DatasetVersionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GetFitDatabase : RoomDatabase() {
    abstract fun syncSequenceDao(): SyncSequenceDao

    abstract fun datasetVersionDao(): DatasetVersionDao

    abstract fun localProfileDao(): LocalProfileDao
}

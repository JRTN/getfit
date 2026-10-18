package com.jrussell.fitness.core.database.infrastructure

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jrussell.fitness.core.database.entity.UserProfileEntity

/**
 * Finds the local profile: the one query that cannot take an `ownerId`, because it is how the
 * owner is discovered. Only the current-owner provider uses it.
 */
@Dao
interface LocalProfileDao {
    /**
     * The oldest live profile, or null on first launch. Ties are broken by ID, so the answer never
     * depends on row order.
     */
    @Query("SELECT * FROM UserProfile WHERE deletedAt IS NULL ORDER BY createdAt, id LIMIT 1")
    suspend fun findLocalProfile(): UserProfileEntity?

    @Insert
    suspend fun insert(entity: UserProfileEntity)

    @Update
    suspend fun update(entity: UserProfileEntity)
}

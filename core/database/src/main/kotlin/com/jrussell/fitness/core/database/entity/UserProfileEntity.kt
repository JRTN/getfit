package com.jrussell.fitness.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jrussell.fitness.core.model.Sex
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

/**
 * The local profile: one row in v1, created silently at first launch. Its [id] is the owner ID
 * of every other user-owned row, and [ownerId] equals [id]. The personal fields stay null until
 * the user enters them.
 */
@Entity(tableName = "UserProfile")
data class UserProfileEntity(
    @PrimaryKey val id: Uuid,
    val ownerId: Uuid,
    val displayName: String?,
    val sex: Sex?,
    val birthDate: LocalDate?,
    val heightM: Double?,
    @Embedded val sync: SyncMetadata,
)

package com.jrussell.fitness.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

/**
 * A versioned day-start setting: from [effectiveFrom] on (until a later row), days begin
 * [dayStartMinutes] after midnight. Evaluating a past day uses the row with the greatest
 * `effectiveFrom` on or before it, so changing the setting never re-dates history.
 * One row per owner per effective date.
 */
@Entity(
    tableName = "DayBoundarySetting",
    indices = [Index(value = ["ownerId", "effectiveFrom"], unique = true)],
)
data class DayBoundarySettingEntity(
    @PrimaryKey val id: Uuid,
    val ownerId: Uuid,
    val dayStartMinutes: Int,
    val effectiveFrom: LocalDate,
    @Embedded val sync: SyncMetadata,
)

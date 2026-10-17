package com.jrussell.fitness.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

/**
 * The global revision counter: a single row (`id` = 1) whose [value] every write transaction
 * increments and stamps onto the rows it writes as `localRevision`. Never synced.
 */
@Entity(tableName = "SyncSequence")
data class SyncSequenceEntity(@PrimaryKey val id: Int, val value: Long)

/**
 * How far one future sync remote has got: rows with `localRevision > lastPushedRevision` still
 * need pushing. Empty in v1 (no remote exists); never synced itself.
 */
@Entity(tableName = "SyncCursor")
data class SyncCursorEntity(
    @PrimaryKey val remoteId: String,
    val lastPushedRevision: Long,
    val lastPulledCursor: String?,
)

/** Which version of a bundled seed (built-in measurement types, exercises, nutrients) is applied. */
@Entity(tableName = "DatasetVersion")
data class DatasetVersionEntity(@PrimaryKey val datasetKey: String, val version: Int, val seededAt: Instant)

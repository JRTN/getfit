package com.jrussell.fitness.core.domain.backup

import com.jrussell.fitness.core.domain.Outcome
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

/** A document the user picked through the system file picker, by its content URI. */
data class DocumentRef(val uri: String)

/** Why a backup, export or restore failed; each maps to one message in Settings. */
enum class BackupFailure {
    /** The file isn't a GetFit backup, or can't be read at all. */
    UNREADABLE,

    /** A backup from a newer version of GetFit. */
    NEWER_VERSION,

    /** A database file, but not one GetFit wrote. */
    NOT_GETFIT,

    /** A GetFit database that fails its integrity check. */
    DAMAGED,

    /** A readable backup with entries this version can't use; nothing was restored. */
    INVALID_ROWS,

    /** The daily-backup folder is missing, unreachable or no longer permitted. */
    FOLDER_UNAVAILABLE,

    /** The file couldn't be written. */
    WRITE_FAILED,
}

/** How a restore treats the data already on the phone. */
enum class RestoreKind {
    /** A JSON export: combined with what's here, the newer version of each entry winning. */
    MERGE,

    /** A database file: replaces everything here; the app restarts. */
    REPLACE,
}

/** What a chosen file holds, shown before the user confirms a restore. */
data class RestorePreview(val kind: RestoreKind, val entryCount: Int, val newestChange: Instant?)

/** What a confirmed restore did. */
sealed interface RestoreResult {
    /** A merge finished: rows [added], [updated] (the file's copy was newer) and [unchanged]. */
    data class Merged(val added: Int, val updated: Int, val unchanged: Int) : RestoreResult

    /** A replacement is staged; the app must restart to finish it. */
    data object RestartRequired : RestoreResult
}

/**
 * The daily backup's state: the chosen folder's name (`null` until one is chosen), when the last
 * backup succeeded, and why the last attempt failed (`null` once a later one succeeds).
 */
data class BackupStatus(val folderName: String?, val lastSuccess: Instant?, val lastFailure: BackupFailure?)

/**
 * Backup and restore (spec: "Backup, export and the sync seam"): a daily database snapshot into a
 * folder chosen once, manual JSON and database exports, and restore from either kind of file.
 * Failures are [com.jrussell.fitness.core.domain.DomainError.Backup].
 */
interface BackupRepository {
    val status: Flow<BackupStatus>

    /** Remembers [folder] (keeping access to it across restarts) and schedules the daily backup. */
    suspend fun chooseFolder(folder: DocumentRef): Outcome<Unit>

    /** Writes a snapshot into the chosen folder now; returns when it was taken. */
    suspend fun backUpNow(): Outcome<Instant>

    /** Writes the JSON export to [to]; returns how many entries (measurements) it holds. */
    suspend fun exportJson(to: DocumentRef): Outcome<Int>

    /** Writes a database copy to [to]. */
    suspend fun exportDatabase(to: DocumentRef): Outcome<Unit>

    /** Reads [from] and describes what restoring it would do, without changing anything. */
    suspend fun inspect(from: DocumentRef): Outcome<RestorePreview>

    /** Restores [from]: merges a JSON export, or stages a database file and asks for a restart. */
    suspend fun restore(from: DocumentRef): Outcome<RestoreResult>
}

/** Restarts the app's process, to finish a restore that replaced the database. */
interface AppRestarter {
    fun restart()
}

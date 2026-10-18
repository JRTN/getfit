package com.jrussell.fitness.core.data.backup

import com.jrussell.fitness.core.backup.documents.DocumentStore
import com.jrussell.fitness.core.backup.snapshot.SnapshotCheck
import com.jrussell.fitness.core.backup.snapshot.inspectSnapshot
import com.jrussell.fitness.core.backup.snapshot.isSqliteFile
import com.jrussell.fitness.core.backup.snapshot.snapshotFileName
import com.jrussell.fitness.core.backup.snapshot.snapshotsToDelete
import com.jrussell.fitness.core.backup.snapshot.writeSnapshot
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.USER_DATABASE_IDENTITY_HASHES
import com.jrussell.fitness.core.database.USER_DATABASE_VERSION
import com.jrussell.fitness.core.domain.backup.BackupFailure
import com.jrussell.fitness.core.importers.export.BundleDecode
import com.jrussell.fitness.core.importers.export.ExportBundle
import com.jrussell.fitness.core.importers.export.decodeExportBundle
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlin.time.Instant

/** A chosen document after it was copied in and identified. */
internal sealed interface ReadFile {
    /** A SQLite file, already checked. */
    data class Snapshot(val file: File, val check: SnapshotCheck) : ReadFile

    /** A readable JSON export whose rows passed the checks. */
    data class Bundle(val bundle: ExportBundle) : ReadFile

    /** Not restorable, for [failure]. */
    data class Failed(val failure: BackupFailure) : ReadFile
}

/**
 * The file side of [DefaultBackupRepository]: snapshots of `user.db`, copying to and from the
 * documents the user picked, and the daily backup folder. Runs on the caller's thread; the
 * repository moves all of it onto the IO dispatcher.
 */
internal class BackupFileOperations(
    private val database: GetFitDatabase,
    private val documents: DocumentStore,
    private val files: BackupFiles,
) {
    /**
     * Writes a snapshot taken at [takenAt] into [folderUri] and prunes snapshots beyond the newest
     * 14; `null` on success, otherwise why it failed.
     */
    fun snapshotIntoFolder(folderUri: String?, takenAt: Instant): BackupFailure? {
        val existing: Map<String, String>? = folderUri?.let(documents::listNames)
        val name: String = snapshotFileName(takenAt)
        val target: String? = existing?.let { names: Map<String, String> ->
            names[name] ?: documents.createFile(checkNotNull(folderUri), name, SQLITE_MIME_TYPE)
        }
        return when {
            existing == null -> BackupFailure.FOLDER_UNAVAILABLE
            target == null || !copySnapshotTo(target) -> BackupFailure.WRITE_FAILED
            else -> {
                snapshotsToDelete((existing.keys + name).toList()).forEach { old: String ->
                    existing[old]?.let(documents::delete)
                }
                null
            }
        }
    }

    /** Writes a fresh snapshot of `user.db` to the document [uri]; `false` if it couldn't be written. */
    fun copySnapshotTo(uri: String): Boolean {
        val snapshot: File = File(files.cacheDirectory, SNAPSHOT_CACHE_NAME)
        writeSnapshot(database.openHelper.writableDatabase, snapshot)
        val written: Boolean = write(uri) { output: OutputStream -> snapshot.inputStream().use { it.copyTo(output) } }
        snapshot.delete()
        return written
    }

    /** Keeps the current data at the safety copy, just before a restore replaces it. */
    fun writeSafetyCopy() {
        writeSnapshot(database.openHelper.writableDatabase, files.pendingRestore.safetyCopy)
    }

    /** Writes to [uri] with [body]; `false` if the document can't be opened or written. */
    fun write(uri: String, body: (OutputStream) -> Unit): Boolean {
        return try {
            documents.openOutput(uri)?.use { output: OutputStream ->
                body(output)
                true
            } ?: false
        } catch (
            @Suppress("SwallowedException") failed: IOException,
        ) {
            false
        }
    }

    /**
     * Copies the chosen document to the cache and identifies it by its first bytes: a SQLite file
     * is inspected; anything else is decoded as a JSON export and passed to [rowsAreRestorable].
     */
    suspend fun readChosen(uri: String, rowsAreRestorable: suspend (ExportBundle) -> Boolean): ReadFile {
        val input: File = File(files.cacheDirectory, RESTORE_INPUT_NAME)
        return if (copyIn(uri, input)) identify(input, rowsAreRestorable) else ReadFile.Failed(BackupFailure.UNREADABLE)
    }

    private suspend fun identify(input: File, rowsAreRestorable: suspend (ExportBundle) -> Boolean): ReadFile {
        val head: ByteArray = input.inputStream().use { stream -> stream.readNBytes(SQLITE_HEADER_LENGTH) }
        if (isSqliteFile(head)) {
            return ReadFile.Snapshot(
                input,
                inspectSnapshot(input, USER_DATABASE_VERSION, USER_DATABASE_IDENTITY_HASHES),
            )
        }
        return when (val decoded: BundleDecode = decodeExportBundle(input.readText(Charsets.UTF_8))) {
            is BundleDecode.Decoded -> {
                val restorable: Boolean = rowsAreRestorable(decoded.bundle)
                if (restorable) ReadFile.Bundle(decoded.bundle) else ReadFile.Failed(BackupFailure.INVALID_ROWS)
            }
            is BundleDecode.NewerVersion -> ReadFile.Failed(BackupFailure.NEWER_VERSION)
            is BundleDecode.Unreadable -> ReadFile.Failed(BackupFailure.UNREADABLE)
        }
    }

    private fun copyIn(uri: String, input: File): Boolean {
        return try {
            files.cacheDirectory.mkdirs()
            documents.openInput(uri)?.use { stream -> input.outputStream().use { stream.copyTo(it) } } != null
        } catch (
            @Suppress("SwallowedException") failed: IOException,
        ) {
            false
        }
    }

    private companion object {
        const val SQLITE_MIME_TYPE: String = "application/vnd.sqlite3"
        const val SNAPSHOT_CACHE_NAME: String = "snapshot.db"
        const val RESTORE_INPUT_NAME: String = "restore-input"
        const val SQLITE_HEADER_LENGTH: Int = 16
    }
}

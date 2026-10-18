package com.jrussell.fitness.core.data.backup

import com.jrussell.fitness.core.backup.documents.DocumentStore
import com.jrussell.fitness.core.backup.work.BackupScheduler
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Instant

/**
 * A [DocumentStore] over plain files for tests: a "tree URI" is a directory path and a document
 * URI a file path. [permitted] stands for the persisted grant; a missing directory is unreachable.
 */
internal class FileDocumentStore(var permitted: Boolean = true) : DocumentStore {
    override fun displayName(uri: String): String? {
        return File(uri).takeIf { file: File -> file.exists() }?.name
    }

    override fun takePersistablePermission(treeUri: String): Boolean {
        return permitted && File(treeUri).isDirectory
    }

    override fun hasPermission(treeUri: String): Boolean {
        return permitted
    }

    override fun openInput(uri: String): InputStream? {
        return File(uri).takeIf { file: File -> file.isFile }?.inputStream()
    }

    override fun openOutput(uri: String): OutputStream? {
        val file: File = File(uri)
        return if (file.parentFile?.isDirectory == true) file.outputStream() else null
    }

    override fun createFile(treeUri: String, name: String, mimeType: String): String? {
        val folder: File = File(treeUri)
        return if (permitted && folder.isDirectory) File(folder, name).apply { createNewFile() }.path else null
    }

    override fun listNames(treeUri: String): Map<String, String>? {
        val folder: File = File(treeUri)
        return if (permitted && folder.isDirectory) {
            folder.listFiles().orEmpty().associate { file: File -> file.name to file.path }
        } else {
            null
        }
    }

    override fun delete(uri: String): Boolean {
        return File(uri).delete()
    }
}

/** A [BackupScheduler] that records what it was asked to do. */
internal class RecordingScheduler : BackupScheduler {
    var ensured: Int = 0
    var rescheduled: Int = 0
    val catchUps: MutableList<Pair<Instant?, Instant>> = mutableListOf()

    override fun ensureScheduled() {
        ensured++
    }

    override fun reschedule() {
        rescheduled++
    }

    override fun catchUpIfStale(lastSuccess: Instant?, now: Instant) {
        catchUps.add(lastSuccess to now)
    }
}

package com.jrussell.fitness.core.backup.snapshot

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * A full restore, waiting for the next app start.
 *
 * `user.db` can't be swapped while Room has it open, so a validated snapshot is [stage]d in
 * [restoreDirectory] and the app restarts; on the next start, before Room opens,
 * [installIfPending] moves it over [databaseFile]. Staging writes to a temporary file and renames
 * it only once the copy is complete and flushed, so a process killed mid-copy leaves nothing
 * that could be installed.
 */
class PendingRestore(private val restoreDirectory: File, private val databaseFile: File) {
    private val staged: File = File(restoreDirectory, STAGED_NAME)

    /** Where the current data is copied (with `VACUUM INTO`) just before a restore replaces it. */
    val safetyCopy: File = File(restoreDirectory, SAFETY_COPY_NAME)

    /** Copies [source] into place for the next start. */
    fun stage(source: File) {
        restoreDirectory.mkdirs()
        val temporary: File = File(restoreDirectory, STAGED_NAME + TEMPORARY_SUFFIX)
        source.inputStream().use { input ->
            FileOutputStream(temporary).use { output: FileOutputStream ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
        Files.move(
            temporary.toPath(),
            staged.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    }

    /**
     * Installs a staged restore, if there is one: the old WAL and shared-memory files are deleted
     * (they belong to the database being replaced), then the staged file becomes [databaseFile].
     * Leftover temporary files from an interrupted [stage] are always deleted.
     *
     * @return whether a restore was installed.
     */
    fun installIfPending(): Boolean {
        restoreDirectory.listFiles().orEmpty()
            .filter { file: File -> file.name.endsWith(TEMPORARY_SUFFIX) }
            .forEach { file: File -> file.delete() }
        if (!staged.isFile) {
            return false
        }
        File(databaseFile.path + "-wal").delete()
        File(databaseFile.path + "-shm").delete()
        databaseFile.parentFile?.mkdirs()
        Files.move(staged.toPath(), databaseFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return true
    }

    private companion object {
        const val STAGED_NAME: String = "staged.db"
        const val SAFETY_COPY_NAME: String = "before-restore.db"
        const val TEMPORARY_SUFFIX: String = ".tmp"
    }
}

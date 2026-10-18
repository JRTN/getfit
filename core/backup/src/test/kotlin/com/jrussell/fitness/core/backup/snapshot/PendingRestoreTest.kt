package com.jrussell.fitness.core.backup.snapshot

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PendingRestoreTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val restoreDirectory: File by lazy { File(temporaryFolder.root, "files/restore") }
    private val databaseFile: File by lazy {
        File(temporaryFolder.root, "databases/user.db").apply {
            parentFile?.mkdirs()
            writeText("current")
        }
    }
    private val pending: PendingRestore by lazy { PendingRestore(restoreDirectory, databaseFile) }

    @Test
    fun install_replacesDatabaseAndDropsWal() {
        val wal: File = File(databaseFile.path + "-wal").apply { writeText("wal") }
        val shm: File = File(databaseFile.path + "-shm").apply { writeText("shm") }
        pending.stage(temporaryFolder.newFile("chosen.db").apply { writeText("restored") })

        assertThat(pending.installIfPending()).isTrue()

        assertThat(databaseFile.readText()).isEqualTo("restored")
        assertThat(wal.exists()).isFalse()
        assertThat(shm.exists()).isFalse()
        assertThat(File(restoreDirectory, "staged.db").exists()).isFalse()
    }

    @Test
    fun install_onlyCompleteStagedFile_isInstalled() {
        restoreDirectory.mkdirs()
        val leftover: File = File(restoreDirectory, "staged.db.tmp").apply { writeText("half a fi") }

        assertThat(pending.installIfPending()).isFalse()

        assertThat(databaseFile.readText()).isEqualTo("current")
        assertThat(leftover.exists()).isFalse()
    }

    @Test
    fun install_nothingStaged_returnsFalse() {
        assertThat(pending.installIfPending()).isFalse()
        assertThat(databaseFile.readText()).isEqualTo("current")
    }

    @Test
    fun stage_leavesNoTemporaryFile() {
        pending.stage(temporaryFolder.newFile("chosen.db").apply { writeText("restored") })

        assertThat(File(restoreDirectory, "staged.db").readText()).isEqualTo("restored")
        assertThat(File(restoreDirectory, "staged.db.tmp").exists()).isFalse()
        assertThat(pending.safetyCopy).isEqualTo(File(restoreDirectory, "before-restore.db"))
    }
}

package com.jrussell.fitness.core.data.logging

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.jrussell.fitness.core.testing.FakeClock
import java.io.File
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileLoggerTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val clock: FakeClock = FakeClock(Instant.parse("2026-10-18T12:34:56Z"))

    @Test
    fun fileLogger_writesLevelTagMessageAndStackTrace() {
        val directory: File = File(temporaryFolder.root, "logs")
        val logger: FileLogger = FileLogger(directory, clock, TimeZone.UTC)

        logger.info("Backup", "snapshot written")
        logger.error("Backup", "snapshot failed", IllegalStateException("disk full"))

        val text: String = File(directory, "getfit-20261018.log").readText()
        assertThat(text).contains("2026-10-18T12:34:56Z INFO Backup: snapshot written")
        assertThat(text).contains("ERROR Backup: snapshot failed")
        assertThat(text).contains("java.lang.IllegalStateException: disk full")
    }

    @Test
    fun fileLogger_dropsFilesOlderThan7Days() {
        val directory: File = File(temporaryFolder.root, "logs").apply { mkdirs() }
        val old: File = File(directory, "getfit-20261010.log").apply { writeText("old") }
        val recent: File = File(directory, "getfit-20261012.log").apply { writeText("recent") }
        val unrelated: File = File(directory, "notes.txt").apply { writeText("keep") }

        FileLogger(directory, clock, TimeZone.UTC).warn("Startup", "hello")

        assertThat(old.exists()).isFalse()
        assertThat(recent.exists()).isTrue()
        assertThat(unrelated.exists()).isTrue()
    }

    @Test
    fun fileLogger_stopsAppendingPastTheSizeCap() {
        val directory: File = File(temporaryFolder.root, "logs")
        val logger: FileLogger = FileLogger(directory, clock, TimeZone.UTC)
        val file: File = File(directory, "getfit-20261018.log")
        directory.mkdirs()
        file.writeText("x".repeat(FileLogger.MAX_FILE_BYTES.toInt()))

        logger.info("Backup", "one more")

        assertThat(file.readText().contains("one more")).isFalse()
    }

    @Test
    fun fileLogger_unwritableDirectory_doesNotThrow() {
        val notADirectory: File = temporaryFolder.newFile("logs")

        FileLogger(notADirectory, clock, TimeZone.UTC).error("Backup", "nowhere to write", RuntimeException())
        clock.advanceBy(1.days)
    }
}

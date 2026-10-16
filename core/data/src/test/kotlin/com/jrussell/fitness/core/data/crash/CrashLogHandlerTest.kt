package com.jrussell.fitness.core.data.crash

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import java.io.File
import kotlin.time.Clock
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashLogHandlerTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val clock: SteppingClock = SteppingClock(startEpochMillis = 1_000L)
    private val previous: RecordingHandler = RecordingHandler()
    private val thread: Thread = Thread("crash-test-thread")

    @Test
    fun uncaughtException_writesStackTraceFile() {
        val directory: File = File(temporaryFolder.root, "crash-logs")
        val handler: CrashLogHandler = CrashLogHandler(directory, clock, previous)

        handler.uncaughtException(thread, IllegalStateException("boom"))

        val files: List<File> = directory.listFiles().orEmpty().toList()
        assertThat(files.map { file: File -> file.name }).containsExactly("crash-0000000001000.txt")
        val text: String = files.single().readText()
        assertThat(text).contains("crash-test-thread")
        assertThat(text).contains("java.lang.IllegalStateException: boom")
        assertThat(text).contains("\tat ")
    }

    @Test
    fun uncaughtException_delegatesToPrevious() {
        val handler: CrashLogHandler = CrashLogHandler(temporaryFolder.root, clock, previous)
        val throwable: Throwable = IllegalStateException("boom")

        handler.uncaughtException(thread, throwable)

        assertThat(previous.calls).hasSize(1)
        assertThat(previous.calls.single().first).isSameInstanceAs(thread)
        assertThat(previous.calls.single().second).isSameInstanceAs(throwable)
    }

    @Test
    fun uncaughtException_keepsOnlyNewestTenFiles() {
        val handler: CrashLogHandler = CrashLogHandler(temporaryFolder.root, clock, previous)

        repeat(12) {
            handler.uncaughtException(thread, IllegalStateException("boom"))
        }

        val names: List<String> = temporaryFolder.root.list().orEmpty().sorted()
        assertThat(names).hasSize(CrashLogHandler.MAX_FILES)
        assertThat(names.first()).isEqualTo("crash-0000000001002.txt")
        assertThat(names.last()).isEqualTo("crash-0000000001011.txt")
    }

    @Test
    fun uncaughtException_whenDirectoryUnwritable_stillDelegates() {
        val notADirectory: File = temporaryFolder.newFile("occupied")
        val handler: CrashLogHandler = CrashLogHandler(notADirectory, clock, previous)

        handler.uncaughtException(thread, IllegalStateException("boom"))

        assertThat(previous.calls).hasSize(1)
    }

    @Test
    fun uncaughtException_withNullPrevious_doesNotThrow() {
        val handler: CrashLogHandler = CrashLogHandler(temporaryFolder.root, clock, null)

        handler.uncaughtException(thread, IllegalStateException("boom"))

        assertThat(temporaryFolder.root.list().orEmpty().toList()).hasSize(1)
    }

    /** Returns [startEpochMillis], then one millisecond later on every call. */
    private class SteppingClock(startEpochMillis: Long) : Clock {
        private var nextEpochMillis: Long = startEpochMillis

        override fun now(): Instant {
            val instant: Instant = Instant.fromEpochMilliseconds(nextEpochMillis)
            nextEpochMillis += 1
            return instant
        }
    }

    private class RecordingHandler : Thread.UncaughtExceptionHandler {
        val calls: MutableList<Pair<Thread, Throwable>> = mutableListOf()

        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            calls.add(thread to throwable)
        }
    }
}

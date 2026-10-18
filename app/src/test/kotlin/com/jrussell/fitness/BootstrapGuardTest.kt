package com.jrussell.fitness

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isInstanceOf
import com.jrussell.fitness.core.testing.RecordingLogger
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BootstrapGuardTest {
    private val logger: RecordingLogger = RecordingLogger()

    @Test
    fun runGuarded_failure_logsError_doesNotThrow() = runTest {
        runGuarded(logger, task = "Bootstrap") { error("seed table missing") }

        assertThat(logger.entries).containsExactly("ERROR Startup: Bootstrap failed: seed table missing")
    }

    @Test
    fun runGuarded_cancellation_propagates() = runTest {
        assertFailure {
            runGuarded(logger, task = "Bootstrap") { throw CancellationException("stopped") }
        }.isInstanceOf<CancellationException>()
    }
}

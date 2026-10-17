package com.jrussell.fitness.core.sync

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NoOpSyncGatewayTest {
    private val gateway: SyncGateway = NoOpSyncGateway()

    @Test
    fun push_isSkippedWithReason() = runTest {
        assertThat(gateway.push(emptyList())).isEqualTo(PushResult.Skipped(reason = "Sync disabled"))
    }

    @Test
    fun pull_isEmpty() = runTest {
        assertThat(gateway.pull(sinceRemoteCursor = null)).isEqualTo(PullResult.Empty)
    }
}

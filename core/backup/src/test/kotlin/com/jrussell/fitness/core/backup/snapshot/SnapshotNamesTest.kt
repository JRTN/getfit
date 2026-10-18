package com.jrussell.fitness.core.backup.snapshot

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import kotlin.time.Instant
import org.junit.Test

class SnapshotNamesTest {
    @Test
    fun snapshotFileName_formatsUtc() {
        // 05:47 UTC, whatever the device's zone.
        assertThat(snapshotFileName(Instant.parse("2026-10-18T05:47:59Z")))
            .isEqualTo("getfit-snapshot-20261018-0547Z.db")
    }

    @Test
    fun snapshotsToDelete_keepsNewest14_ignoresOtherFiles() {
        val snapshots: List<String> = (1..16).map { day: Int ->
            "getfit-snapshot-202610${day.toString().padStart(2, '0')}-0300Z.db"
        }
        val names: List<String> = snapshots.shuffled() + listOf("notes.txt", "getfit-backup-2026-10-01.json")

        assertThat(snapshotsToDelete(names)).containsExactlyInAnyOrder(
            "getfit-snapshot-20261001-0300Z.db",
            "getfit-snapshot-20261002-0300Z.db",
        )
    }
}

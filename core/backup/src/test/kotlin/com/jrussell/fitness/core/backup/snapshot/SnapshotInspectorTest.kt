package com.jrussell.fitness.core.backup.snapshot

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import java.io.File
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SnapshotInspectorTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val v1Only: Map<Int, String> = mapOf(1 to TEST_HASH_V1)

    private fun file(name: String = "snapshot.db"): File {
        return File(temporaryFolder.root, name)
    }

    @Test
    fun inspect_valid_reportsLiveCountAndNewestChange() {
        aGetFitFile(file())

        // m3 is tombstoned: not counted, but its update is still the newest change.
        assertThat(inspectSnapshot(file(), currentVersion = 1, identityHashes = v1Only))
            .isEqualTo(SnapshotCheck.Valid(measurementCount = 2, newestChange = Instant.fromEpochMilliseconds(5000)))
    }

    @Test
    fun inspect_newerVersion_isNewer() {
        aGetFitFile(file(), userVersion = 2)

        assertThat(inspectSnapshot(file(), 1, v1Only)).isEqualTo(SnapshotCheck.NewerVersion(2))
    }

    @Test
    fun inspect_olderVersionWithKnownHash_isValid() {
        aGetFitFile(file(), userVersion = 1, identityHash = TEST_HASH_V1)

        val check: SnapshotCheck = inspectSnapshot(file(), 2, mapOf(1 to TEST_HASH_V1, 2 to TEST_HASH_V2))

        assertThat(check is SnapshotCheck.Valid).isTrue()
    }

    @Test
    fun inspect_olderVersionWithUnknownHash_isNotGetFit() {
        aGetFitFile(file(), userVersion = 1, identityHash = "0000")

        assertThat(inspectSnapshot(file(), 2, mapOf(1 to TEST_HASH_V1, 2 to TEST_HASH_V2)))
            .isEqualTo(SnapshotCheck.NotGetFit)
    }

    @Test
    fun inspect_otherAppsDatabase_isNotGetFit() {
        aGetFitFile(file(), identityHash = null)

        assertThat(inspectSnapshot(file(), 1, v1Only)).isEqualTo(SnapshotCheck.NotGetFit)
    }

    @Test
    fun inspect_wrongHash_isNotGetFit() {
        aGetFitFile(file(), identityHash = TEST_HASH_V2)

        assertThat(inspectSnapshot(file(), 1, v1Only)).isEqualTo(SnapshotCheck.NotGetFit)
    }

    @Test
    fun inspect_textFile_isNotSqlite() {
        file().writeText("""{"formatVersion": 1}""")

        assertThat(inspectSnapshot(file(), 1, v1Only)).isEqualTo(SnapshotCheck.NotSqlite)
        assertThat(isSqliteFile(file().readBytes())).isFalse()
    }

    @Test
    fun inspect_truncatedFile_isDamaged() {
        val whole: File = file("whole.db")
        aGetFitFile(whole)
        file().writeBytes(whole.readBytes().copyOf(TRUNCATED_BYTES))

        assertThat(isSqliteFile(file().readBytes())).isTrue()
        assertThat(inspectSnapshot(file(), 1, v1Only)).isEqualTo(SnapshotCheck.Damaged)
    }

    @Test
    fun inspect_dayStart1500_isInvalidRows() {
        aGetFitFile(file(), dayStartMinutes = 1500)

        assertThat(inspectSnapshot(file(), 1, v1Only)).isEqualTo(SnapshotCheck.InvalidRows)
    }

    @Test
    fun inspect_badZone_isInvalidRows() {
        aGetFitFile(file(), timeZone = "Mars/Olympus")

        assertThat(inspectSnapshot(file(), 1, v1Only)).isEqualTo(SnapshotCheck.InvalidRows)
    }

    @Test
    fun inspect_unknownUnitCode_isInvalidRows() {
        aGetFitFile(file(), canonicalUnit = "furlong")

        assertThat(inspectSnapshot(file(), 1, v1Only)).isEqualTo(SnapshotCheck.InvalidRows)
    }

    @Test
    fun inspect_otherAppsDatabaseWithHighVersion_isNotGetFit() {
        // Most apps set user_version; without Room's table it is not a "newer GetFit".
        aGetFitFile(file(), userVersion = 7, identityHash = null)

        assertThat(inspectSnapshot(file(), 1, v1Only)).isEqualTo(SnapshotCheck.NotGetFit)
    }

    @Test
    fun inspect_unknownSourceName_isInvalidRows() {
        aGetFitFile(file(), source = "FAX")

        assertThat(inspectSnapshot(file(), 1, v1Only)).isEqualTo(SnapshotCheck.InvalidRows)
    }

    @Test
    fun inspect_unknownEnteredUnit_isInvalidRows() {
        aGetFitFile(file(), enteredUnit = "stone")

        assertThat(inspectSnapshot(file(), 1, v1Only)).isEqualTo(SnapshotCheck.InvalidRows)
    }

    private companion object {
        const val TRUNCATED_BYTES: Int = 4096
    }
}

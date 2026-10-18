package com.jrussell.fitness.core.importers.export

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThan
import kotlin.time.Instant
import org.junit.jupiter.api.Test

class ExportBundleCodecTest {
    @Test
    fun roundTrip_keepsEveryFieldIncludingTombstones() {
        val bundle: ExportBundle = aBundle(
            measurements = listOf(aMeasurement(deletedAt = Instant.parse("2026-10-03T09:00:00Z"))),
        )

        assertThat(decodeExportBundle(encodeExportBundle(bundle))).isEqualTo(BundleDecode.Decoded(bundle))
    }

    @Test
    fun encoded_hasTopLevelFieldsInSpecOrder() {
        val text: String = encodeExportBundle(aBundle())
        val positions: List<Int> = listOf("formatVersion", "exportedAt", "appVersion", "ownerId", "profiles")
            .map { field: String -> text.indexOf("\"$field\"") }

        positions.zipWithNext().forEach { (earlier: Int, later: Int) ->
            assertThat(earlier).isLessThan(later)
        }
    }

    @Test
    fun decode_higherFormatVersion_isNewerVersion() {
        assertThat(decodeExportBundle("""{"formatVersion": 2, "somethingNew": [1, 2]}"""))
            .isEqualTo(BundleDecode.NewerVersion(2))
    }

    @Test
    fun decode_truncated_isUnreadable() {
        val text: String = encodeExportBundle(aBundle())

        assertThat(decodeExportBundle(text.take(text.length / 2))).isInstanceOf<BundleDecode.Unreadable>()
    }

    @Test
    fun decode_notJson_isUnreadable() {
        assertThat(decodeExportBundle("PNG\u0000\u0001 not json")).isInstanceOf<BundleDecode.Unreadable>()
        assertThat(decodeExportBundle("""{"ownerId": "x"}""")).isInstanceOf<BundleDecode.Unreadable>()
    }

    @Test
    fun decode_unknownExtraField_isIgnored() {
        val bundle: ExportBundle = aBundle()
        val text: String = encodeExportBundle(bundle).replaceFirst("{", """{"addedLater": {"x": 1},""")

        assertThat(decodeExportBundle(text)).isEqualTo(BundleDecode.Decoded(bundle))
    }
}

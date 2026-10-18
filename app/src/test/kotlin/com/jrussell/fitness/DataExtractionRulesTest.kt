package com.jrussell.fitness

import android.content.Context
import android.content.res.XmlResourceParser
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.xmlpull.v1.XmlPullParser

/**
 * Auto Backup (spec: "Backup layers") copies `user.db` and the DataStore preferences to the
 * user's Google account, and nothing else: no crash logs, no logs, no restore staging, no
 * reference catalog. The device ID is never backed up because it lives in `noBackupFilesDir`.
 */
@RunWith(RobolectricTestRunner::class)
class DataExtractionRulesTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun cloudAndTransfer_includeExactlyUserDbWalAndDatastore() {
        val expected: Array<String> = arrayOf("database:user.db", "database:user.db-wal", "file:datastore/")

        assertThat(rules().getValue("cloud-backup").map { it.toString() }).containsExactlyInAnyOrder(*expected)
        assertThat(rules().getValue("device-transfer").map { it.toString() }).containsExactlyInAnyOrder(*expected)
    }

    @Test
    fun nothingNamesReferenceDbCrashLogsOrRestore() {
        val paths: List<String> = rules().values.flatten().map { rule: Rule -> rule.path }

        val forbidden: List<String> = listOf("reference", "crash-logs", "logs", "restore", "corrupt")
        val named: List<String> = paths.filter { path: String ->
            forbidden.any { prefix: String -> path.startsWith(prefix) }
        }
        assertThat(named).isEmpty()
        assertThat(rules().values.flatten().filter { rule: Rule -> rule.kind == "exclude" }.size).isEqualTo(0)
    }

    @Test
    fun manifest_pointsAtRules() {
        // ApplicationInfo.dataExtractionRulesRes is hidden, so the manifest source is checked directly.
        val manifest: String = File("src/main/AndroidManifest.xml").readText()

        assertThat(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\"")).isTrue()
    }

    /** One `<include>` or `<exclude>` element. */
    private data class Rule(val kind: String, val domain: String, val path: String) {
        override fun toString(): String {
            return "$domain:$path"
        }
    }

    /** The rules file's elements, grouped by their section (`cloud-backup`, `device-transfer`). */
    private fun rules(): Map<String, List<Rule>> {
        val parser: XmlResourceParser = context.resources.getXml(R.xml.data_extraction_rules)
        val sections: MutableMap<String, MutableList<Rule>> = mutableMapOf()
        var section: String? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "cloud-backup", "device-transfer" -> section = parser.name.also { sections[it] = mutableListOf() }
                    "include", "exclude" -> sections.getValue(checkNotNull(section)).add(
                        Rule(
                            kind = parser.name,
                            domain = parser.getAttributeValue(null, "domain"),
                            path = parser.getAttributeValue(null, "path"),
                        ),
                    )
                }
            }
        }
        return sections
    }
}

package com.jrussell.fitness.core.backup.snapshot

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** How many daily snapshots the backup folder keeps. */
const val SNAPSHOTS_KEPT: Int = 14

private val SNAPSHOT_NAME: Regex = Regex("""getfit-snapshot-\d{8}-\d{4}Z\.db""")

/**
 * The file name of a snapshot taken at [at]: `getfit-snapshot-20261018-0547Z.db`. UTC, so names
 * sort in time order even across a clock change (local time repeats an hour when clocks fall
 * back); no colons, which some document providers reject.
 */
fun snapshotFileName(at: Instant): String {
    val utc: LocalDateTime = at.toLocalDateTime(TimeZone.UTC)
    val date: String = "${utc.year}${two(utc.month.ordinal + 1)}${two(utc.day)}"
    return "getfit-snapshot-$date-${two(utc.hour)}${two(utc.minute)}Z.db"
}

/**
 * The snapshot files among [names] beyond the newest [keep]. Files that aren't snapshots (a
 * manual export, anything else in the folder) are never listed.
 */
fun snapshotsToDelete(names: List<String>, keep: Int = SNAPSHOTS_KEPT): List<String> {
    return names.filter { name: String -> SNAPSHOT_NAME.matches(name) }
        .sortedDescending()
        .drop(keep)
}

private fun two(value: Int): String {
    return value.toString().padStart(2, '0')
}

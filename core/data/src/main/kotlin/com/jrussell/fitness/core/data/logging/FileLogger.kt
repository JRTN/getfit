package com.jrussell.fitness.core.data.logging

import com.jrussell.fitness.core.model.logging.Logger
import java.io.File
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A [Logger] that appends to one file per day in [directory] (`getfit-yyyyMMdd.log`, dated in
 * [zone]). Files older than [KEPT_DAYS] days are deleted when a new day's file starts, and a file
 * stops growing at [MAX_FILE_BYTES], so logs never fill the phone. A write that fails is dropped:
 * logging must never break what it describes.
 *
 * `:app` binds it with `files/logs/`, next to the crash logs (a viewer in Settings comes later).
 */
class FileLogger(private val directory: File, private val clock: Clock, private val zone: TimeZone) : Logger {
    private var prunedFor: LocalDate? = null

    override fun debug(tag: String, message: String) {
        append("DEBUG", tag, message, null)
    }

    override fun info(tag: String, message: String) {
        append("INFO", tag, message, null)
    }

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        append("WARN", tag, message, throwable)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        append("ERROR", tag, message, throwable)
    }

    @Synchronized
    private fun append(level: String, tag: String, message: String, throwable: Throwable?) {
        val now: Instant = clock.now()
        val day: LocalDate = now.toLocalDateTime(zone).date
        try {
            directory.mkdirs()
            if (prunedFor != day) {
                pruneBefore(now)
                prunedFor = day
            }
            val file: File = File(directory, fileName(day))
            if (file.length() < MAX_FILE_BYTES) {
                val trace: String = throwable?.let { "\n" + it.stackTraceToString() } ?: ""
                file.appendText("$now $level $tag: $message$trace\n")
            }
        } catch (
            @Suppress("SwallowedException") ignored: IOException,
        ) {
            // Dropped on purpose; see the class comment.
        }
    }

    private fun pruneBefore(now: Instant) {
        val oldestKept: String = fileName((now - KEPT_DAYS.days).toLocalDateTime(zone).date)
        directory.listFiles().orEmpty()
            .filter { file: File -> LOG_NAME.matches(file.name) && file.name < oldestKept }
            .forEach { file: File -> file.delete() }
    }

    private fun fileName(day: LocalDate): String {
        val month: String = (day.month.ordinal + 1).toString().padStart(2, '0')
        return "getfit-${day.year}$month${day.day.toString().padStart(2, '0')}.log"
    }

    companion object {
        /** How many days of logs are kept. */
        const val KEPT_DAYS: Int = 7

        /** The size at which a day's file stops growing. */
        const val MAX_FILE_BYTES: Long = 512L * 1024L

        private val LOG_NAME: Regex = Regex("""getfit-\d{8}\.log""")
    }
}

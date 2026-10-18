package com.jrussell.fitness.core.testing

import com.jrussell.fitness.core.model.logging.Logger

/** A [Logger] that keeps each line as `"LEVEL tag: message"`, plus `: <throwable message>` if any. */
class RecordingLogger : Logger {
    val entries: MutableList<String> = mutableListOf()

    override fun debug(tag: String, message: String) {
        entries.add("DEBUG $tag: $message")
    }

    override fun info(tag: String, message: String) {
        entries.add("INFO $tag: $message")
    }

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        entries.add(line("WARN", tag, message, throwable))
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        entries.add(line("ERROR", tag, message, throwable))
    }

    private fun line(level: String, tag: String, message: String, throwable: Throwable?): String {
        return "$level $tag: $message" + (throwable?.message?.let { cause: String -> ": $cause" } ?: "")
    }
}

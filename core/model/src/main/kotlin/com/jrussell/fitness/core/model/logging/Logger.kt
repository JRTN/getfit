package com.jrussell.fitness.core.model.logging

/**
 * Where code logs (code convention 10): an interface here, so pure modules can log too, with a
 * file-backed implementation in `:core:data` bound by `:app`. Background work (the daily backup,
 * later the Health Connect import) logs through it, so a failure overnight leaves a trace.
 *
 * [tag] names the area ("Backup", "Startup"); implementations never throw.
 */
interface Logger {
    fun debug(tag: String, message: String)

    fun info(tag: String, message: String)

    fun warn(tag: String, message: String, throwable: Throwable? = null)

    fun error(tag: String, message: String, throwable: Throwable? = null)
}

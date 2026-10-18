package com.jrussell.fitness

import com.jrussell.fitness.core.model.logging.Logger
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a startup [task] so that its failure is logged instead of crashing the process. A crash in
 * `Application.onCreate` work would repeat on every launch (a crash loop); logged, the app still
 * opens and the problem shows in the logs. Cancellation is passed on, never swallowed.
 */
internal suspend fun runGuarded(logger: Logger, task: String, block: suspend () -> Unit) {
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (
        @Suppress("TooGenericExceptionCaught") failed: Exception,
    ) {
        logger.error(STARTUP_TAG, "$task failed", failed)
    }
}

private const val STARTUP_TAG: String = "Startup"

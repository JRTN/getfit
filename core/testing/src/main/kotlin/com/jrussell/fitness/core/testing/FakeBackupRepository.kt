package com.jrussell.fitness.core.testing

import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.domain.backup.AppRestarter
import com.jrussell.fitness.core.domain.backup.BackupRepository
import com.jrussell.fitness.core.domain.backup.BackupStatus
import com.jrussell.fitness.core.domain.backup.DocumentRef
import com.jrussell.fitness.core.domain.backup.RestorePreview
import com.jrussell.fitness.core.domain.backup.RestoreResult
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory [BackupRepository] for ViewModel tests: each call returns the outcome set in its
 * property and records its argument. Set [gate] to hold calls open until a test completes it
 * (to test what happens while one is still running).
 */
class FakeBackupRepository(initialStatus: BackupStatus = BackupStatus(null, null, null)) : BackupRepository {
    override val status: MutableStateFlow<BackupStatus> = MutableStateFlow(initialStatus)

    var chooseFolderOutcome: Outcome<Unit> = Outcome.Success(Unit)
    var backUpNowOutcome: Outcome<Instant> = Outcome.Success(Instant.fromEpochMilliseconds(0))
    var exportJsonOutcome: Outcome<Int> = Outcome.Success(0)
    var exportDatabaseOutcome: Outcome<Unit> = Outcome.Success(Unit)
    var inspectOutcome: Outcome<RestorePreview>? = null
    var restoreOutcome: Outcome<RestoreResult> = Outcome.Success(RestoreResult.Merged(0, 0, 0))
    var gate: CompletableDeferred<Unit>? = null

    private val calls: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())

    /** Every call so far, e.g. `"backUpNow"` or `"exportJson:content://x"`. */
    val recorded: StateFlow<List<String>> get() = calls

    override suspend fun chooseFolder(folder: DocumentRef): Outcome<Unit> {
        record("chooseFolder:${folder.uri}")
        return chooseFolderOutcome
    }

    override suspend fun backUpNow(): Outcome<Instant> {
        record("backUpNow")
        return backUpNowOutcome
    }

    override suspend fun exportJson(to: DocumentRef): Outcome<Int> {
        record("exportJson:${to.uri}")
        return exportJsonOutcome
    }

    override suspend fun exportDatabase(to: DocumentRef): Outcome<Unit> {
        record("exportDatabase:${to.uri}")
        return exportDatabaseOutcome
    }

    override suspend fun inspect(from: DocumentRef): Outcome<RestorePreview> {
        record("inspect:${from.uri}")
        return checkNotNull(inspectOutcome) { "Set inspectOutcome first" }
    }

    override suspend fun restore(from: DocumentRef): Outcome<RestoreResult> {
        record("restore:${from.uri}")
        return restoreOutcome
    }

    private suspend fun record(call: String) {
        calls.value = calls.value + call
        gate?.await()
    }
}

/** An [AppRestarter] that counts restarts instead of restarting. */
class FakeAppRestarter : AppRestarter {
    var restarts: Int = 0
        private set

    override fun restart() {
        restarts++
    }
}

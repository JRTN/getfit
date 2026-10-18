package com.jrussell.fitness.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrussell.fitness.core.domain.DomainError
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.domain.backup.AppRestarter
import com.jrussell.fitness.core.domain.backup.BackupFailure
import com.jrussell.fitness.core.domain.backup.BackupRepository
import com.jrussell.fitness.core.domain.backup.BackupStatus
import com.jrussell.fitness.core.domain.backup.DocumentRef
import com.jrussell.fitness.core.domain.backup.RestorePreview
import com.jrussell.fitness.core.domain.backup.RestoreResult
import com.jrussell.fitness.core.domain.time.WallClock
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

/** The result of the last backup action, shown under the section heading. */
sealed interface BackupMessage {
    data class Exported(val entries: Int) : BackupMessage

    data object BackedUp : BackupMessage

    data class Merged(val added: Int, val updated: Int) : BackupMessage

    data class Failed(val failure: BackupFailure) : BackupMessage
}

/** A chosen file waiting for the user to confirm the restore; [changedOn] is its newest change, locally. */
data class PendingRestoreUi(val ref: DocumentRef, val preview: RestorePreview, val changedOn: LocalDate?)

/**
 * What Settings › Backup shows. Dates are already local ([lastBackup], [today]), so the screen
 * needs no time zone; [exportDate] (`yyyy-MM-dd`) names exported files.
 */
data class BackupUiState(
    val folderName: String?,
    val lastBackup: LocalDate?,
    val lastFailure: BackupFailure?,
    val today: LocalDate,
    val exportDate: String,
    val busy: Boolean,
    val message: BackupMessage?,
    val pendingRestore: PendingRestoreUi?,
)

/**
 * Settings › Backup: the daily backup folder, "Back up now", the two exports and restore. One
 * action runs at a time: while one is running ([BackupUiState.busy]) further taps are ignored, so
 * a double tap never exports or restores twice. A restore that replaced the database restarts the
 * app ([AppRestarter]).
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
    private val restarter: AppRestarter,
    private val clock: WallClock,
) : ViewModel() {
    private val transient: MutableStateFlow<Transient> = MutableStateFlow(Transient())
    private var running: Job? = null

    val uiState: StateFlow<BackupUiState?> = combine(repository.status, transient) {
            status: BackupStatus,
            now: Transient,
        ->
        val today: LocalDate = localDate(clock.now())
        BackupUiState(
            folderName = status.folderName,
            lastBackup = status.lastSuccess?.let(::localDate),
            lastFailure = status.lastFailure,
            today = today,
            exportDate = today.toString(),
            busy = now.busy,
            message = now.message,
            pendingRestore = now.pendingRestore,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    fun onFolderChosen(uri: String) {
        perform {
            when (val outcome: Outcome<Unit> = repository.chooseFolder(DocumentRef(uri))) {
                is Outcome.Success -> null
                is Outcome.Failure -> failed(outcome)
            }
        }
    }

    fun onBackUpNow() {
        perform {
            when (val outcome: Outcome<Instant> = repository.backUpNow()) {
                is Outcome.Success -> BackupMessage.BackedUp
                is Outcome.Failure -> failed(outcome)
            }
        }
    }

    fun onExportJson(uri: String) {
        perform {
            when (val outcome: Outcome<Int> = repository.exportJson(DocumentRef(uri))) {
                is Outcome.Success -> BackupMessage.Exported(outcome.value)
                is Outcome.Failure -> failed(outcome)
            }
        }
    }

    fun onExportDatabase(uri: String) {
        perform {
            when (val outcome: Outcome<Unit> = repository.exportDatabase(DocumentRef(uri))) {
                is Outcome.Success -> BackupMessage.BackedUp
                is Outcome.Failure -> failed(outcome)
            }
        }
    }

    /** Reads the chosen file and asks the user to confirm what restoring it would do. */
    fun onRestoreFileChosen(uri: String) {
        val ref: DocumentRef = DocumentRef(uri)
        perform {
            when (val outcome: Outcome<RestorePreview> = repository.inspect(ref)) {
                is Outcome.Success -> {
                    val preview: RestorePreview = outcome.value
                    transient.update {
                        it.copy(pendingRestore = PendingRestoreUi(ref, preview, preview.newestChange?.let(::localDate)))
                    }
                    null
                }
                is Outcome.Failure -> failed(outcome)
            }
        }
    }

    fun onRestoreConfirmed() {
        val pending: PendingRestoreUi = transient.value.pendingRestore ?: return
        transient.update { it.copy(pendingRestore = null) }
        perform {
            when (val outcome: Outcome<RestoreResult> = repository.restore(pending.ref)) {
                is Outcome.Success -> when (val result: RestoreResult = outcome.value) {
                    is RestoreResult.Merged -> BackupMessage.Merged(result.added, result.updated)
                    RestoreResult.RestartRequired -> {
                        restarter.restart()
                        null
                    }
                }
                is Outcome.Failure -> failed(outcome)
            }
        }
    }

    fun onRestoreDismissed() {
        transient.update { it.copy(pendingRestore = null) }
    }

    /** Runs [action] unless another is running; its result becomes the message (or none). */
    private fun perform(action: suspend () -> BackupMessage?) {
        if (running?.isActive == true) {
            return
        }
        transient.update { it.copy(busy = true, message = null) }
        running = viewModelScope.launch {
            val message: BackupMessage? = action()
            transient.update { it.copy(busy = false, message = message) }
        }
    }

    private fun failed(outcome: Outcome.Failure): BackupMessage {
        val failure: BackupFailure = (outcome.error as? DomainError.Backup)?.failure ?: BackupFailure.WRITE_FAILED
        return BackupMessage.Failed(failure)
    }

    private fun localDate(instant: Instant): LocalDate {
        return instant.toLocalDateTime(clock.timeZone()).date
    }

    /** State that lives only while this screen does. */
    private data class Transient(
        val busy: Boolean = false,
        val message: BackupMessage? = null,
        val pendingRestore: PendingRestoreUi? = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS: Long = 5_000L
    }
}

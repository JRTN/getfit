package com.jrussell.fitness.feature.settings

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.jrussell.fitness.core.domain.DomainError
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.domain.backup.BackupFailure
import com.jrussell.fitness.core.domain.backup.BackupStatus
import com.jrussell.fitness.core.domain.backup.DocumentRef
import com.jrussell.fitness.core.domain.backup.RestoreKind
import com.jrussell.fitness.core.domain.backup.RestorePreview
import com.jrussell.fitness.core.domain.backup.RestoreResult
import com.jrussell.fitness.core.testing.FakeAppRestarter
import com.jrussell.fitness.core.testing.FakeBackupRepository
import com.jrussell.fitness.core.testing.FakeWallClock
import com.jrussell.fitness.core.testing.MainDispatcherRule
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test

class BackupViewModelTest {
    @get:Rule
    val mainDispatcherRule: MainDispatcherRule = MainDispatcherRule()

    private val repository: FakeBackupRepository = FakeBackupRepository()
    private val restarter: FakeAppRestarter = FakeAppRestarter()
    private val clock: FakeWallClock =
        FakeWallClock(Instant.parse("2026-10-18T15:00:00Z"), TimeZone.of("America/Toronto"))

    private fun viewModel(): BackupViewModel {
        return BackupViewModel(repository, restarter, clock)
    }

    @Test
    fun status_isShownWithLocalDates() = runTest {
        repository.status.value = BackupStatus("Backups", Instant.parse("2026-10-17T02:00:00Z"), null)

        val state: BackupUiState = viewModel().uiState.filterNotNull().first()

        assertThat(state.folderName).isEqualTo("Backups")
        // 02:00 UTC on Oct 17 is still Oct 16 in Toronto.
        assertThat(state.lastBackup).isEqualTo(LocalDate(2026, 10, 16))
        assertThat(state.today).isEqualTo(LocalDate(2026, 10, 18))
        assertThat(state.exportDate).isEqualTo("2026-10-18")
    }

    @Test
    fun folderChosen_callsRepository() = runTest {
        viewModel().onFolderChosen("content://tree/backups")

        assertThat(repository.recorded.value).containsExactly("chooseFolder:content://tree/backups")
    }

    @Test
    fun folderRefused_showsFolderError() = runTest {
        repository.chooseFolderOutcome = Outcome.Failure(DomainError.Backup(BackupFailure.FOLDER_UNAVAILABLE))
        val viewModel: BackupViewModel = viewModel()

        viewModel.onFolderChosen("content://tree/backups")

        assertThat(state(viewModel).message).isEqualTo(BackupMessage.Failed(BackupFailure.FOLDER_UNAVAILABLE))
    }

    @Test
    fun backUpNow_success_showsBackedUp() = runTest {
        val viewModel: BackupViewModel = viewModel()

        viewModel.onBackUpNow()

        assertThat(state(viewModel).message).isEqualTo(BackupMessage.BackedUp)
        assertThat(state(viewModel).busy).isEqualTo(false)
    }

    @Test
    fun exportJson_success_showsCount() = runTest {
        repository.exportJsonOutcome = Outcome.Success(42)
        val viewModel: BackupViewModel = viewModel()

        viewModel.onExportJson("content://doc/1")

        assertThat(state(viewModel).message).isEqualTo(BackupMessage.Exported(42))
    }

    @Test
    fun restoreChosen_showsPreview_confirmMerges_showsCounts() = runTest {
        val preview: RestorePreview = RestorePreview(RestoreKind.MERGE, 12, Instant.parse("2026-10-10T12:00:00Z"))
        repository.inspectOutcome = Outcome.Success(preview)
        repository.restoreOutcome = Outcome.Success(RestoreResult.Merged(added = 3, updated = 1, unchanged = 8))
        val viewModel: BackupViewModel = viewModel()

        viewModel.onRestoreFileChosen("content://doc/backup.json")
        val pending: PendingRestoreUi? = state(viewModel).pendingRestore
        viewModel.onRestoreConfirmed()

        assertThat(
            pending,
        ).isEqualTo(PendingRestoreUi(DocumentRef("content://doc/backup.json"), preview, LocalDate(2026, 10, 10)))
        assertThat(state(viewModel).pendingRestore).isNull()
        assertThat(state(viewModel).message).isEqualTo(BackupMessage.Merged(added = 3, updated = 1))
        assertThat(restarter.restarts).isEqualTo(0)
    }

    @Test
    fun restoreReplace_confirmed_restarts() = runTest {
        repository.inspectOutcome = Outcome.Success(RestorePreview(RestoreKind.REPLACE, 12, null))
        repository.restoreOutcome = Outcome.Success(RestoreResult.RestartRequired)
        val viewModel: BackupViewModel = viewModel()

        viewModel.onRestoreFileChosen("content://doc/backup.db")
        viewModel.onRestoreConfirmed()

        assertThat(restarter.restarts).isEqualTo(1)
    }

    @Test
    fun restoreDismissed_restoresNothing() = runTest {
        repository.inspectOutcome = Outcome.Success(RestorePreview(RestoreKind.REPLACE, 12, null))
        val viewModel: BackupViewModel = viewModel()

        viewModel.onRestoreFileChosen("content://doc/backup.db")
        viewModel.onRestoreDismissed()

        assertThat(state(viewModel).pendingRestore).isNull()
        assertThat(repository.recorded.value).containsExactly("inspect:content://doc/backup.db")
    }

    @Test
    fun restoreFailure_showsErrorMessage_noRestart() = runTest {
        repository.inspectOutcome = Outcome.Failure(DomainError.Backup(BackupFailure.NEWER_VERSION))
        val viewModel: BackupViewModel = viewModel()

        viewModel.onRestoreFileChosen("content://doc/future.json")

        assertThat(state(viewModel).message).isEqualTo(BackupMessage.Failed(BackupFailure.NEWER_VERSION))
        assertThat(state(viewModel).pendingRestore).isNull()
        assertThat(restarter.restarts).isEqualTo(0)
    }

    @Test
    fun secondActionWhileBusy_isIgnored() = runTest {
        val gate: CompletableDeferred<Unit> = CompletableDeferred()
        repository.gate = gate
        val viewModel: BackupViewModel = viewModel()

        viewModel.onBackUpNow()
        viewModel.onExportJson("content://doc/1")
        val busy: Boolean = state(viewModel).busy
        gate.complete(Unit)

        assertThat(busy).isEqualTo(true)
        assertThat(repository.recorded.value).containsExactly("backUpNow")
    }

    private suspend fun state(viewModel: BackupViewModel): BackupUiState {
        return viewModel.uiState.filterNotNull().first()
    }
}

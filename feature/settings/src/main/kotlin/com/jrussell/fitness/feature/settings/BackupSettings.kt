package com.jrussell.fitness.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jrussell.fitness.core.designsystem.theme.GetFitTheme
import com.jrussell.fitness.core.domain.backup.BackupFailure
import com.jrussell.fitness.core.domain.backup.RestoreKind
import com.jrussell.fitness.core.ui.format.formatShortDate
import kotlinx.datetime.LocalDate

private const val JSON_MIME_TYPE: String = "application/json"
private const val SQLITE_MIME_TYPE: String = "application/vnd.sqlite3"

/** What restore offers in the file picker: JSON exports and database copies, by any type a provider reports. */
private val RESTORE_MIME_TYPES: Array<String> =
    arrayOf(JSON_MIME_TYPE, "application/octet-stream", SQLITE_MIME_TYPE, "application/x-sqlite3")

/**
 * Settings › Backup, wired to its [BackupViewModel] and the system file pickers: a folder for the
 * daily backup (chosen once), a document to export to, and a document to restore from.
 */
@Composable
fun BackupSettings(modifier: Modifier = Modifier, viewModel: BackupViewModel = hiltViewModel()) {
    val state: BackupUiState? by viewModel.uiState.collectAsStateWithLifecycle()
    val chooseFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.onFolderChosen(it.toString()) }
    }
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON_MIME_TYPE)) { uri ->
        uri?.let { viewModel.onExportJson(it.toString()) }
    }
    val exportDatabase =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SQLITE_MIME_TYPE)) { uri ->
            uri?.let { viewModel.onExportDatabase(it.toString()) }
        }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onRestoreFileChosen(it.toString()) }
    }
    state?.let { loaded: BackupUiState ->
        BackupSection(
            state = loaded,
            actions = BackupSectionActions(
                onChooseFolder = { chooseFolder.launch(null) },
                onBackUpNow = viewModel::onBackUpNow,
                onExportJson = { exportJson.launch("getfit-backup-${loaded.exportDate}.json") },
                onExportDatabase = { exportDatabase.launch("getfit-backup-${loaded.exportDate}.db") },
                onRestore = { restore.launch(RESTORE_MIME_TYPES) },
                onRestoreConfirmed = viewModel::onRestoreConfirmed,
                onRestoreDismissed = viewModel::onRestoreDismissed,
            ),
            modifier = modifier,
        )
    }
}

/** What the Backup section's controls do; [None] for previews and tests. */
class BackupSectionActions(
    val onChooseFolder: () -> Unit = {},
    val onBackUpNow: () -> Unit = {},
    val onExportJson: () -> Unit = {},
    val onExportDatabase: () -> Unit = {},
    val onRestore: () -> Unit = {},
    val onRestoreConfirmed: () -> Unit = {},
    val onRestoreDismissed: () -> Unit = {},
) {
    companion object {
        val None: BackupSectionActions = BackupSectionActions()
    }
}

/**
 * The Backup section's content: heading, the last action's result (announced politely by
 * TalkBack), the daily backup's status and buttons, then export and restore. Buttons sit in flow
 * rows, so at large font sizes they wrap instead of clipping. While an action runs ([BackupUiState.busy])
 * every button is disabled.
 */
@Composable
fun BackupSection(state: BackupUiState, actions: BackupSectionActions, modifier: Modifier = Modifier) {
    val enabled: Boolean = !state.busy
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_backup_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp).semantics { heading() },
        )
        state.message?.let { message: BackupMessage -> ResultMessage(message) }
        Text(text = stringResource(R.string.backup_daily_title), style = MaterialTheme.typography.bodyLarge)
        DailyStatus(state)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val folderLabel: Int = if (state.folderName ==
                null
            ) {
                R.string.backup_choose_folder
            } else {
                R.string.backup_change_folder
            }
            OutlinedButton(onClick = actions.onChooseFolder, enabled = enabled) { Text(stringResource(folderLabel)) }
            OutlinedButton(onClick = actions.onBackUpNow, enabled = enabled && state.folderName != null) {
                Text(stringResource(R.string.backup_now))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ExportAndRestoreButtons(enabled, actions)
        Text(
            text = stringResource(R.string.backup_export_json_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    state.pendingRestore?.let { pending: PendingRestoreUi -> RestoreDialog(pending, state.today, actions) }
}

/** The last action's result, announced politely by TalkBack when it appears. */
@Composable
private fun ResultMessage(message: BackupMessage) {
    Text(
        text = messageText(message),
        style = MaterialTheme.typography.bodyMedium,
        color = if (message is BackupMessage.Failed) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun ExportAndRestoreButtons(enabled: Boolean, actions: BackupSectionActions) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = actions.onExportJson, enabled = enabled) {
            Text(stringResource(R.string.backup_export_json))
        }
        OutlinedButton(onClick = actions.onExportDatabase, enabled = enabled) {
            Text(stringResource(R.string.backup_export_db))
        }
        OutlinedButton(onClick = actions.onRestore, enabled = enabled) { Text(stringResource(R.string.backup_restore)) }
    }
}

@Composable
private fun DailyStatus(state: BackupUiState) {
    val failure: BackupFailure? = state.lastFailure
    val folder: String? = state.folderName
    val text: String = when {
        folder == null -> stringResource(R.string.backup_daily_off)
        failure != null -> stringResource(failureText(failure))
        state.lastBackup != null -> stringResource(
            R.string.backup_daily_on,
            folder,
            formatShortDate(state.lastBackup, state.today),
        )
        else -> stringResource(R.string.backup_daily_never, folder)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (folder != null &&
            failure != null
        ) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

/** Confirms a restore: a merge combines; a replacement warns that everything here goes and the app restarts. */
@Composable
private fun RestoreDialog(pending: PendingRestoreUi, today: LocalDate, actions: BackupSectionActions) {
    val replace: Boolean = pending.preview.kind == RestoreKind.REPLACE
    val changed: String = pending.changedOn?.let { date: LocalDate -> formatShortDate(date, today) } ?: "—"
    val count: Int = pending.preview.entryCount
    AlertDialog(
        onDismissRequest = actions.onRestoreDismissed,
        title = {
            Text(
                stringResource(
                    if (replace) R.string.backup_restore_replace_title else R.string.backup_restore_merge_title,
                ),
            )
        },
        text = {
            val body: Int = if (replace) R.plurals.backup_restore_replace_body else R.plurals.backup_restore_merge_body
            Text(pluralStringResource(body, count, count, changed))
        },
        confirmButton = {
            TextButton(onClick = actions.onRestoreConfirmed) {
                Text(
                    stringResource(
                        if (replace) R.string.backup_restore_replace_confirm else R.string.backup_restore_confirm,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onRestoreDismissed) { Text(stringResource(R.string.backup_cancel)) }
        },
    )
}

@Composable
private fun messageText(message: BackupMessage): String {
    return when (message) {
        is BackupMessage.Exported -> pluralStringResource(
            R.plurals.backup_done_exported,
            message.entries,
            message.entries,
        )
        BackupMessage.BackedUp -> stringResource(R.string.backup_done_snapshot)
        is BackupMessage.Merged -> stringResource(R.string.backup_done_merged, message.added, message.updated)
        is BackupMessage.Failed -> stringResource(failureText(message.failure))
    }
}

@StringRes
private fun failureText(failure: BackupFailure): Int {
    return when (failure) {
        BackupFailure.UNREADABLE, BackupFailure.NOT_GETFIT -> R.string.backup_error_unreadable
        BackupFailure.NEWER_VERSION -> R.string.backup_error_newer
        BackupFailure.DAMAGED -> R.string.backup_error_damaged
        BackupFailure.INVALID_ROWS -> R.string.backup_error_invalid
        BackupFailure.FOLDER_UNAVAILABLE -> R.string.backup_error_folder
        BackupFailure.WRITE_FAILED -> R.string.backup_error_write
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun BackupSectionPreview() {
    GetFitTheme {
        BackupSection(
            state = BackupUiState(
                folderName = "GetFit backups",
                lastBackup = LocalDate(2026, 10, 17),
                lastFailure = null,
                today = LocalDate(2026, 10, 18),
                exportDate = "2026-10-18",
                busy = false,
                message = BackupMessage.Exported(42),
                pendingRestore = null,
            ),
            actions = BackupSectionActions.None,
        )
    }
}
